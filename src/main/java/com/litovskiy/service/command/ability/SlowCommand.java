package com.litovskiy.service.command.ability;

import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.CommandResult;
import com.litovskiy.config.properties.AbilityProperties;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.PlayerBehaviorStats;
import com.litovskiy.log.Action;
import com.litovskiy.log.metadata.GrowthModifierLogMetadata;
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

@Component
@RequiredArgsConstructor
@BotCommand(
    name = "slow",
    description = "замедлить рост врага бесплатно",
    arguments = {
        @BotCommand.CommandArgument(
            name = "user",
            required = true,
            type = BotCommand.ArgumentType.USER,
            description = "Цель"
        )
    }
)
public class SlowCommand implements Command {

    private final PlayerService playerDao;
    private final GameLogService gameLogService;
    private final PlayerBehaviorStatService playerBehaviorStatService;
    private final PlayerStatusService playerStatusService;
    private final Clock clock;
    private final AbilityProperties abilityProperties;
    private final AbilityTargetHelper abilityTargetHelper;


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
            return CommandResult.single("Мне кажется ты что-то попутал.");
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

        CommandBlockReason blockReason = playerStatusService.validateActionAllowed(actor, Action.SLOW);

        if (!blockReason.allowed()) {
            return CommandResult.single(blockReason.message());
        }

        double growthPenalty = abilityProperties.getSlowGrowthPenalty();
        double minGrowth = abilityProperties.getMinPendingGrowth();
        double maxGrowth = abilityProperties.getMaxPendingGrowth();

        double affectedGrowthModifier = playerStatusService.modifyGrowthModifier(actor, target, Action.SLOW, growthPenalty);

        double oldTargetGrowthModifier = target.getPendingFailChanceModifier();
        double newTargetGrowthModifier =  Math.clamp(
            target.getPendingGrowthModifier() - affectedGrowthModifier,
            minGrowth,
            maxGrowth
        );
        actor.setLastAbilityTime(LocalDateTime.now(clock));
        target.setPendingGrowthModifier(newTargetGrowthModifier);

        GrowthModifierLogMetadata logMetadata = new GrowthModifierLogMetadata(oldTargetGrowthModifier, newTargetGrowthModifier, affectedGrowthModifier);
        gameLogService.logAbility(playerId, actor.getSize(), null,
            targetPlayerId, target.getSize(), null,
            Action.SLOW, logMetadata);

        PlayerBehaviorStats stat = playerBehaviorStatService.applyAbility(playerId, Action.SLOW, targetPlayerId);
        PlayerStatus status = playerStatusService.applyStatus(actor, stat);

        playerDao.save(target);
        playerDao.save(actor);

        String result = "Вы ослабили следующий рост цели на " + abilityTargetHelper.toPercent(affectedGrowthModifier) + "%.";

        return CommandResult.of(
            CommandMessage.reply(result),
            CommandMessage.broadcast(status.getMessage())
        );
    }
}
