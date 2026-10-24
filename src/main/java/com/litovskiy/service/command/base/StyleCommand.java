package com.litovskiy.service.command.base;

import com.litovskiy.bot.CommandResult;
import com.litovskiy.entity.Platform;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.command.BotCommand;
import com.litovskiy.service.command.Command;
import com.litovskiy.service.command.CommandContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@BotCommand(
    name = "style",
    description = "посмотреть или изменить стиль роста в беседе",
    arguments = {
        @BotCommand.CommandArgument(
            name = "name",
            type = BotCommand.ArgumentType.STRING,
            description = "Название стиля"
        )
    }
)
@Component
@RequiredArgsConstructor
public class StyleCommand implements Command {

    private final ConversationStyleService conversationStyleService;

    @Override
    public CommandResult execute(CommandContext context) {
        Platform platform = context.platform();
        Long scopeId = context.chatId();

        if (scopeId == null) {
            return CommandResult.single(platform == Platform.TELEGRAM
                ? "Стиль настраивается только в группах."
                : "Стиль можно менять только на сервере.");
        }

        String styleName = context.arguments().length > 0 ? context.arguments()[0] : null;
        if (styleName == null || styleName.isBlank()) {
            return CommandResult.single(conversationStyleService.describeCurrentStyle(platform, scopeId));
        }

        if (platform == Platform.DISCORD && !context.canManageScope()) {
            return CommandResult.single("Менять стиль сервера могут только участники с правом Manage Server.");
        }

        return CommandResult.single(platform == Platform.TELEGRAM
            ? conversationStyleService.updateTelegramStyle(scopeId, context.userId(), styleName)
            : conversationStyleService.updateDiscordStyle(scopeId, styleName));
    }
}
