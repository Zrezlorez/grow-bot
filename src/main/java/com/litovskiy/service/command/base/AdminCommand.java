package com.litovskiy.service.command.base;

import com.litovskiy.bot.CommandResult;
import com.litovskiy.service.AdminCommandService;
import com.litovskiy.service.command.BotCommand;
import com.litovskiy.service.command.Command;
import com.litovskiy.service.command.CommandContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@BotCommand(
    name = "admin",
    description = "админ-команды",
    access = BotCommand.Access.ADMIN,
    arguments = {
        @BotCommand.CommandArgument(
            name = "command",
            type = BotCommand.ArgumentType.STRING,
            description = "Например: config, player show telegram @name"
        )
    }
)
@Component
@RequiredArgsConstructor
public class AdminCommand implements Command {

    private final AdminCommandService adminCommandService;

    @Override
    public CommandResult execute(CommandContext context) {
        String rawCommand = context.arguments().length > 0 ? context.arguments()[0] : "";

        return adminCommandService.handle(
            context.platform(),
            context.userId(),
            rawCommand
        );
    }
}
