package com.litovskiy.service.command.ability;

import com.litovskiy.bot.CommandResult;
import com.litovskiy.config.properties.AbilityProperties;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.PlayerBehaviorStats;
import com.litovskiy.log.Action;
import com.litovskiy.log.LogMetadata;
import com.litovskiy.log.metadata.TransferLogMetadata;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.ability.AbilityTargetContext;
import com.litovskiy.service.ability.AbilityTargetHelper;
import com.litovskiy.service.ability.PlayerStatus;
import com.litovskiy.service.ability.PlayerStatusService;
import com.litovskiy.service.command.CommandContext;
import com.litovskiy.service.data.PlayerService;
import com.litovskiy.service.log.GameLogService;
import com.litovskiy.service.log.PlayerBehaviorStatService;
import com.litovskiy.util.CommandBlockReason;
import com.litovskiy.util.TargetUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferCommandTest {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-01-01T12:00:00Z"),
        ZoneOffset.UTC
    );
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    private PlayerService playerDao;
    private GameLogService gameLogService;
    private ConversationStyleService conversationStyleService;
    private PlayerBehaviorStatService playerBehaviorStatService;
    private PlayerStatusService playerStatusService;
    private AbilityTargetHelper abilityTargetHelper;
    private TransferCommand command;

    @BeforeEach
    void setUp() {
        playerDao = mock(PlayerService.class);
        gameLogService = mock(GameLogService.class);
        conversationStyleService = mock(ConversationStyleService.class);
        playerBehaviorStatService = mock(PlayerBehaviorStatService.class);
        playerStatusService = mock(PlayerStatusService.class);
        abilityTargetHelper = mock(AbilityTargetHelper.class);

        AbilityProperties abilityProperties = new AbilityProperties();
        abilityProperties.setTransferCommission(0.15);

        command = new TransferCommand(
            playerDao,
            gameLogService,
            conversationStyleService,
            playerBehaviorStatService,
            playerStatusService,
            abilityProperties,
            abilityTargetHelper,
            CLOCK
        );
    }

    @Test
    void transfersValueAndDeductsCommissionFromReceivedAmount() {
        Player actor = player(1L, 1000.0);
        Player target = player(2L, 100.0);
        target.setTelegramDisplayName("Target");
        givenSuccessfulTarget(actor, target);
        givenAllowedAction(actor);

        when(playerStatusService.modifyTransferCommission(actor, 0.15)).thenReturn(0.15);
        PlayerBehaviorStats stat = new PlayerBehaviorStats(1L);
        stat.applyAbility(Action.TRANSFER, 2L);
        when(playerBehaviorStatService.applyAbility(1L, Action.TRANSFER, 2L)).thenReturn(stat);
        when(playerStatusService.applyStatus(actor, stat)).thenReturn(PlayerStatus.NONE);

        CommandResult result = command.execute(context(10L, target(2L, false), "100"));

        assertThat(actor.getSize()).isEqualTo(900.0);
        assertThat(target.getSize()).isEqualTo(185.0);
        assertThat(actor.getLastAbilityTime()).isEqualTo(NOW);
        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages().getFirst().getText()).isNotBlank();

        ArgumentCaptor<LogMetadata> metadata = ArgumentCaptor.forClass(LogMetadata.class);
        verify(gameLogService).logAbility(
            eq(1L),
            eq(1000.0),
            eq(900.0),
            eq(2L),
            eq(100.0),
            eq(185.0),
            eq(Action.TRANSFER),
            metadata.capture()
        );
        assertThat(metadata.getValue()).isInstanceOf(TransferLogMetadata.class);
        assertThat(((TransferLogMetadata) metadata.getValue()).cost()).isEqualTo(15.0);
        verify(playerDao).save(target);
        verify(playerDao).save(actor);
    }

    @Test
    void zeroCommissionCreditsFullTransferValue() {
        Player actor = player(1L, 1000.0);
        Player target = player(2L, 100.0);
        target.setTelegramDisplayName("Target");
        givenSuccessfulTarget(actor, target);
        givenAllowedAction(actor);

        when(playerStatusService.modifyTransferCommission(actor, 0.15)).thenReturn(0.0);
        PlayerBehaviorStats stat = new PlayerBehaviorStats(1L);
        stat.applyAbility(Action.TRANSFER, 2L);
        when(playerBehaviorStatService.applyAbility(1L, Action.TRANSFER, 2L)).thenReturn(stat);
        when(playerStatusService.applyStatus(actor, stat)).thenReturn(PlayerStatus.NONE);

        command.execute(context(10L, target(2L, false), "100"));

        assertThat(actor.getSize()).isEqualTo(900.0);
        assertThat(target.getSize()).isEqualTo(200.0);

        ArgumentCaptor<LogMetadata> metadata = ArgumentCaptor.forClass(LogMetadata.class);
        verify(gameLogService).logAbility(
            eq(1L),
            eq(1000.0),
            eq(900.0),
            eq(2L),
            eq(100.0),
            eq(200.0),
            eq(Action.TRANSFER),
            metadata.capture()
        );
        assertThat(((TransferLogMetadata) metadata.getValue()).cost()).isZero();
    }

    @Test
    void appliesPenaltyForNegativeTransferValue() {
        Player actor = player(1L, 1000.0);
        Player target = player(2L, 100.0);
        givenSuccessfulTarget(actor, target);
        givenAllowedAction(actor);

        CommandResult result = command.execute(context(10L, target(2L, false), "-5"));

        assertThat(actor.getSize()).isEqualTo(950.0);
        assertThat(actor.getLastAbilityTime()).isEqualTo(NOW);
        assertThat(result.messages().getFirst().getText()).isNotBlank();
        verify(playerDao).save(actor);
        verifyNoInteractions(gameLogService, playerBehaviorStatService);
    }

    @Test
    void rejectsTransferLargerThanCurrentSize() {
        Player actor = player(1L, 100.0);
        Player target = player(2L, 1000.0);
        givenSuccessfulTarget(actor, target);
        givenAllowedAction(actor);

        CommandResult result = command.execute(context(10L, target(2L, false), "100"));

        assertThat(result.messages().getFirst().getText()).isNotBlank();
        assertThat(actor.getSize()).isEqualTo(100.0);
        assertThat(target.getSize()).isEqualTo(1000.0);
        verifyNoInteractions(playerDao, gameLogService, playerBehaviorStatService);
    }

    @Test
    void rejectsBlockedAction() {
        Player actor = player(1L, 1000.0);
        Player target = player(2L, 100.0);
        givenSuccessfulTarget(actor, target);
        when(playerStatusService.ignoresAbilityCooldown(actor, Action.TRANSFER)).thenReturn(false);
        when(playerStatusService.validateActionAllowed(actor, Action.TRANSFER))
            .thenReturn(CommandBlockReason.createBlocked("blocked"));

        CommandResult result = command.execute(context(10L, target(2L, false), "100"));

        assertThat(result.messages().getFirst().getText()).isEqualTo("blocked");
        verifyNoInteractions(playerDao, gameLogService, playerBehaviorStatService);
    }

    @Test
    void rejectsBotTarget() {
        CommandResult result = command.execute(context(10L, target(2L, true), "100"));

        assertThat(result.messages().getFirst().getText()).isNotBlank();
        verifyNoInteractions(abilityTargetHelper, playerDao, gameLogService, playerBehaviorStatService);
    }

    @Test
    void rejectsDirectMessage() {
        CommandResult result = command.execute(context(null, target(2L, false), "100"));

        assertThat(result.messages().getFirst().getText()).isNotBlank();
        verifyNoInteractions(abilityTargetHelper, playerDao, gameLogService, playerBehaviorStatService);
    }

    @Test
    void rejectsMissingTarget() {
        CommandResult result = command.execute(context(10L, Optional.empty(), "100"));

        assertThat(result.messages().getFirst().getText()).isNotBlank();
        verifyNoInteractions(abilityTargetHelper, playerDao, gameLogService, playerBehaviorStatService);
    }

    @Test
    void rejectsMissingTransferValue() {
        CommandResult result = command.execute(context(10L, target(2L, false)));

        assertThat(result.messages().getFirst().getText()).isNotBlank();
        verifyNoInteractions(abilityTargetHelper, playerDao, gameLogService, playerBehaviorStatService);
    }

    private void givenSuccessfulTarget(Player actor, Player target) {
        when(abilityTargetHelper.prepareTargetAbility(Platform.TELEGRAM, 1L, 10L, 2L))
            .thenReturn(AbilityTargetContext.success(NOW, actor, target));
    }

    private void givenAllowedAction(Player actor) {
        when(playerStatusService.ignoresAbilityCooldown(actor, Action.TRANSFER)).thenReturn(false);
        when(playerStatusService.validateActionAllowed(actor, Action.TRANSFER))
            .thenReturn(CommandBlockReason.createAllowed());
        when(conversationStyleService.getStyle(Platform.TELEGRAM, 10L))
            .thenReturn(GrowthStyle.TREE);
    }

    private CommandContext context(Long chatId, Optional<TargetUser> target, String... arguments) {
        return new CommandContext(
            1L,
            chatId,
            target,
            Platform.TELEGRAM,
            "transfer",
            arguments,
            false,
            false
        );
    }

    private Optional<TargetUser> target(long id, boolean bot) {
        return Optional.of(new TargetUser(id, bot));
    }

    private Player player(long id, double size) {
        Player player = new Player(size);
        player.setId(id);
        return player;
    }
}
