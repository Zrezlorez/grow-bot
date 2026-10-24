package com.litovskiy.exception;

public class CommandNotFoundException extends RuntimeException {
    public CommandNotFoundException(String command) {
        super("Команда " + command + " не найдена");
    }
}
