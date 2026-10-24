package com.litovskiy.bot.tg;

import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.MessageDelivery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class TgMessageSender {

    public Message sendMessage(
        TelegramClient telegramClient,
        long chatId,
        Integer messageThreadId,
        Integer replyToMessageId,
        CommandMessage message
    ) {
        if (message == null || message.getText() == null || message.getText().isBlank()) {
            return null;
        }

        Integer actualReplyToMessageId = message.getDelivery() == MessageDelivery.REPLY
            ? replyToMessageId
            : null;

        var builder = SendMessage.builder()
            .chatId(chatId)
            .text(message.getText());

        if (messageThreadId != null) {
            builder.messageThreadId(messageThreadId);
        }

        if (actualReplyToMessageId != null) {
            builder.replyToMessageId(actualReplyToMessageId);
        }

        if (message.getHtml()) {
            builder.parseMode("HTML");
            builder.disableWebPagePreview(true);
        }

        try {
            return telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            log.warn("Failed to send Telegram message. chatId={}, replyTo={}", chatId, actualReplyToMessageId, e);
            return null;
        }
    }
}
