package net.minervamc.minerva.quest.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minervamc.minerva.Minerva;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Minimal chat-capture utility — the project has no anvil/text-input helper. Closes the menu, waits for the
 * admin's next chat message, cancels it, and runs the callback on the main thread. Registered once in
 * {@code Lib.onEnable()}.
 */
public class ChatInput implements Listener {
    private static final Map<UUID, Consumer<String>> waiting = new HashMap<>();

    public static void await(Player player, String prompt, Consumer<String> onInput) {
        player.closeInventory();
        player.sendMessage(Component.text(prompt, NamedTextColor.AQUA));
        player.sendMessage(Component.text("Type your answer in chat, or 'cancel' to cancel.", NamedTextColor.GRAY));
        waiting.put(player.getUniqueId(), onInput);
    }

    public static boolean isAwaiting(Player player) {
        return waiting.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = waiting.remove(player.getUniqueId());
        if (callback == null) return;
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("Cancelled.", NamedTextColor.GRAY));
            return;
        }
        Bukkit.getScheduler().runTask(Minerva.getInstance(), () -> callback.accept(message));
    }
}
