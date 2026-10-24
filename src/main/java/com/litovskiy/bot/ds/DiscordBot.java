package com.litovskiy.bot.ds;

import com.litovskiy.entity.Platform;
import com.litovskiy.service.data.ConversationParticipantService;
import com.litovskiy.service.PlayerAccountService;
import com.litovskiy.service.activity.ActivityService;
import com.litovskiy.bot.CommandResult;
import com.litovskiy.service.command.BotCommand;
import com.litovskiy.service.command.CommandRegistry;
import com.litovskiy.util.StringUtil;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordBot extends ListenerAdapter {

    private final DiscordResponseBuilder discordResponseBuilder;
    private final CommandRegistry commandRegistry;
    private final ActivityService activityService;
    private final ConversationParticipantService conversationParticipantService;
    private final PlayerAccountService playerAccountService;
    private final OkHttpClient httpClient;
    private final DiscordMessageSender sender;

    @Value("${discord.token}")
    private String token;

    @SneakyThrows
    public void start() {
        if (token == null || token.isBlank()) {
            log.warn("Discord bot is not enabled because token is empty");
            return;
        }
        JDA jda = JDABuilder.createDefault(token)
            .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES)
            .setHttpClient(httpClient)
            .addEventListeners(this)
            .build();

        jda.awaitReady();
        registerCommands(jda);
    }

    private void registerCommands(JDA jda) {
        List<SlashCommandData> commands = commandRegistry.getAll().stream()
            .filter(registered -> List.of(registered.metadata().platforms()).contains(Platform.DISCORD))
            .sorted(Comparator.comparing(registered -> registered.metadata().name()))
            .map(this::toSlashCommand)
            .toList();

        jda.updateCommands()
            .addCommands(commands)
            .queue();
    }

    private SlashCommandData toSlashCommand(CommandRegistry.RegisteredCommand registered) {
        BotCommand metadata = registered.metadata();
        SlashCommandData command = Commands.slash(metadata.name(), metadata.description());

        for (BotCommand.CommandArgument argument : metadata.arguments()) {
            command.addOption(
                toOptionType(argument.type()),
                argument.name(),
                optionDescription(argument),
                argument.required()
            );
        }

        return command;
    }

    private OptionType toOptionType(BotCommand.ArgumentType type) {
        return switch (type) {
            case STRING -> OptionType.STRING;
            case NUMBER -> OptionType.NUMBER;
            case USER -> OptionType.USER;
        };
    }

    private String optionDescription(BotCommand.CommandArgument argument) {
        return argument.description().isBlank()
            ? argument.name()
            : argument.description();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        playerAccountService.updateDiscordTag(event.getUser().getIdLong(), StringUtil.formatDiscordTag(event.getUser()));
        if (event.isFromGuild()) {
            conversationParticipantService.registerParticipant(
                Platform.DISCORD,
                event.getUser().getIdLong(),
                event.getGuild().getIdLong()
            );
        }
        CommandResult response = discordResponseBuilder.buildResponse(event);

        sender.sendMessages(event, response);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.isWebhookMessage()) {
            return;
        }

        playerAccountService.updateDiscordTag(event.getAuthor().getIdLong(), StringUtil.formatDiscordTag(event.getAuthor()));
        conversationParticipantService.registerParticipant(
            Platform.DISCORD,
            event.getAuthor().getIdLong(),
            event.getGuild().getIdLong()
        );
        activityService.recordMessage(Platform.DISCORD, event.getAuthor().getIdLong(), event.getGuild().getIdLong());
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getMember().getUser().isBot()) {
            return;
        }

        conversationParticipantService.registerParticipant(
            Platform.DISCORD,
            event.getMember().getIdLong(),
            event.getGuild().getIdLong()
        );

        if (event.getChannelLeft() != null && event.getChannelJoined() == null) {
            activityService.endVoiceSession(Platform.DISCORD, event.getMember().getIdLong());
            return;
        }

        if (event.getChannelJoined() != null && event.getChannelLeft() == null) {
            activityService.startVoiceSession(Platform.DISCORD, event.getMember().getIdLong(), event.getGuild().getIdLong());
        }
    }
}
