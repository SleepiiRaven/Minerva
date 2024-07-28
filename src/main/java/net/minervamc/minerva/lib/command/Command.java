package net.minervamc.minerva.lib.command;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.lib.text.TextContext;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// Proceed with caution!
// Do not try to understand this please...
public class Command extends org.bukkit.command.Command {

    private final HashMap<String, Method> cachedCommandMethods;
    private final HashMap<String, Method> cachedTabMethods;

    public Command(@NotNull String name) {
        this(name,"_","_", List.of());

    }
    public Command(@NotNull String name, @NotNull String description, @NotNull String usageMessage, @NotNull List<String> aliases) {
        super(name, description, usageMessage, aliases);
        cachedCommandMethods = new HashMap<>();
        cachedTabMethods = new HashMap<>();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        CommandContext context = new CommandContext(sender, this, args);
        {
            Method method = this.cachedCommandMethods.get("allArgs");
            if(method != null) {
                if (method.getParameterCount() == 1) {
                    ICommand annotation = method.getAnnotation(ICommand.class);
                    String permission = null;
                    if (annotation != null) {
                        if (!annotation.permission().isEmpty() || !annotation.permission().isBlank())
                            permission = annotation.permission();
                    }

                    if (permission != null) {
                        if (sender.hasPermission(permission)) {
                            try {
                                method.invoke(this, context);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        }else {
                            if(!annotation.permissionMessage().isEmpty() || annotation.permissionMessage().isBlank()) {
                                sender.sendMessage(TextContext.hexAndLegacy(annotation.permissionMessage()));
                            }
                        }
                    }else {
                        try {
                            method.invoke(this, context);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }
            }
        }

        if(args.length == 0) {
            Method method = this.cachedCommandMethods.get("noArgs");
            if(method == null) return true;
            if(method.getParameterCount() != 1) return true;
            ICommand annotation = method.getAnnotation(ICommand.class);
            String permission = null;
            if(annotation != null) {
                if(!annotation.permission().isEmpty() || !annotation.permission().isBlank()) permission = annotation.permission();
            }

            if(permission != null) {
                if(!sender.hasPermission(permission)) {
                    if(!annotation.permissionMessage().isEmpty() || annotation.permissionMessage().isBlank()) {
                        sender.sendMessage(TextContext.hexAndLegacy(annotation.permissionMessage()));
                    }
                    return true;
                }
            }
            try {
                method.invoke(this, context);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        String name = args[0].toLowerCase();
        Method method = this.cachedCommandMethods.get(name);
        if(method == null) return true;

        if(method.getParameterCount() != 1) return true;
        ICommand annotation = method.getAnnotation(ICommand.class);
        String permission = null;
        if(annotation != null) {
            if(!annotation.permission().isEmpty() || !annotation.permission().isBlank()) permission = annotation.permission();
        }

        if(permission != null) {
            if(!sender.hasPermission(permission)) return true;
        }
        try {
            method.invoke(this, context); // INVOKED HERE!
        } catch (IllegalAccessException | InvocationTargetException |
                 CommandException | IllegalArgumentException ignored) {}
        return true;
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
        Method[] methods = this.getClass().getDeclaredMethods();
        for(Method method : methods) {
            if(method.isAnnotationPresent(ICommand.class)) {
                method.setAccessible(true);
                ICommand annotation = method.getAnnotation(ICommand.class);
                if(annotation == null) continue;

                if (cachedCommandMethods.containsKey(annotation.name())) {
                    Minerva.getLog().info("Duplicate Command was attempted to be registered.");
                    continue;
                }
                if (annotation.noArgs()) {
                    cachedCommandMethods.put("noArgs", method);
                    continue;
                }
                if (annotation.allArgs()) {
                    cachedCommandMethods.put("allArgs", method);
                    continue;
                }
                if(annotation.name() == null || annotation.name().isEmpty() || annotation.name().isBlank()) {
                    if(cachedCommandMethods.containsKey(method.getName())) {
                        Minerva.getLog().info("Duplicate Command was attempted to be registered.");
                        continue;
                    }
                    cachedCommandMethods.put(method.getName().toLowerCase(), method);
                    continue;
                }
                cachedCommandMethods.put(annotation.name().toLowerCase(), method);
            }
            if(method.isAnnotationPresent(ITabComplete.class)) {
                method.setAccessible(true);
                ITabComplete annotation = method.getAnnotation(ITabComplete.class);
                if(annotation == null) continue;

                if(cachedTabMethods.containsKey(annotation.name())) {
                    Minerva.getLog().info("Duplicate TabCompleter was attempted to be registered.");
                    continue;
                }
                cachedTabMethods.put(annotation.name().toLowerCase(), method);
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
