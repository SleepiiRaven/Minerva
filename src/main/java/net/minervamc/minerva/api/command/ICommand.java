package net.minervamc.minerva.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@SuppressWarnings("unused")
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ICommand {
    String name() default "";
    String permission() default "";
    String permissionMessage() default ""; // uses legacy and hex formatting
    String tabCompleter() default "";
    int cooldown() default 1; // not implemented
    boolean noArgs() default false;
    boolean allArgs() default false;
}