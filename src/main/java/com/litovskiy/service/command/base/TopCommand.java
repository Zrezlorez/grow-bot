package com.litovskiy.service.command.base;

import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.CommandResult;
import com.litovskiy.config.properties.GameProperties;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.repository.ConversationParticipantRepository;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.command.BotCommand;
import com.litovskiy.service.command.Command;
import com.litovskiy.service.command.CommandContext;
import com.litovskiy.service.data.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import static com.litovskiy.entity.GrowthStyle.convertValue;
import static com.litovskiy.util.StringUtil.formatTelegramPlayer;

@BotCommand(
    name = "top",
    description = "увидеть топ игроков в своей беседе (в личных сообщениях - глобальный топ)"
)
@Component
@RequiredArgsConstructor
public class TopCommand implements Command {


    private final PlayerService playerDao;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationStyleService conversationStyleService;
    private final GameProperties gameProperties;

    @Override
    public CommandResult execute(CommandContext context) {
        Platform platform = context.platform();
        Long chatId = context.chatId();
        boolean isChannel = chatId == null;
        int limit = gameProperties.getLeaderboardLimit();
        List<Player> rankedPlayers = isChannel
            ? playerDao.findTopByPlatform(platform, limit)
            : findScopeLeaderboard(platform, chatId);

        if (rankedPlayers.isEmpty()) {
            return CommandResult.single(isChannel
                ? "Глобальный лидерборд пока пуст."
                : "В этой беседе пока нет участников для лидерборда.");
        }

        GrowthStyle style = conversationStyleService.getStyle(platform, chatId);
        StringBuilder builder = new StringBuilder(isChannel
            ? "Глобальный топ по показателю "
            : "Топ этой беседы по показателю ");
        builder.append(style.getDisplayName()).append(":\n");

        int rows = Math.min(limit, rankedPlayers.size());
        for (int index = 0; index < rows; index++) {
            Player player = rankedPlayers.get(index);
            builder.append(index + 1)
                .append(". ")
                .append(formatPlayer(platform, player, chatId))
                .append(" — ")
                .append(convertValue(player.getSize(), style));

            if (Objects.equals(platform.getProfileId(player), context.userId())) {
                builder.append(" ← вы");
            }

            builder.append('\n');
        }

        appendRequesterPlace(builder, rankedPlayers, platform, context.userId(), rows);
        return CommandResult.of(
            CommandMessage.reply(builder.toString().trim(), platform == Platform.TELEGRAM)
        );
    }


    private List<Player> findScopeLeaderboard(Platform platform, long scopeId) {
        List<Long> participantIds = conversationParticipantRepository.findByPlatformAndScopeId(platform, scopeId);
        return playerDao.findByIds(participantIds).stream()
            .filter(player -> platform.getProfileId(player) != null)
            .sorted(Comparator.comparingDouble(Player::getSize).reversed())
            .toList();
    }

    private void appendRequesterPlace(StringBuilder builder,
                                      List<Player> rankedPlayers,
                                      Platform platform,
                                      long requesterProfileId,
                                      int visibleRows) {
        for (int index = 0; index < rankedPlayers.size(); index++) {
            if (Objects.equals(platform.getProfileId(rankedPlayers.get(index)), requesterProfileId) && index >= visibleRows) {
                builder.append("\nВаше место: ")
                    .append(index + 1)
                    .append(" из ")
                    .append(rankedPlayers.size());
                return;
            }
        }
    }

    private String formatPlayer(Platform platform, Player player, Long scopeId) {
        Long profileId = platform.getProfileId(player);
        if (profileId == null) {
            return "аккаунт " + player.getId();
        }

        return switch (platform) {
            case TELEGRAM -> formatTelegramPlayer(player, scopeId);
            case DISCORD -> "<@" + profileId + ">";
        };
    }
}
