package com.litovskiy.service.command.ability;

import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.CommandResult;
import com.litovskiy.config.properties.AbilityProperties;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.PlayerBehaviorStats;
import com.litovskiy.log.Action;
import com.litovskiy.log.metadata.GrowthModifierLogMetadata;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.ability.AbilitySelfContext;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDateTime;
import static com.litovskiy.util.StringUtil.round;

@Component
@RequiredArgsConstructor
@BotCommand(
    name = "turtle",
    description = "увеличить себе рост бесплатно"
)
public class TurtleCommand implements Command {

    private final PlayerService playerDao;
    private final GameLogService gameLogService;
    private final PlayerBehaviorStatService playerBehaviorStatService;
    private final PlayerStatusService playerStatusService;
    private final AbilityProperties abilityProperties;
    private final AbilityTargetHelper abilityTargetHelper;
    private final Clock clock;

    @Override
    @Transactional
    public CommandResult execute(CommandContext context) {
        Platform platform = context.platform();
        long playerId = context.userId();

        AbilitySelfContext abilitySelfContext = abilityTargetHelper.prepareSelfAbility(platform, playerId);

        if (!abilitySelfContext.success()) {
            return CommandResult.single(abilitySelfContext.rejectionMessage());
        }

        Player actor = abilitySelfContext.player();

        CommandBlockReason blockReason = playerStatusService.validateActionAllowed(actor, Action.TURTLE);

        if (!blockReason.allowed()) {
            return CommandResult.single(blockReason.message());
        }

        double increaseBonus = abilityProperties.getTurtleGrowthBonus();
        double maxGrowthBonus = abilityProperties.getMaxPendingGrowth();
        double oldGrowthBonus = actor.getPendingGrowthModifier();


        PlayerBehaviorStats stat = playerBehaviorStatService.applyAbility(playerId, Action.TURTLE, null);
        PlayerStatus status = playerStatusService.applyStatus(actor, stat);

        double affectedGrowthModifier = playerStatusService.modifyGrowthModifier(actor, null, Action.TURTLE, increaseBonus);

        actor.setLastAbilityTime(LocalDateTime.now(clock));
        actor.setPendingGrowthModifier(Math.min(
            maxGrowthBonus,
            round(oldGrowthBonus + affectedGrowthModifier)
        ));

        GrowthModifierLogMetadata logMetadata = new GrowthModifierLogMetadata(
            oldGrowthBonus,
            actor.getPendingGrowthModifier(),
            affectedGrowthModifier
        );

        gameLogService.logAbility(playerId, actor.getSize(), null,
            null, null, null,
            Action.TURTLE, logMetadata);


        playerDao.save(actor);
        String result = "Вы усилили свой следующий рост на " + abilityTargetHelper.toPercent(affectedGrowthModifier) + "%.";

        return CommandResult.of(
            CommandMessage.reply(result),
            CommandMessage.broadcast(status.getMessage())
        );
    }
}
