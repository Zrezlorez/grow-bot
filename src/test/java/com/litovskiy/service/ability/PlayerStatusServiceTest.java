package com.litovskiy.service.ability;

import com.litovskiy.config.properties.AbilityProperties;
import com.litovskiy.config.properties.GrowthProperties;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.PlayerBehaviorStats;
import com.litovskiy.log.Action;
import com.litovskiy.service.log.PlayerBehaviorStatService;
import com.litovskiy.util.CommandBlockReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PlayerStatusServiceTest {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-01-01T12:00:00Z"),
        ZoneOffset.UTC
    );
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    private PlayerStatusService service;
    private PlayerBehaviorStatService behaviorStatService;

    @BeforeEach
    void setUp() {
        behaviorStatService = mock(PlayerBehaviorStatService.class);
        service = new PlayerStatusService(
            CLOCK,
            behaviorStatService,
            new AbilityProperties(),
            new GrowthProperties()
        );
    }

    @Test
    void traderPaysNoTransferCommission() {
        Player player = playerWithStatus(PlayerStatus.TRADER, NOW.plusHours(1));

        assertThat(service.modifyTransferCommission(player, 0.15)).isZero();
    }

    @Test
    void regularPlayerPaysTransferCommission() {
        Player player = playerWithStatus(PlayerStatus.NONE, null);

        assertThat(service.modifyTransferCommission(player, 0.15)).isEqualTo(0.15);
    }

    @Test
    void expiredTraderPaysTransferCommission() {
        Player player = playerWithStatus(PlayerStatus.TRADER, NOW.minusSeconds(1));

        assertThat(service.modifyTransferCommission(player, 0.15)).isEqualTo(0.15);
    }

    @Test
    void traderIgnoresTransferCooldownOnly() {
        Player player = playerWithStatus(PlayerStatus.TRADER, NOW.plusHours(1));

        assertThat(service.ignoresAbilityCooldown(player, Action.TRANSFER)).isTrue();
        assertThat(service.ignoresAbilityCooldown(player, Action.FUCK)).isFalse();
    }

    @Test
    void expiredStatusBecomesNone() {
        Player player = playerWithStatus(PlayerStatus.TRADER, NOW.minusSeconds(1));

        assertThat(service.getActiveStatus(player)).isEqualTo(PlayerStatus.NONE);
        assertThat(player.getStatus()).isEqualTo(PlayerStatus.NONE);
        assertThat(player.getStatusUntil()).isNull();
    }

    @Test
    void ludomaniaBlocksSelfBuffAbilities() {
        Player player = playerWithStatus(PlayerStatus.LUDOMANIA, NOW.plusHours(1));

        CommandBlockReason blocked = service.validateActionAllowed(player, Action.TURTLE);
        CommandBlockReason allowed = service.validateActionAllowed(player, Action.TRANSFER);

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.message()).isNotBlank();
        assertThat(allowed.allowed()).isTrue();
    }

    @Test
    void crudDoublesSlowGrowthModifier() {
        Player actor = playerWithStatus(PlayerStatus.CRUD, NOW.plusHours(1));
        Player target = playerWithStatus(PlayerStatus.NONE, null);

        assertThat(service.modifyGrowthModifier(actor, target, Action.SLOW, 0.2))
            .isEqualTo(0.4);
    }

    @Test
    void priestTargetBlocksSlowGrowthModifier() {
        Player actor = playerWithStatus(PlayerStatus.NONE, null);
        Player target = playerWithStatus(PlayerStatus.PRIEST, NOW.plusHours(1));

        assertThat(service.modifyGrowthModifier(actor, target, Action.SLOW, 0.2)).isZero();
    }

    @Test
    void shaoLinSetsTurtleGrowthModifierToHalf() {
        Player actor = playerWithStatus(PlayerStatus.SHAO_LIN, NOW.plusHours(1));
        Player target = playerWithStatus(PlayerStatus.NONE, null);

        assertThat(service.modifyGrowthModifier(actor, target, Action.TURTLE, 0.2))
            .isEqualTo(0.5);
    }

    @Test
    void doesNotApplyStatusBeforeThirdSameAbility() {
        Player player = playerWithStatus(PlayerStatus.NONE, null);
        PlayerBehaviorStats stat = new PlayerBehaviorStats(1L);
        stat.applyAbility(Action.TRANSFER, 2L);
        stat.applyAbility(Action.TRANSFER, 2L);

        assertThat(service.applyStatus(player, stat)).isEqualTo(PlayerStatus.NONE);
        assertThat(player.getStatus()).isEqualTo(PlayerStatus.NONE);
    }

    @Test
    void appliesStatusOnThirdSameAbility() {
        Player player = playerWithStatus(PlayerStatus.NONE, null);
        PlayerBehaviorStats stat = new PlayerBehaviorStats(1L);
        stat.applyAbility(Action.TRANSFER, 2L);
        stat.applyAbility(Action.TRANSFER, 2L);
        stat.applyAbility(Action.TRANSFER, 2L);

        assertThat(service.applyStatus(player, stat)).isEqualTo(PlayerStatus.TRADER);
        assertThat(player.getStatus()).isEqualTo(PlayerStatus.TRADER);
        assertThat(player.getStatusUntil()).isEqualTo(NOW.plusHours(36));
        assertThat(stat.getSameAbilityStreak()).isZero();
        assertThat(stat.getLastAbilityAction()).isNull();
        verify(behaviorStatService).save(stat);
    }

    private Player playerWithStatus(PlayerStatus status, LocalDateTime statusUntil) {
        Player player = new Player(100);
        player.setStatus(status);
        player.setStatusUntil(statusUntil);
        return player;
    }
}
