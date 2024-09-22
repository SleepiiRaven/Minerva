package net.minervamc.minerva.lib.command;

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
    CommandUser user() default CommandUser.ALL;
    String invalidUserMessage() default "&cInvalid User, Command must be executed by a {}"; // replaces with appropriate user
    int cooldown() default 0;
    String cooldownMessage() default "&cYou cannot run this command for another {}"; // uses legacy and hex formatting, also replaces {} with time.
    boolean noArgs() default false;
    boolean allArgs() default false;
}