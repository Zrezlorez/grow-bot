package com.litovskiy.bot;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommandMessage {

    private String text;
    private MessageDelivery delivery;
    private Boolean html;

    public static CommandMessage reply(String text) {
        return new CommandMessage(text, MessageDelivery.REPLY, false);
    }

    public static CommandMessage reply(String text, boolean html) {
        return new CommandMessage(text, MessageDelivery.REPLY, html);
    }

    public static CommandMessage broadcast(String text) {
        return new CommandMessage(text, MessageDelivery.BROADCAST, false);
    }
}
