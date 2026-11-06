package com.litovskiy.service.command;

import com.litovskiy.bot.CommandResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandRegistryTest {

    @Test
    void findsCommandByName() {
        CommandRegistry registry = registry(new GrowCommandStub());

        assertThat(registry.find("grow")).isPresent();
        assertThat(registry.find("GROW")).isPresent();
        assertThat(registry.find("/grow")).isPresent();
    }

    @Test
    void findsCommandByAlias() {
        CommandRegistry registry = registry(new GrowCommandStub());

        assertThat(registry.find("g")).isPresent();
        assertThat(registry.find("/G")).isPresent();
    }

    @Test
    void returnsEmptyForUnknownCommand() {
        CommandRegistry registry = registry(new GrowCommandStub());

        assertThat(registry.find("unknown")).isEmpty();
    }

    @Test
    void rejectsDuplicateCommandName() {
        assertThatThrownBy(() -> registry(new GrowCommandStub(), new DuplicateGrowCommandStub()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("grow");
    }

    @Test
    void rejectsDuplicateAlias() {
        assertThatThrownBy(() -> registry(new GrowCommandStub(), new AliasGrowCommandStub()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("g");
    }

    @Test
    void rejectsCommandWithoutAnnotation() {
        assertThatThrownBy(() -> registry(new UnannotatedCommandStub()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(UnannotatedCommandStub.class.getName());
    }

    @Test
    void listsEachCommandOnce() {
        CommandRegistry registry = registry(new GrowCommandStub(), new ProfileCommandStub());

        assertThat(registry.getAll()).hasSize(2);
    }

    private CommandRegistry registry(Command... commands) {
        return new CommandRegistry(List.of(commands));
    }

    private abstract static class BaseCommandStub implements Command {
        @Override
        public CommandResult execute(CommandContext context) {
            return CommandResult.single("ok");
        }
    }

    @BotCommand(name = "grow", aliases = "g")
    private static class GrowCommandStub extends BaseCommandStub {
    }

    @BotCommand(name = "grow")
    private static class DuplicateGrowCommandStub extends BaseCommandStub {
    }

    @BotCommand(name = "other", aliases = "g")
    private static class AliasGrowCommandStub extends BaseCommandStub {
    }

    @BotCommand(name = "profile")
    private static class ProfileCommandStub extends BaseCommandStub {
    }

    private static class UnannotatedCommandStub extends BaseCommandStub {
    }
}
