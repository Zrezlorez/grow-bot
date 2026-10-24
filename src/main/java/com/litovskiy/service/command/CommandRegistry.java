package com.litovskiy.service.command;

import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class CommandRegistry {
    private final Map<String, RegisteredCommand> commands = new HashMap<>();

    public CommandRegistry(List<Command> commandBeans) {
        for (Command command : commandBeans) {
            register(command);
        }
    }

    private void register(Command command) {
        BotCommand metadata = command.getClass()
            .getAnnotation(BotCommand.class);

        if (metadata == null) {
            throw new IllegalStateException(
                "У класса команды нет аннотации. Исправляй код в классе: "
                    + command.getClass().getName()
            );
        }

        RegisteredCommand registered = new RegisteredCommand(
            command,
            metadata
        );

        registerName(metadata.name(), registered);

        for (String alias : metadata.aliases()) {
            registerName(alias, registered);
        }
    }

    private void registerName(String name, RegisteredCommand command) {
        String normalized = normalize(name);

        RegisteredCommand existing = commands.putIfAbsent(normalized, command);
        if (existing != null) {
            throw new IllegalStateException(
                "Дублирующееся имя команды '" + normalized + "': "
                    + existing.command().getClass().getName()
                    + " и " + command.command().getClass().getName()
            );
        }
    }

    public Optional<RegisteredCommand> find(String name) {
        return Optional.ofNullable(
            commands.get(normalize(name))
        );
    }

    public Collection<RegisteredCommand> getAll() {
        return Collections.unmodifiableCollection(
            new HashSet<>(commands.values())
        );
    }

    private String normalize(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }

    public record RegisteredCommand(Command command, BotCommand metadata) {}
}
