package com.litovskiy.service.command.base;

import com.litovskiy.bot.CommandResult;
import com.litovskiy.service.LinkService;
import com.litovskiy.service.command.BotCommand;
import com.litovskiy.service.command.Command;
import com.litovskiy.service.command.CommandContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@BotCommand(
    name = "link",
    description = "привязать свой тг/дс (рекомендуется использовать в лс с ботом)",
    arguments = {
        @BotCommand.CommandArgument(
            name = "code",
            type = BotCommand.ArgumentType.STRING,
            description = "Код из другого бота"
        )
    }
)
@Component
@RequiredArgsConstructor
public class LinkCommand implements Command {

    private final LinkService linkService;

    @Override
    public CommandResult execute(CommandContext context) {
        String code = context.arguments().length > 0 ? context.arguments()[0] : null;

        return CommandResult.single(code == null || code.isBlank()
            ? linkService.createCode(context.platform(), context.userId())
            : linkService.linkProfile(context.platform(), context.userId(), code));
    }
}
