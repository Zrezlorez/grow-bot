package com.litovskiy.entity;

import com.litovskiy.log.Action;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerBehaviorStatsTest {

    @Test
    void initializesStreaksOnFirstAbility() {
        PlayerBehaviorStats stats = new PlayerBehaviorStats(1L);

        stats.applyAbility(Action.TRANSFER, 2L);

        assertThat(stats.getLastAbilityAction()).isEqualTo(Action.TRANSFER);
        assertThat(stats.getActionTargetId()).isEqualTo(2L);
        assertThat(stats.getSameAbilityStreak()).isEqualTo(1);
        assertThat(stats.getSameAbilityAndTargetStreak()).isEqualTo(1);
    }

    @Test
    void incrementsStreaksForSameAbilityAndTarget() {
        PlayerBehaviorStats stats = new PlayerBehaviorStats(1L);

        stats.applyAbility(Action.TRANSFER, 2L);
        stats.applyAbility(Action.TRANSFER, 2L);
        stats.applyAbility(Action.TRANSFER, 2L);

        assertThat(stats.getSameAbilityStreak()).isEqualTo(3);
        assertThat(stats.getSameAbilityAndTargetStreak()).isEqualTo(3);
    }

    @Test
    void resetsTargetStreakWhenTargetChanges() {
        PlayerBehaviorStats stats = new PlayerBehaviorStats(1L);

        stats.applyAbility(Action.TRANSFER, 2L);
        stats.applyAbility(Action.TRANSFER, 2L);
        stats.applyAbility(Action.TRANSFER, 3L);

        assertThat(stats.getSameAbilityStreak()).isEqualTo(3);
        assertThat(stats.getSameAbilityAndTargetStreak()).isEqualTo(1);
        assertThat(stats.getActionTargetId()).isEqualTo(3L);
    }

    @Test
    void resetsStreaksWhenAbilityChanges() {
        PlayerBehaviorStats stats = new PlayerBehaviorStats(1L);

        stats.applyAbility(Action.TRANSFER, 2L);
        stats.applyAbility(Action.TRANSFER, 2L);
        stats.applyAbility(Action.PRAY, null);

        assertThat(stats.getLastAbilityAction()).isEqualTo(Action.PRAY);
        assertThat(stats.getActionTargetId()).isNull();
        assertThat(stats.getSameAbilityStreak()).isEqualTo(1);
        assertThat(stats.getSameAbilityAndTargetStreak()).isEqualTo(1);
    }
}
