package com.litovskiy.service.command;

import com.litovskiy.entity.Platform;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BotCommand {

    String name();

    String[] aliases() default {};

    String description() default "";

    Platform[] platforms() default {
        Platform.TELEGRAM,
        Platform.DISCORD
    };

    Access access() default Access.PUBLIC;

    CommandArgument[] arguments() default {};

    enum Access {
        PUBLIC,
        ADMIN
    }

    enum ArgumentType {
        STRING,
        NUMBER,
        USER
    }

    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface CommandArgument {

        String name();

        boolean required() default false;

        ArgumentType type() default ArgumentType.STRING;

        String description() default "";
    }
}
