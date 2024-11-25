package net.minervamc.minerva.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class CombatListener implements Listener {
    static Map<Player, Map<Entity, Long>> playersInCombat = new HashMap<>();
    private final Long COMBAT_TIME = 10000L;

    public static boolean isInCombatOrHostile(Player player, Entity inCombatWith) {
        if ((inCombatWith instanceof Monster monster && monster.getTarget() == player) || (playersInCombat.get(player) != null && playersInCombat.get(player).containsKey(inCombatWith))) {
            return true;
        } else if (Party.partyList(player) != null) {
            for (Player pMember : Party.partyList(player)) {
                if ((inCombatWith instanceof Monster monster && monster.getTarget() == pMember) || (playersInCombat.get(pMember) != null && playersInCombat.get(pMember).containsKey(inCombatWith))) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isInCombat(Player player, Entity inCombatWith) {
        if (playersInCombat.get(player) != null && playersInCombat.get(player).containsKey(inCombatWith)) {
            return true;
        } else if (Party.partyList(player) != null) {
            for (Player pMember : Party.partyList(player)) {
                if (playersInCombat.get(pMember) != null && playersInCombat.get(pMember).containsKey(inCombatWith)) {
                    return true;
                }
            }
        }

        return false;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() == event.getDamager() || (event.getDamager() instanceof Projectile projectile && projectile.getShooter() == event.getEntity())) return;

        if (event.getEntity() instanceof Player player) {
            enterCombat(player, event.getDamager());
        }

        if (event.getDamager() instanceof Player player) {
            enterCombat(player, event.getEntity());
        }

        if (PlayerStats.whoSummonedMe(event.getDamager()) != null) {
            enterCombat(PlayerStats.whoSummonedMe(event.getDamager()), event.getEntity());
        }
    }

    public void enterCombat(Player player, Entity other) {
        if (!playersInCombat.containsKey(player)) {
            playersInCombat.put(player, new HashMap<>());

            Map<Entity, Long> entities = playersInCombat.get(player);

            new BukkitRunnable() {
                @Override
                public void run() {
                    List<Entity> expiredEntities = new ArrayList<>();

                    for (Entity combatEntity : entities.keySet()) {
                        if (entities.get(combatEntity) < System.currentTimeMillis()) {
                            expiredEntities.add(combatEntity);
                        }
                    }

                    for (Entity expiredEntity : expiredEntities) {
                        entities.remove(expiredEntity);
                    }

                    if (entities.isEmpty()) {
                        player.sendActionBar(Component.text("You are no longer in combat!", TextColor.color(220, 20, 60)));
                        playersInCombat.remove(player);
                        this.cancel();
                    }
                }
            }.runTaskTimer(Minerva.getInstance(), 0L, COMBAT_TIME/50);
        }

        Map<Entity, Long> entities = playersInCombat.get(player);

        if (entities.containsKey(other)) {
            entities.remove(other);
        } else if (entities.keySet().isEmpty()) {
            player.sendActionBar(Component.text("You are now in combat!", TextColor.color(220, 20, 60)));
        }

        entities.put(other, System.currentTimeMillis() + COMBAT_TIME);
    }

    @EventHandler
    public void killEvent(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            playersInCombat.remove(player);
        }

        for (Player player : playersInCombat.keySet()) {
            playersInCombat.get(player).remove(event.getEntity());
            if (playersInCombat.get(player).keySet().isEmpty()) {
                player.sendActionBar(Component.text("You are no longer in combat!", TextColor.color(220, 20, 60)));
            }
        }
    }
}
