package com.litovskiy.service.command.base;

import com.litovskiy.bot.CommandResult;
import com.litovskiy.service.command.BotCommand;
import com.litovskiy.service.command.Command;
import com.litovskiy.service.command.CommandContext;
import org.springframework.stereotype.Component;

@BotCommand(
    name = "help",
    aliases = {"start"},
    description = "список всех команд"
)
@Component
public class HelpCommand implements Command {

    @Override
    public CommandResult execute(CommandContext context) {
        return CommandResult.single("""
            /help - список всех команд
            /grow - вырастить показатель, раз в 8 часов
            /jackpot - увеличить шанс джекпота и неудачи за процент от своего размера
            /slow - замедлить рост врага бесплатно
            /fuck - увеличить шанс неудачи для врага за процент от своего размера
            /turtle - увеличить себе рост бесплатно
            /pray - уменьшить себе шанс неудачи
            /transfer [число] - перевести часть роста другому игроку с комиссией (переводить можно только тем, у кого меньше)
            /top - увидеть топ игроков в своей беседе (в личных сообщениях - глобальный топ)
            /link - привязать свой тг/дс (рекомендуется использовать в лс с ботом)
            /style - изменить стиль роста (только в беседах)
            /profile - посмотреть профиль (свой или другого игрока)
            """);
    }
}
