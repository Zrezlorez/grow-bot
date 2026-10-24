package com.litovskiy.bot.ds;

import com.litovskiy.bot.CommandResult;
import com.litovskiy.entity.Platform;
import com.litovskiy.exception.AccessDeniedException;
import com.litovskiy.exception.CommandNotFoundException;
import com.litovskiy.exception.MissingArgumentException;
import com.litovskiy.service.command.CommandContext;
import com.litovskiy.service.command.CommandManager;
import com.litovskiy.util.TargetUser;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordResponseBuilder {

    private final CommandManager commandManager;

    public CommandResult buildResponse(SlashCommandInteractionEvent event) {
        Optional<TargetUser> target = event.getOptions().stream()
            .filter(option -> option.getType() == OptionType.USER)
            .findFirst()
            .map(OptionMapping::getAsUser)
            .map(this::toTargetUser);

        String[] arguments = event.getOptions().stream()
            .filter(option -> option.getType() != OptionType.USER)
            .map(OptionMapping::getAsString)
            .toArray(String[]::new);

        CommandContext context = new CommandContext(
            event.getUser().getIdLong(),
            event.isFromGuild() ? event.getGuild().getIdLong() : null,
            target,
            Platform.DISCORD,
            event.getName(),
            arguments,
            false,
            canManageScope(event)
        );

        try {
            return commandManager.execute(context);
        } catch (CommandNotFoundException e) {
            log.debug("Unknown Discord command: {}", event.getName());
            return null;
        } catch (MissingArgumentException e) {
            return CommandResult.single(e.getMessage());
        } catch (AccessDeniedException e) {
            return CommandResult.single("Недостаточно прав для этой команды.");
        }
    }

    private TargetUser toTargetUser(User user) {
        return new TargetUser(user.getIdLong(), user.isBot());
    }

    private boolean canManageScope(SlashCommandInteractionEvent event) {
        return event.isFromGuild()
            && event.getMember() != null
            && event.getMember().hasPermission(Permission.MANAGE_SERVER);
    }
}
