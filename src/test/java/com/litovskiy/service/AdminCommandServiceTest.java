package com.litovskiy.service;

import com.litovskiy.bot.CommandResult;
import com.litovskiy.config.cloud.GithubConfigService;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.data.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminCommandServiceTest {

    private PlayerService playerDao;
    private AdminCommandService service;

    @BeforeEach
    void setUp() {
        playerDao = mock(PlayerService.class);
        service = new AdminCommandService(
            playerDao,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            mock(GithubConfigService.class)
        );
    }

    @Test
    void showsPlayerWithMissingTimes() {
        Player player = player(1L, 10.0);
        player.setTelegramChatId(123L);
        when(playerDao.findByTelegramUsername("123")).thenReturn(player);

        CommandResult result = service.handle(Platform.TELEGRAM, 1L, "player show telegram 123");

        String text = result.messages().getFirst().getText();
        assertThat(text)
            .contains("playerId = 1")
            .contains("lastGrowTime = не задан")
            .contains("lastAbilityTime = не задан")
            .contains("telegramChatId = 123");
    }

    @Test
    void showsPlayerWithTimes() {
        Player player = player(2L, 20.0);
        player.setTelegramChatId(456L);
        player.setLastGrowTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        player.setLastAbilityTime(LocalDateTime.of(2026, 1, 1, 11, 0));
        when(playerDao.findByTelegramUsername("456")).thenReturn(player);

        String text = service.handle(Platform.TELEGRAM, 1L, "player show telegram 456")
            .messages()
            .getFirst()
            .getText();

        assertThat(text)
            .contains("playerId = 2")
            .contains("lastGrowTime = 2026-01-01T10:00")
            .contains("lastAbilityTime = 2026-01-01T11:00");
    }

    private Player player(long id, double size) {
        Player player = new Player(size);
        player.setId(id);
        return player;
    }
}
