package com.litovskiy.service.command.ability;

import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.CommandResult;
import com.litovskiy.config.properties.AbilityProperties;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.PlayerBehaviorStats;
import com.litovskiy.log.Action;
import com.litovskiy.log.metadata.TransferLogMetadata;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.ability.AbilityTargetContext;
import com.litovskiy.service.ability.AbilityTargetHelper;
import com.litovskiy.service.ability.PlayerStatus;
import com.litovskiy.service.ability.PlayerStatusService;
import com.litovskiy.service.command.BotCommand;
import com.litovskiy.service.command.Command;
import com.litovskiy.service.command.CommandContext;
import com.litovskiy.service.data.PlayerService;
import com.litovskiy.service.log.GameLogService;
import com.litovskiy.service.log.PlayerBehaviorStatService;
import com.litovskiy.util.CommandBlockReason;
import com.litovskiy.util.TargetUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import static com.litovskiy.util.StringUtil.round;

@Component
@RequiredArgsConstructor
@BotCommand(
    name = "transfer",
    description = "перевести часть роста другому игроку с комиссией (переводить можно только тем, у кого меньше)",
    arguments = {
        @BotCommand.CommandArgument(
            name = "user",
            required = true,
            type = BotCommand.ArgumentType.USER,
            description = "Цель"
        ),
        @BotCommand.CommandArgument(
            name = "value",
            required = true,
            type = BotCommand.ArgumentType.NUMBER,
            description = "Размер перевода"
        )
    }
)
public class TransferCommand implements Command {

    private final PlayerService playerDao;
    private final GameLogService gameLogService;
    private final ConversationStyleService conversationStyleService;
    private final PlayerBehaviorStatService playerBehaviorStatService;
    private final PlayerStatusService playerStatusService;
    private final AbilityProperties abilityProperties;
    private final AbilityTargetHelper abilityTargetHelper;
    private final Clock clock;

    @Override
    @Transactional
    public CommandResult execute(CommandContext context) {

        Platform platform = context.platform();
        Long scopeId = context.chatId();
        long playerId = context.userId();

        if (scopeId == null) {
            return CommandResult.single("Эта способность доступна только в беседе или на сервере.");
        }

        Optional<TargetUser> targetUser = context.targetId();
        if (targetUser.isEmpty()) {
            return CommandResult.single("Ответьте этой командой на сообщение цели.");
        }

        if (targetUser.get().bot()) {
            return CommandResult.single("Спасибо конечно, но мне ваши подачки не нужны.");
        }

        long targetPlayerId = targetUser.get().id();

        double transferValue;
        try {
            transferValue = Double.parseDouble(context.arguments()[0]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return CommandResult.single("После команды нужно указать число для перевода");
        }

        AbilityTargetContext abilityTargetContext = abilityTargetHelper.prepareTargetAbility(
            platform,
            playerId,
            scopeId,
            targetPlayerId
        );

        boolean ignoreCooldown = playerStatusService.ignoresAbilityCooldown(abilityTargetContext.actor(), Action.TRANSFER);

        if (!ignoreCooldown && !abilityTargetContext.success()) {
            return CommandResult.single(abilityTargetContext.rejectionMessage());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Player actor = abilityTargetContext.actor();
        Player target = abilityTargetContext.target();

        CommandBlockReason blockReason = playerStatusService.validateActionAllowed(actor, Action.TRANSFER);

        if (!blockReason.allowed()) {
            return CommandResult.single(blockReason.message());
        }

        GrowthStyle growthStyle = conversationStyleService.getStyle(platform, scopeId);

        if (transferValue < 0 || !Double.isFinite(transferValue)) {
            double diff = actor.getSize() * 0.05;
            actor.setLastAbilityTime(now);
            actor.setSize(Math.max(
                1.0,
                round(actor.getSize() - diff)
            ));
            playerDao.save(actor);
            String result = "Вы обвиняетесь в подрыве государственного строя. " +
                "В качестве меры наказания к вам будет применен уголовный штраф в размере 5% от текущего роста. " +
                "Ваш рост уменьшен на " + GrowthStyle.convertValue(diff, growthStyle) + ".";
            return CommandResult.single(result);
        }

        if (actor.getSize() - 1 < transferValue) {
            return CommandResult.single("Вы не можете перевести больше, чем у вас есть");
        }

        String name = switch (platform) {
            case TELEGRAM -> target.getTelegramDisplayName();
            case DISCORD -> target.getDiscordTag();
        };
        if (name == null || name.isBlank()) {
            name = "получателю";
        }

        double costPercent = abilityProperties.getTransferCommission();

        double affectedCostPercent = playerStatusService.modifyTransferCommission(actor, costPercent);
        double cost = round(transferValue * affectedCostPercent);

        double oldActorSize = actor.getSize();
        double oldTargetSize = target.getSize();

        actor.setSize(Math.max(1.0, round(actor.getSize() - transferValue)));
        if (!ignoreCooldown) {
            actor.setLastAbilityTime(now);
        }

        target.setSize(Math.max(
            1.0,
            round(target.getSize() + transferValue - cost)
        ));

        TransferLogMetadata logMetadata = new TransferLogMetadata(cost);

        gameLogService.logAbility(playerId, oldActorSize, actor.getSize(),
            targetPlayerId, oldTargetSize, target.getSize(),
            Action.TRANSFER, logMetadata);

        PlayerBehaviorStats stat = playerBehaviorStatService.applyAbility(playerId, Action.TRANSFER, targetPlayerId);
        PlayerStatus status = playerStatusService.applyStatus(actor, stat);

        playerDao.save(target);
        playerDao.save(actor);

        String result = "Вы перевели " + GrowthStyle.convertValue(transferValue, growthStyle) + " "
            + name + " с комиссией в размере " + GrowthStyle.convertValue(cost, growthStyle);

        return CommandResult.of(
            CommandMessage.reply(result),
            CommandMessage.broadcast(status.getMessage())
        );
    }
}
