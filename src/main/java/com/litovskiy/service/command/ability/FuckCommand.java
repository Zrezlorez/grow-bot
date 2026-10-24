package com.litovskiy.service.command.ability;

import com.litovskiy.bot.CommandResult;
import com.litovskiy.config.properties.AbilityProperties;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.log.Action;
import com.litovskiy.log.metadata.FuckLogMetadata;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.ability.AbilityTargetContext;
import com.litovskiy.service.ability.AbilityTargetHelper;
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
    name = "fuck",
    description = "увеличить шанс неудачи для врага за процент от своего размера",
    arguments = {
        @BotCommand.CommandArgument(
            name = "user",
            required = true,
            type = BotCommand.ArgumentType.USER,
            description = "Цель"
        )
    }
)
public class FuckCommand implements Command {

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
            return CommandResult.single("Советую извиниться и больше не ошибаться.");
        }

        long targetPlayerId = targetUser.get().id();

        AbilityTargetContext abilityTargetContext = abilityTargetHelper.prepareTargetAbility(
            platform,
            playerId,
            scopeId,
            targetPlayerId
        );

        if (!abilityTargetContext.success()) {
            return CommandResult.single(abilityTargetContext.rejectionMessage());
        }


        Player actor = abilityTargetContext.actor();
        Player target = abilityTargetContext.target();

        CommandBlockReason blockReason = playerStatusService.validateActionAllowed(actor, Action.FUCK);

        if (!blockReason.allowed()) {
            return CommandResult.single(blockReason.message());
        }

        GrowthStyle growthStyle = conversationStyleService.getStyle(platform, scopeId);

        double costPercent = abilityProperties.getFuckCostPercent();
        double failBonus = abilityProperties.getFuckFailChancePenalty();
        double maxFailChance = abilityProperties.getMaxPendingFailChance();
        double modifiedFailChance = playerStatusService.modifyFailChance(actor, target, failBonus);

        double cost = round(actor.getSize() * costPercent);
        double oldActorSize = actor.getSize();

        if (modifiedFailChance == -1) {
            return CommandResult.single("Вы не можете атаковать игрока, если ваш рост меньше чем 10% от его размера");
        }

        actor.setSize(Math.max(1.0, round(actor.getSize() - cost)));
        actor.setLastAbilityTime(LocalDateTime.now(clock));


        double oldTargetFailChance = target.getPendingFailChanceModifier();
        target.setPendingFailChanceModifier(Math.min(
            maxFailChance,
            round(oldTargetFailChance + modifiedFailChance)
        ));

        FuckLogMetadata logMetadata = new FuckLogMetadata(oldTargetFailChance, target.getPendingFailChanceModifier(), cost);

        gameLogService.logAbility(playerId, oldActorSize, actor.getSize(),
            targetPlayerId, target.getSize(), null,
            Action.FUCK, logMetadata);

        playerBehaviorStatService.applyAbility(playerId, Action.FUCK, targetPlayerId);

        playerDao.save(target);
        playerDao.save(actor);

        String result = "Вы усилили шанс неудачи цели на " + abilityTargetHelper.toPercent(failBonus)
            + "% за " + GrowthStyle.convertValue(cost, growthStyle) + ". Следующая попытка роста у цели будет опаснее.";

        return CommandResult.single(result);
    }
}
