package com.litovskiy.service.command;

import com.litovskiy.bot.CommandResult;

public interface Command {
    CommandResult execute(CommandContext context);
}
