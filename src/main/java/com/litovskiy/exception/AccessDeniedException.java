package com.litovskiy.exception;

import com.litovskiy.entity.Platform;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(long id, Platform platform, String command) {
        super(String.format("Пользователь %s:%s попытался использовать %s", id, platform, command));
    }
}
