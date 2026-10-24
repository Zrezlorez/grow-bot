package com.litovskiy.bot.tg;

import com.litovskiy.bot.CommandResult;
import com.litovskiy.entity.Platform;
import com.litovskiy.exception.AccessDeniedException;
import com.litovskiy.exception.CommandNotFoundException;
import com.litovskiy.exception.MissingArgumentException;
import com.litovskiy.service.PlayerAccountService;
import com.litovskiy.service.command.CommandContext;
import com.litovskiy.service.command.CommandManager;
import com.litovskiy.util.StringUtil;
import com.litovskiy.util.TargetUser;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgResponseBuilder {

    private static final Set<String> TARGET_PROFILE_COMMANDS = Set.of(
        "fuck",
        "slow",
        "transfer"
    );

    private final CommandManager commandManager;
    private final PlayerAccountService playerAccountService;

    public CommandResult build(Update update, String command, String[] commandParts, long chatId, long profileId) {
        Message message = update.getMessage();
        User replyTarget = extractReplyTarget(message);
        Optional<TargetUser> target = Optional.ofNullable(replyTarget)
            .map(user -> new TargetUser(user.getId(), user.getIsBot()));

        String commandName = normalizeCommandName(command);

        if (replyTarget != null
            && chatId < 0
            && !replyTarget.getIsBot()
            && TARGET_PROFILE_COMMANDS.contains(commandName)) {
            playerAccountService.updateTelegramProfile(
                replyTarget.getId(),
                StringUtil.formatTelegramDisplayName(replyTarget),
                replyTarget.getUserName()
            );
        }

        CommandContext context = new CommandContext(
            profileId,
            chatId < 0 ? chatId : null,
            target,
            Platform.TELEGRAM,
            commandName,
            extractArguments(commandParts),
            message.getIsFromOffline(),
            true
        );

        try {
            return commandManager.execute(context);
        } catch (CommandNotFoundException e) {
            log.debug("Unknown Telegram command: {}", commandName);
            return null;
        } catch (MissingArgumentException e) {
            return CommandResult.single(e.getMessage());
        } catch (AccessDeniedException e) {
            return CommandResult.single("Недостаточно прав для этой команды.");
        }
    }

    private String[] extractArguments(String[] commandParts) {
        return commandParts.length > 1
            ? new String[]{commandParts[1]}
            : new String[0];
    }

    private String normalizeCommandName(String command) {
        if (command == null) {
            return "";
        }

        String normalized = command.trim();
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }

    private User extractReplyTarget(Message message) {
        Message replyMessage = message.getReplyToMessage();
        if (replyMessage == null
            || replyMessage.getMessageId().equals(replyMessage.getMessageThreadId())) {
            return null;
        }
        return replyMessage.getFrom();
    }
}
