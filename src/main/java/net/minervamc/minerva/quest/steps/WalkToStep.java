package net.minervamc.minerva.quest.steps;

import com.google.gson.JsonObject;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minervamc.minerva.quest.Quest;
import net.minervamc.minerva.quest.gui.ChatInput;
import net.minervamc.minerva.quest.gui.SimpleMenu;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.utils.PlayerStatsAdapter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/** Completes when the player gets within {@code radius} blocks of a target location. */
public class WalkToStep extends QuestStep {
    private Location target;
    private double radius = 3.0;
    private transient BukkitTask task;

    @Override
    public StepType getType() {
        return StepType.WALK_TO;
    }

    @Override
    public ItemStack getIcon() {
        return ItemCreator.create(Component.text("Walk to Location"), Material.LEATHER_BOOTS);
    }

    @Override
    public String getEditorTitle() {
        if (target == null) return "Walk to (unset)";
        return "Walk to " + target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ();
    }

    @Override
    public List<String> getEditorLore() {
        return List.of("Radius: " + radius + " blocks");
    }

    @Override
    public void writeJson(JsonObject o) {
        if (target != null) o.addProperty("target", PlayerStatsAdapter.locationToString(target));
        o.addProperty("radius", radius);
    }

    @Override
    public void readJson(JsonObject o) {
        if (o.has("target") && !o.get("target").isJsonNull()) {
            target = PlayerStatsAdapter.stringToLocation(o.get("target").getAsString());
        }
        if (o.has("radius")) radius = o.get("radius").getAsDouble();
    }

    @Override
    public void begin(StepContext ctx) {
        if (target == null || target.getWorld() == null) {
            // Misconfigured step: don't trap the player.
            ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), ctx::complete);
            return;
        }
        Player player = ctx.player();
        double radiusSq = radius * radius;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                Location loc = player.getLocation();
                if (loc.getWorld() != null && loc.getWorld().equals(target.getWorld())
                        && loc.distanceSquared(target) <= radiusSq) {
                    cancel();
                    task = null;
                    ctx.complete();
                }
            }
        }.runTaskTimer(ctx.plugin(), 5L, 5L);
    }

    @Override
    public void cleanup(StepContext ctx) {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void openEditor(Player admin, Quest quest, Runnable back) {
        SimpleMenu menu = new SimpleMenu(27, Component.text("Edit: Walk to Location"));
        menu.fillBorder();
        menu.setItem(11, ItemCreator.create(Component.text("Set target to my position"), Material.COMPASS),
                (p, e) -> {
                    target = p.getLocation().clone();
                    net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    openEditor(p, quest, back);
                });
        menu.setItem(15, ItemCreator.create(Component.text("Set radius (" + radius + ")"), Material.TARGET),
                (p, e) -> ChatInput.await(p, "Enter the trigger radius in blocks (e.g. 3):", input -> {
                    try {
                        radius = Math.max(0.5, Double.parseDouble(input.trim()));
                        net.minervamc.minerva.quest.storage.QuestStorage.save(quest);
                    } catch (NumberFormatException ex) {
                        p.sendMessage(Component.text("Not a valid number."));
                    }
                    openEditor(p, quest, back);
                }));
        menu.setBackButton(back);
        menu.open(admin);
    }
}
