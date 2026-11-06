package com.litovskiy.util;

import com.litovskiy.entity.Player;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilTest {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-01-01T12:00:00Z"),
        ZoneOffset.UTC
    );
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Test
    void roundsToTwoDecimalPlaces() {
        assertThat(StringUtil.round(1.234)).isEqualTo(1.23);
        assertThat(StringUtil.round(1.235)).isEqualTo(1.24);
        assertThat(StringUtil.round(-1.235)).isEqualTo(-1.24);
    }

    @Test
    void formatsDuration() {
        assertThat(StringUtil.formatDuration(Duration.ZERO)).isEqualTo("0 сек");
        assertThat(StringUtil.formatDuration(Duration.ofSeconds(59))).isEqualTo("59 сек");
        assertThat(StringUtil.formatDuration(Duration.ofMinutes(1))).isEqualTo("1 мин 0 сек");
        assertThat(StringUtil.formatDuration(Duration.ofHours(2).plusMinutes(3).plusSeconds(4)))
            .isEqualTo("2 ч 3 мин 4 сек");
    }

    @Test
    void escapesHtml() {
        assertThat(StringUtil.escapeHtml(null)).isEmpty();
        assertThat(StringUtil.escapeHtml("<a href=\"x\">&")).isEqualTo("&lt;a href=&quot;x&quot;&gt;&amp;");
    }

    @Test
    void returnsEmptyCooldownWhenLastActionIsMissing() {
        assertThat(StringUtil.getCooldown(null, Duration.ofHours(8), CLOCK)).isEmpty();
    }

    @Test
    void returnsRemainingCooldown() {
        LocalDateTime lastAction = NOW.minusHours(1);

        assertThat(StringUtil.getCooldown(lastAction, Duration.ofHours(8), CLOCK))
            .contains(Duration.ofHours(7));
    }

    @Test
    void returnsEmptyCooldownWhenCooldownIsOver() {
        assertThat(StringUtil.getCooldown(NOW.minusHours(8), Duration.ofHours(8), CLOCK)).isEmpty();
        assertThat(StringUtil.getCooldown(NOW.minusHours(9), Duration.ofHours(8), CLOCK)).isEmpty();
    }

    @Test
    void formatsTelegramDisplayName() {
        assertThat(StringUtil.formatTelegramDisplayName("Ivan", "Petrov", "ivan", 1L))
            .isEqualTo("Ivan Petrov");
        assertThat(StringUtil.formatTelegramDisplayName(null, null, "ivan", 1L))
            .isEqualTo("@ivan");
        assertThat(StringUtil.formatTelegramDisplayName(null, null, null, 5L))
            .isEqualTo("Пользователь 5");
    }

    @Test
    void escapesTelegramNamesInLeaderboard() {
        Player player = new Player(100);
        player.setTelegramChatId(42L);
        player.setTelegramDisplayName("A < B & C");

        assertThat(StringUtil.formatTelegramPlayer(player, 10L))
            .isEqualTo("A &lt; B &amp; C");
        assertThat(StringUtil.formatTelegramPlayer(player, null))
            .contains("tg://user?id=42")
            .contains("A &lt; B &amp; C");
    }

    @Test
    void fallsBackToTelegramIdWhenDisplayNameIsMissing() {
        Player player = new Player(100);
        player.setTelegramChatId(42L);
        player.setTelegramDisplayName("  ");

        assertThat(StringUtil.formatTelegramPlayer(player, 10L)).isEqualTo("id 42");
        assertThat(StringUtil.formatTelegramPlayer(player, null))
            .isEqualTo("<a href=\"tg://user?id=42\">id 42</a>");
    }
}
