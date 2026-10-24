package com.litovskiy.service.command.ability;

import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.CommandResult;
import com.litovskiy.config.properties.AbilityProperties;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.PlayerBehaviorStats;
import com.litovskiy.log.Action;
import com.litovskiy.log.metadata.JackpotLogMetadata;
import com.litovskiy.repository.ConversationParticipantRepository;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.PlayerAccountService;
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
    name = "jackpot",
    description = "увеличить шанс джекпота и неудачи за процент от своего размера"
)
public class JackpotCommand implements Command {

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
        long playerId = context.userId();

        AbilitySelfContext abilitySelfContext = abilityTargetHelper.prepareSelfAbility(platform, playerId);

        if (!abilitySelfContext.success()) {
            return CommandResult.single(abilitySelfContext.rejectionMessage());
        }

        Player actor = abilitySelfContext.player();

        CommandBlockReason blockReason = playerStatusService.validateActionAllowed(actor, Action.JACKPOT);

        if (!blockReason.allowed()) {
            return CommandResult.single(blockReason.message());
        }

        double failBonus = abilityProperties.getJackpotFailChance();
        double critBonus = abilityProperties.getJackpotCritChance();

        double maxCritChance = abilityProperties.getMaxPendingCritChance();
        double maxFailChance = abilityProperties.getMaxPendingFailChance();

        double oldCritChance = actor.getPendingCritChanceModifier();
        double newCritChance = round(oldCritChance + critBonus);

        double oldFailChance = actor.getPendingFailChanceModifier();
        double newFailChance = round(oldFailChance + failBonus);

        actor.setLastAbilityTime(LocalDateTime.now(clock));
        actor.setPendingCritChanceModifier(Math.min(
            maxCritChance,
            newCritChance
        ));

        actor.setPendingFailChanceModifier(Math.min(
            maxFailChance,
            newFailChance
        ));

        JackpotLogMetadata logMetadata = new JackpotLogMetadata(
            oldCritChance, newCritChance,
            oldFailChance, newFailChance,
            newCritChance - oldCritChance,
            newFailChance - oldFailChance);

        gameLogService.logAbility(playerId, actor.getSize(), null,
            null, null, null,
            Action.JACKPOT, logMetadata);

        PlayerBehaviorStats stat = playerBehaviorStatService.applyAbility(playerId, Action.JACKPOT, null);
        PlayerStatus status = playerStatusService.applyStatus(actor, stat);

        playerDao.save(actor);
        String result = "Вы увеличили шанс джекпота на " + abilityTargetHelper.toPercent(critBonus)
            + "% и повысили шанс неудачи на " + abilityTargetHelper.toPercent(failBonus)
            + "% для следующего роста.";

        return CommandResult.of(
            CommandMessage.reply(result),
            CommandMessage.broadcast(status.getMessage())
        );
    }
}
