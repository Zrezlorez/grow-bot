package com.litovskiy.service.command;

import com.litovskiy.entity.Platform;
import com.litovskiy.util.TargetUser;
import java.util.Optional;

public record CommandContext(
    long userId,
    Long chatId,
    Optional<TargetUser> targetId,
    Platform platform,
    String commandName,
    String[] arguments,
    Boolean isScheduledMessage,
    boolean canManageScope
) {
}
