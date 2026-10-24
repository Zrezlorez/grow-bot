package com.litovskiy.service.command;

import com.litovskiy.bot.CommandResult;
import com.litovskiy.exception.AccessDeniedException;
import com.litovskiy.exception.CommandNotFoundException;
import com.litovskiy.exception.MissingArgumentException;
import com.litovskiy.service.data.AdminAccessService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CommandManager {

    private final CommandRegistry registry;
    private final AdminAccessService accessChecker;

    public CommandResult execute(CommandContext context) {
        CommandRegistry.RegisteredCommand registered =
            registry.find(context.commandName())
                .orElseThrow(() ->
                    new CommandNotFoundException(
                        context.commandName()
                    )
                );

        BotCommand metadata = registered.metadata();

        validatePlatform(metadata, context);
        validateAccess(metadata, context);
        validateArguments(metadata, context);

        return registered.command().execute(context);
    }

    private void validatePlatform(BotCommand metadata, CommandContext context) {
        if (!List.of(metadata.platforms()).contains(context.platform())) {
            throw new CommandNotFoundException(context.commandName());
        }
    }

    private void validateAccess(BotCommand metadata, CommandContext context) {
        if (metadata.access() == BotCommand.Access.ADMIN
            && !accessChecker.isAdmin(context.platform(), context.userId())) {
            throw new AccessDeniedException(context.userId(), context.platform(), context.commandName());
        }
    }

    private void validateArguments(BotCommand metadata, CommandContext context) {
        BotCommand.CommandArgument[] definitions =
            metadata.arguments();

        String[] arguments = context.arguments();
        int textArgumentIndex = 0;

        for (BotCommand.CommandArgument argument : definitions) {
            if (argument.type() == BotCommand.ArgumentType.USER) {
                continue;
            }

            if (argument.required() && textArgumentIndex >= arguments.length) {
                throw new MissingArgumentException(argument.name());
            }

            textArgumentIndex++;
        }
    }
}
