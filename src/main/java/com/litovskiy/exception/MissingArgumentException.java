package com.litovskiy.exception;

import lombok.Getter;

@Getter
public class MissingArgumentException extends RuntimeException {

    private final String argumentName;

    public MissingArgumentException(String argumentName) {
        super("Нет обязательного аргумента: " + argumentName);
        this.argumentName = argumentName;
    }
}
