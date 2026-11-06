package com.litovskiy.service.command;

import com.litovskiy.bot.CommandResult;
import com.litovskiy.entity.Platform;
import com.litovskiy.exception.AccessDeniedException;
import com.litovskiy.exception.CommandNotFoundException;
import com.litovskiy.exception.MissingArgumentException;
import com.litovskiy.service.data.AdminAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandManagerTest {

    private AdminAccessService accessService;

    @BeforeEach
    void setUp() {
        accessService = mock(AdminAccessService.class);
    }

    @Test
    void rejectsUnknownCommand() {
        CommandManager manager = manager(new PublicCommandStub());

        assertThatThrownBy(() -> manager.execute(context("missing")))
            .isInstanceOf(CommandNotFoundException.class)
            .hasMessageContaining("missing");
    }

    @Test
    void rejectsCommandOnUnsupportedPlatform() {
        TelegramOnlyCommandStub command = new TelegramOnlyCommandStub();
        CommandManager manager = manager(command);

        assertThatThrownBy(() -> manager.execute(context("telegram-only", Platform.DISCORD)))
            .isInstanceOf(CommandNotFoundException.class);
        assertThat(command.calls).isZero();
    }

    @Test
    void rejectsAdminCommandForRegularUser() {
        AdminCommandStub command = new AdminCommandStub();
        CommandManager manager = manager(command);
        when(accessService.isAdmin(Platform.TELEGRAM, 1L)).thenReturn(false);

        assertThatThrownBy(() -> manager.execute(context("admin")))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("admin");
        assertThat(command.calls).isZero();
    }

    @Test
    void allowsAdminCommandForAdmin() {
        AdminCommandStub command = new AdminCommandStub();
        CommandManager manager = manager(command);
        when(accessService.isAdmin(Platform.TELEGRAM, 1L)).thenReturn(true);

        CommandResult result = manager.execute(context("admin"));

        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().getFirst().getText()).isEqualTo("ok");
        assertThat(command.calls).isEqualTo(1);
    }

    @Test
    void rejectsMissingRequiredTextArgument() {
        CommandManager manager = manager(new TextArgumentCommandStub());

        assertThatThrownBy(() -> manager.execute(context("text")))
            .isInstanceOfSatisfying(MissingArgumentException.class,
                exception -> assertThat(exception.getArgumentName()).isEqualTo("value"));
    }

    @Test
    void allowsMissingOptionalTextArgument() {
        OptionalTextArgumentCommandStub command = new OptionalTextArgumentCommandStub();
        CommandManager manager = manager(command);

        manager.execute(context("optional-text"));

        assertThat(command.calls).isEqualTo(1);
    }

    @Test
    void skipsTargetUserArgumentDuringValidation() {
        UserArgumentCommandStub command = new UserArgumentCommandStub();
        CommandManager manager = manager(command);

        manager.execute(context("user", Platform.TELEGRAM, "100"));

        assertThat(command.calls).isEqualTo(1);
    }

    @Test
    void requiresTextArgumentAfterSkippedUserArgument() {
        UserArgumentCommandStub command = new UserArgumentCommandStub();
        CommandManager manager = manager(command);

        assertThatThrownBy(() -> manager.execute(context("user")))
            .isInstanceOfSatisfying(MissingArgumentException.class,
                exception -> assertThat(exception.getArgumentName()).isEqualTo("value"));
        assertThat(command.calls).isZero();
    }

    @Test
    void passesContextToCommand() {
        CommandManager manager = manager(new PublicCommandStub());
        CommandContext context = context("public");

        CommandResult result = manager.execute(context);

        assertThat(result.messages().getFirst().getText()).isEqualTo("ok");
    }

    private CommandManager manager(Command... commands) {
        return new CommandManager(new CommandRegistry(List.of(commands)), accessService);
    }

    private CommandContext context(String commandName) {
        return context(commandName, Platform.TELEGRAM);
    }

    private CommandContext context(String commandName, Platform platform, String... arguments) {
        return new CommandContext(
            1L,
            10L,
            Optional.empty(),
            platform,
            commandName,
            arguments,
            false,
            false
        );
    }

    private abstract static class BaseCommandStub implements Command {
        protected int calls;

        @Override
        public CommandResult execute(CommandContext context) {
            calls++;
            return CommandResult.single("ok");
        }
    }

    @BotCommand(name = "public")
    private static class PublicCommandStub extends BaseCommandStub {
    }

    @BotCommand(name = "telegram-only", platforms = Platform.TELEGRAM)
    private static class TelegramOnlyCommandStub extends BaseCommandStub {
    }

    @BotCommand(name = "admin", access = BotCommand.Access.ADMIN)
    private static class AdminCommandStub extends BaseCommandStub {
    }

    @BotCommand(
        name = "text",
        arguments = @BotCommand.CommandArgument(
            name = "value",
            required = true,
            type = BotCommand.ArgumentType.STRING
        )
    )
    private static class TextArgumentCommandStub extends BaseCommandStub {
    }

    @BotCommand(
        name = "optional-text",
        arguments = @BotCommand.CommandArgument(
            name = "value",
            type = BotCommand.ArgumentType.STRING
        )
    )
    private static class OptionalTextArgumentCommandStub extends BaseCommandStub {
    }

    @BotCommand(
        name = "user",
        arguments = {
            @BotCommand.CommandArgument(
                name = "target",
                required = true,
                type = BotCommand.ArgumentType.USER
            ),
            @BotCommand.CommandArgument(
                name = "value",
                required = true,
                type = BotCommand.ArgumentType.NUMBER
            )
        }
    )
    private static class UserArgumentCommandStub extends BaseCommandStub {
    }
}
