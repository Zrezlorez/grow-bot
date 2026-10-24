package com.litovskiy.service.command.base;

import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.CommandResult;
import com.litovskiy.config.properties.GrowthProperties;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.PlayerAccountService;
import com.litovskiy.service.ability.AbilityTargetHelper;
import com.litovskiy.service.ability.PlayerStatusService;
import com.litovskiy.service.command.BotCommand;
import com.litovskiy.service.command.Command;
import com.litovskiy.service.command.CommandContext;
import com.litovskiy.util.StringUtil;
import com.litovskiy.util.TargetUser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.Optional;
import static com.litovskiy.entity.GrowthStyle.convertValue;

@BotCommand(
    name = "profile",
    description = "посмотреть профиль (свой или другого игрока)",
    arguments = {
        @BotCommand.CommandArgument(
            name = "user",
            type = BotCommand.ArgumentType.USER,
            description = "Игрок"
        )
    }
)
@Component
@RequiredArgsConstructor
public class ProfileCommand implements Command {

    private final PlayerAccountService playerAccountService;
    private final ConversationStyleService conversationStyleService;
    private final PlayerStatusService playerStatusService;
    private final GrowthProperties growthProperties;
    private final AbilityTargetHelper abilityTargetHelper;
    private final Clock clock;

    @Override
    public CommandResult execute(CommandContext context) {
        if (context.targetId().map(TargetUser::bot).orElse(false)) {
            return CommandResult.single("Бот нищий, у него нет профиля");
        }

        return buildProfileResponse(context.platform(), context.userId(), context.targetId(), context.chatId());
    }

    public CommandResult buildProfileResponse(Platform platform, long playerId, Optional<TargetUser> target, Long scopeId) {
        long currentPlayerId = target.map(TargetUser::id).orElse(playerId);
        Player player = playerAccountService.resolveOrCreate(platform, currentPlayerId);
        GrowthStyle style = conversationStyleService.getStyle(platform, scopeId);

        String profileName = switch (platform) {
            case TELEGRAM -> player.getTelegramDisplayName();
            case DISCORD -> player.getDiscordTag();
        };
        if (profileName == null || profileName.isBlank()) {
            profileName = "Игрок " + player.getId();
        }

        String text = """
                Профиль игрока %s
                
                %s: %s
                Активный статус: %s
                
                Время до следующего роста: %s
                Время до следующей способности: %s
                """.formatted(
            profileName,
            StringUtils.capitalize(style.getDisplayName()),
            convertValue(player.getSize(), style),
            playerStatusService.getActiveStatus(player).getName(),
            StringUtil.getCooldown(player.getLastGrowTime(), growthProperties.getGrowthCooldown(), clock).map(StringUtil::formatDuration).orElse("Доступно сейчас"),
            abilityTargetHelper.getAbilityCooldown(player).map(StringUtil::formatDuration).orElse("Доступно сейчас"));

        return CommandResult.of(CommandMessage.reply(text));
    }
}
