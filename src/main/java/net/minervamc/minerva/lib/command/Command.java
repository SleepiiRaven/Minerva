package net.minervamc.minerva.lib.command;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.CooldownManager;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// Proceed with caution!
public class Command extends org.bukkit.command.Command {

    private final HashMap<String, Method> cachedCommandMethods;
    private final HashMap<String, Method> cachedTabMethods;

    public Command(@NotNull String name) {
        this(name,"","", List.of());

    }
    public Command(@NotNull String name, @NotNull String description, @NotNull String usageMessage, @NotNull List<String> aliases) {
        super(name, description, usageMessage, aliases);
        cachedCommandMethods = new HashMap<>();
        cachedTabMethods = new HashMap<>();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        CommandContext context = new CommandContext(sender, this, args);
        executeAllArgs(context);

        if(args.length == 0) {
            executeNoArgs(context);
            return true;
        }

        String name = args[0].toLowerCase();
        Method method = this.cachedCommandMethods.get(name);
        if (method != null) {
            ICommand annotation = method.getAnnotation(ICommand.class);
            String permission = annotation.permission();
            String permissionMessage = annotation.permissionMessage();
            CommandUser user = annotation.user();
            String invalidUserMessage = annotation.invalidUserMessage();
            String cooldownMessage = annotation.cooldownMessage();
            int cooldown = annotation.cooldown();

            if (!isValidUser(context.sender(), user)) {
                context.sender().sendMessage(TextContext.hexAndLegacy(formatInvalidUserMessage(invalidUserMessage, user)));
                return true;
            }

            if (!permission.isEmpty() && !context.sender().hasPermission(permission)) {
                context.sender().sendMessage(TextContext.hexAndLegacy(permissionMessage));
                return true;
            }

            if (!CooldownManager.isCooldownExpired(context.sender().getName(), name)) {
                context.sender().sendMessage(TextContext.hexAndLegacy(cooldownMessage));
                return true;
            }

            CooldownManager.setCooldown(context.sender().getName(), name, cooldown);
            try {
                method.invoke(this, context);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    private void executeAllArgs(CommandContext context) {
        Method method = cachedCommandMethods.get("allArgs");
        if (method != null) {
            ICommand annotation = method.getAnnotation(ICommand.class);
            String permission = annotation.permission();
            String permissionMessage = annotation.permissionMessage();
            String cooldownMessage = annotation.cooldownMessage();
            CommandUser user = annotation.user();
            String invalidUserMessage = annotation.invalidUserMessage();
            int cooldown = annotation.cooldown();

            if (!isValidUser(context.sender(), user)) {
                context.sender().sendMessage(TextContext.hexAndLegacy(formatInvalidUserMessage(invalidUserMessage, user)));
                return;
            }

            if (!permission.isEmpty() && !context.sender().hasPermission(permission)) {
                context.sender().sendMessage(TextContext.hexAndLegacy(permissionMessage));
                return;
            }

            if (!CooldownManager.isCooldownExpired(context.sender().getName(), "allArgs")) {
                context.sender().sendMessage(TextContext.hexAndLegacy(cooldownMessage));
                return;
            }

            CooldownManager.setCooldown(context.sender().getName(), "allArgs", cooldown);
            try {
                method.invoke(this, new CommandContext(context.sender(), this, context.args()));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void executeNoArgs(CommandContext context) {
        Method method = cachedCommandMethods.get("noArgs");
        if (method != null) {
            ICommand annotation = method.getAnnotation(ICommand.class);
            String permission = annotation.permission();
            String permissionMessage = annotation.permissionMessage();
            String cooldownMessage = annotation.cooldownMessage();
            CommandUser user = annotation.user();
            String invalidUserMessage = annotation.invalidUserMessage();
            int cooldown = annotation.cooldown();

            if (!isValidUser(context.sender(), user)) {
                context.sender().sendMessage(TextContext.hexAndLegacy(formatInvalidUserMessage(invalidUserMessage, user)));
                return;
            }

            if (!permission.isEmpty() && !context.sender().hasPermission(permission)) {
                context.sender().sendMessage(TextContext.hexAndLegacy(permissionMessage));
                return;
            }

            if (!CooldownManager.isCooldownExpired(context.sender().getName(), "noArgs")) {
                context.sender().sendMessage(TextContext.hexAndLegacy(cooldownMessage));
                return;
            }

            CooldownManager.setCooldown(context.sender().getName(), "noArgs", cooldown);
            try {
                method.invoke(this, context);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isValidUser(CommandSender sender, CommandUser user) {
        return switch (user) {
            case PLAYER -> sender instanceof Player;
            case CONSOLE -> sender instanceof ConsoleCommandSender;
            case COMMAND_BLOCK -> sender instanceof CommandBlock;
            case ALL -> true;
        };
    }

    private String formatInvalidUserMessage(String message, CommandUser user) {
        String userType = switch (user) {
            case PLAYER -> "player";
            case CONSOLE -> "console";
            case COMMAND_BLOCK -> "command block";
            case ALL -> "sender";
        };
        return message.replace("{}", userType);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        CommandContext context = new CommandContext(sender, this, args);

        List<String> allArgsCompletions = new ArrayList<>();
        if(cachedCommandMethods.get("allArgs") != null) {
            Method commandMethod = cachedCommandMethods.get("allArgs");
            Method method = null;
            if(commandMethod != null) {
                ICommand annotation = commandMethod.getAnnotation(ICommand.class);
                if (annotation != null) {
                    if(!annotation.tabCompleter().isEmpty() || !annotation.tabCompleter().isBlank()) {
                        String tabCompleter = annotation.tabCompleter().toLowerCase();

                        Method tabMethod = this.cachedTabMethods.get(tabCompleter);
                        if(tabMethod != null) {
                            method = tabMethod;
                        }
                    }else {
                        Method tabMethod = this.cachedTabMethods.get("allArgs");
                        if(tabMethod != null) {
                            method = tabMethod;
                        }
                    }
                    if(method != null) {
                        if (!method.getReturnType().equals(List.class) || !method.getGenericReturnType().getTypeName().equals("java.util.List<java.lang.String>")) return List.of();
                        try {
                            @SuppressWarnings("unchecked")
                            List<String> completions = (List<String>) method.invoke(this, context); // INVOKED HERE!
                            allArgsCompletions.addAll(completions);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        if(args.length == 1) {
            List<String> completions = new ArrayList<>(cachedCommandMethods.keySet());
            completions.addAll(allArgsCompletions);
            return completions.stream()
                    .filter(name -> !name.equals("noArgs"))
                    .filter(name -> !name.equals("allArgs"))
                    .filter(name -> {
                        Method method = this.cachedCommandMethods.get(name);
                        if(method == null) {
                            method = this.cachedCommandMethods.get("allArgs");
                            if(method == null) return true;
                        }
                        ICommand annotation = method.getAnnotation(ICommand.class);
                        if (annotation == null) return true;

                        String permission = annotation.permission();
                        if(permission.isEmpty() || permission.isBlank()) return true;
                        return sender.hasPermission(permission);
                    })
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }


        String subcommand = args[0].toLowerCase();
        Method method;

        Method commandMethod = this.cachedCommandMethods.get(subcommand);
        if(commandMethod == null) return List.of();

        ICommand annotation = commandMethod.getAnnotation(ICommand.class);
        if (annotation == null) return List.of();

        if(!annotation.tabCompleter().isEmpty() || !annotation.tabCompleter().isBlank()) {
            String tabCompleter = annotation.tabCompleter().toLowerCase();

            Method tabMethod = this.cachedTabMethods.get(tabCompleter);
            if(tabMethod == null) return List.of();

            method = tabMethod;
        }else {
            Method tabMethod = this.cachedTabMethods.get(subcommand);
            if(tabMethod == null) return List.of();
            method = tabMethod;
        }

        if (!method.getReturnType().equals(List.class) || !method.getGenericReturnType().getTypeName().equals("java.util.List<java.lang.String>")) return List.of();
        try {
            @SuppressWarnings("unchecked")
            List<String> completions = (List<String>) method.invoke(this, context); // INVOKED HERE!
            List<String> mutableCompletions = new ArrayList<>(completions);
            mutableCompletions.addAll(allArgsCompletions);
            return mutableCompletions;
        } catch (IllegalAccessException | InvocationTargetException |
                 CommandException | IllegalArgumentException ignored) {}
        return List.of();
    }

    private void cacheMethods() {
        Method[] methods = this.getClass().getMethods();

        for (Method method  : methods) {
            method.setAccessible(true);
            if(method.getName().equals("allArgs") || method.getName().equals("noArgs")) {
                Minerva.getInstance().getSLF4JLogger().warn("Avoid naming methods with either \"allArgs\" or \"noArgs\", Skipping registration...");
                continue;
            }

            ICommand commandAnnotation = method.getAnnotation(ICommand.class);
            ITabComplete tabAnnotation = method.getAnnotation(ITabComplete.class);

            boolean hasExactlyOneAnnotation = (commandAnnotation != null) ^ (tabAnnotation != null);
            if (hasExactlyOneAnnotation) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1 || !parameterTypes[0].equals(CommandContext.class)) {
                    Minerva.getInstance().getSLF4JLogger().warn("Method {} must have a single parameter of type CommandContext, Skipping registration...", method.getName());
                    continue;
                }
            }

            if (commandAnnotation != null) {
                if(method.getReturnType() != void.class) {
                    Minerva.getInstance().getSLF4JLogger().warn("All command methods must return void, Skipping registration...");
                    continue;
                }
                if(commandAnnotation.allArgs()) {
                    cachedCommandMethods.put("allArgs", method);
                    continue;
                }
                if(commandAnnotation.noArgs()) {
                    cachedCommandMethods.put("noArgs", method);
                    continue;
                }
                String name = (!commandAnnotation.name().isBlank()) ? commandAnnotation.name() : method.getName();
                if(cachedCommandMethods.containsKey(name)) {
                    Minerva.getInstance().getSLF4JLogger().warn("Attempted to register a command but it already exists with name: {}", name);
                    continue;
                }

                cachedCommandMethods.put(name, method);
                continue;
            }

            if (tabAnnotation != null) {
                if (!List.class.isAssignableFrom(method.getReturnType())) {
                    Minerva.getInstance().getSLF4JLogger().warn("All tabCompleter methods must return a List, Skipping registration...");
                    continue;
                }
                String name = tabAnnotation.name();
                if(cachedTabMethods.containsKey(name)) {
                    Minerva.getInstance().getSLF4JLogger().warn("Attempted to register a tabCompleter but it already exists with name: {}", name);
                    continue;
                }

                cachedTabMethods.put(name, method);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void register(JavaPlugin plugin, Command... cmds) {
        for (Command cmd : cmds) {
            plugin.getServer().getCommandMap().register("", cmd);
            cmd.cacheMethods();
        }
    }
}
