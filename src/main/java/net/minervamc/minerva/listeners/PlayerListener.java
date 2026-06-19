package net.minervamc.minerva.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.Gravity;
import net.citizensnpcs.trait.SkinTrait;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.guis.AncestryGUI;
import net.minervamc.minerva.guis.GreekGodsGUI;
import net.minervamc.minerva.guis.MythicalCreaturesGUI;
import net.minervamc.minerva.guis.RomanGodsGUI;
import net.minervamc.minerva.guis.SkillsGUI;
import net.minervamc.minerva.guis.TitansGUI;
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PlayerListener implements Listener {
    private final Minerva plugin = Minerva.getInstance();
    CooldownManager cooldownManager = plugin.getCdInstance();

    // public static Map<Player, NPC> npcs = new HashMap<>();

    /** A non-persistent, in-memory NPC registry for short-lived NPCs. Never written to saves.yml, so these
     * NPCs can never be resurrected after a restart — even if the server is killed before they despawn. */
    private static final String TRANSIENT_REGISTRY = "minerva-transient";

    private static NPCRegistry transientRegistry() {
        NPCRegistry registry = CitizensAPI.getNamedNPCRegistry(TRANSIENT_REGISTRY);
        if (registry == null) {
            registry = CitizensAPI.createNamedNPCRegistry(TRANSIENT_REGISTRY, new MemoryNPCDataStore());
        }
        return registry;
    }

    @EventHandler
    public void prePlayerLogIn(PlayerPreLoginEvent e) {
        // Warm the player's skin by briefly spawning a Citizens NPC with it. PlayerPreLoginEvent can fire off
        // the main thread, so hop onto it before touching Citizens.
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) return;
        final String name = e.getName();
        Bukkit.getScheduler().runTask(Minerva.getInstance(), () -> warmSkin(name));
    }

    private void warmSkin(String name) {
        NPCRegistry registry = transientRegistry();
        NPC npc = registry.createNPC(EntityType.PLAYER, name + " - Loading In");
        npc.getOrAddTrait(SkinTrait.class).setSkinName(name);
        npc.getOrAddTrait(SkinTrait.class).setShouldUpdateSkins(true);
        npc.getOrAddTrait(Gravity.class).setHasGravity(true);

        npc.spawn(Bukkit.getWorlds().getFirst().getSpawnLocation());
        npc.setProtected(false);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= 20) {
                    if (npc.isSpawned()) npc.despawn();
                    registry.deregister(npc);
                    this.cancel();
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    /** Remove any "&lt;player&gt; - Loading In" NPCs that leaked into the persistent registry from older builds
     * (before these were made non-persistent) and got resurrected on restart. Call once on enable. */
    public static int sweepLeakedLoadingNpcs() {
        // Copy first: destroy() deregisters, which would mutate the registry mid-iteration.
        ArrayList<NPC> all = new ArrayList<>();
        for (NPC npc : CitizensAPI.getNPCRegistry()) all.add(npc);

        int removed = 0;
        for (NPC npc : all) {
            String npcName = npc.getName();
            if (npcName != null && ChatColor.stripColor(npcName).endsWith(" - Loading In")) {
                npc.destroy();
                removed++;
            }
        }
        return removed;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        cooldownManager.createContainer(p.getUniqueId());
        PlayerStats pData = PlayerStats.getStats(p.getUniqueId());
        if (pData.getLogoutLoc() == null) {
            pData.setLogoutLoc(p.getLocation());
        } else {
            p.teleport(pData.getLogoutLoc());
        }

        if (!Arrays.stream(pData.getInventory()).allMatch(Objects::isNull)) {
            p.getInventory().setStorageContents(pData.getInventory());
        }
        if (!Arrays.stream(pData.getArmor()).allMatch(Objects::isNull)) {
            p.getInventory().setArmorContents(pData.getArmor());
        }
        if (pData.getOffhand()[0] != null) {
            p.getInventory().setItemInOffHand(pData.getOffhand()[0]);
        }

        pData.setInventory(new ItemStack[36]);
        pData.setArmor(new ItemStack[4]);
        pData.setOffhand(new ItemStack[1]);
        pData.save();

//        if (!pData.getOmegaTrail().isEmpty() && p.hasPermission("minerva.omegatrail")) {
//            new BukkitRunnable() {
//                @Override
//                public void run() {
//                    if (!p.isOnline()) this.cancel();
//
//                    String[] strings = pData.getOmegaTrail().split(",");
//                    for (int stringIndex = 0; stringIndex < strings.length; stringIndex++) {
//                        if (strings[stringIndex].equals("flame")) {
//                            p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 10, 0, 0, 0, 0);
//                        }
//                        if (strings[stringIndex].equals("happyVillager")) {
//                            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation(), 10, 0, 0, 0, 0);
//                        }
//                        if (strings[stringIndex].equals("heart")) {
//                            p.getWorld().spawnParticle(Particle.HEART, p.getLocation(), 10, 0, 0, 0, 0);
//                        }
//                        if (strings[stringIndex].equals("rainbow")) {
//                            List<Vector> particles = ParticleUtils.getVerticalCirclePoints(1, p.getPitch(), p.getYaw(), 14);
//                            Color[] colors = {
//                                    ParticleUtils.colorFromHex("#ffc6ff"),
//                                    ParticleUtils.colorFromHex("#bdb2ff"),
//                                    ParticleUtils.colorFromHex("#a0c4ff"),
//                                    ParticleUtils.colorFromHex("#9bf6ff"),
//                                    ParticleUtils.colorFromHex("#caffbf"),
//                                    ParticleUtils.colorFromHex("#fdffb6"),
//                                    ParticleUtils.colorFromHex("#ffd6a5"),
//                                    ParticleUtils.colorFromHex("#ffadad")
//                            };
//                            for (int i = 0; i < 8; i++) {
//                                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 2, 0).add(particles.get(i)), 2, 0, 0, 0, 0, new Particle.DustOptions(colors[i], 2f));
//                            }
//                        }
//                        if (strings[stringIndex].equals("wings")) {
//                            double Ax = 0;
//                            double Ay = 0.335;
//                            double Bx = 1.8;
//                            double By = 1.9;
//                            double Cx = 0.9;
//                            double Cy = 0;
//                            double Dx = 0;
//                            double Dy = 2;
//                            double Ex = 0;
//                            double Ey = 0.7;
//
//                            Vector init = p.getLocation().getDirection().clone().setY(0).normalize().multiply(-0.4).setY(0.25);
//                            Vector right = ParticleUtils.rotateYAxis(p.getLocation().getDirection().clone().setY(0).normalize(), 90);
//                            Vector A1 = add(init.clone(), right.clone(), Ax, Ay);
//                            Vector A2 = add(init.clone(), right.clone(), -Ax, Ay);
//                            Vector B1 = add(init.clone(), right.clone(), Bx, By);
//                            Vector B2 = add(init.clone(), right.clone(), -Bx, By);
//                            Vector C1 = add(init.clone(), right.clone(), Cx, Cy);
//                            Vector C2 = add(init.clone(), right.clone(), -Cx, Cy);
//                            Vector D1 = add(init.clone(), right.clone(), Dx, Dy);
//                            Vector D2 = add(init.clone(), right.clone(), -Dx, Dy);
//                            Vector E1 = add(init.clone(), right.clone(), Ex, Ey);
//                            Vector E2 = add(init.clone(), right.clone(), -Ex, Ey);
//                            List<Vector> particles = ParticleUtils.getNthBezierPoints(20, A1, B1, C1, D1, E1);
//
//                            Color[] colors = {
//                                    ParticleUtils.colorFromHex("012a36"),
//                                    ParticleUtils.colorFromHex("29274c"),
//                                    ParticleUtils.colorFromHex("7e52a0"),
//                                    ParticleUtils.colorFromHex("d295bf"),
//                                    ParticleUtils.colorFromHex("e6bccd"),
//                                    ParticleUtils.colorFromHex("e4d9ff")
//                            };
//
//                            particles.addAll(ParticleUtils.getNthBezierPoints(20, A2, B2, C2, D2, E2));
//                            for (Vector vec : particles) {
//                                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().clone().add(vec), 2, 0, 0, 0, 0, ParticleUtils.getDustOptionsFromGradient(colors, 1f));
//                            }
//                        }
//                    }
//                }
//            }.runTaskTimer(Minerva.getInstance(), 0L, 5L);
//        }

    }

    private Vector add(Vector init, Vector right, double x, double y) {
        return init.clone().add(right.clone().multiply(x)).add(new Vector(0, y, 0));
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (p.hasMetadata("NPC")) return;

        Minerva.runPermRemoveCommand(p, "venturechat.blueteamchat");
        Minerva.runPermRemoveCommand(p, "venturechat.redteamchat");
        PlayerStats pData = PlayerStats.getStats(p.getUniqueId());
        if (CaptureTheFlag.isPlaying() && CaptureTheFlag.isInGame(p)) {
            pData.setLogoutLoc(CaptureTheFlag.getStartLoc().get(p.getUniqueId()));
        } else {
            pData.setLogoutLoc(p.getLocation());
        }
        pData.save();
        cooldownManager.removeContainer(p.getUniqueId());
    }

    @EventHandler
    public void playerLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("NPC")) return;

        PlayerStats playerStats = PlayerStats.getStats(player.getUniqueId());

        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        // While casting (spell mode entered via Q), left-click = "L" combo input.
        if (playerStats.skillMode && playerStats.skillTriggers.spellMode) {
            if (playerStats.skillTriggers.consumeEntrySwing()) return; // swallow the Q-drop's own arm-swing
            if (!cooldownManager.isCooldownDone(player.getUniqueId(), "Spell Click")) return;
            cooldownManager.setCooldownFromNow(player.getUniqueId(), "Spell Click", 50L);
            playerStats.skillTriggers.click(false); // left = L
        }
    }

    @EventHandler
    public void playerRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) return;
        PlayerStats playerStats = PlayerStats.getStats(player.getUniqueId());
        Action action = event.getAction();
        if (!(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) return;
        if (event.getHand() != EquipmentSlot.HAND) return; // ignore off-hand duplicate fire

        // While casting, right-click = "R" combo input (and suppress item use, e.g. bow draw).
        if (playerStats.skillMode && playerStats.skillTriggers.spellMode) {
            if (!cooldownManager.isCooldownDone(player.getUniqueId(), "Spell Click")) return;
            cooldownManager.setCooldownFromNow(player.getUniqueId(), "Spell Click", 50L);
            event.setCancelled(true);
            playerStats.skillTriggers.click(true); // right = R
        }
    }

    @EventHandler
    public void onDropKey(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) return;
        PlayerStats playerStats = PlayerStats.getStats(player.getUniqueId());
        if (!playerStats.skillMode) return; // skill system off → normal drops allowed

        event.setCancelled(true); // Q is the cast key — never drop items while skill mode is on

        if (playerStats.skillTriggers.spellMode) {
            playerStats.skillTriggers.cancelSpellMode(); // Q again → exit
        } else {
            playerStats.skillTriggers.enterSpellMode(player); // Q → enter spell mode
        }
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        switch (ChatColor.stripColor(event.getView().getTitle())) {
            case AncestryGUI.invName -> AncestryGUI.clickedGUI(event);
            case GreekGodsGUI.invName -> GreekGodsGUI.clickedGUI(event);
            case RomanGodsGUI.invName -> RomanGodsGUI.clickedGUI(event);
            case TitansGUI.invName -> TitansGUI.clickedGUI(event);
            case MythicalCreaturesGUI.invName -> MythicalCreaturesGUI.clickedGUI(event);
        }
        if (ChatColor.stripColor(event.getView().getTitle()).contains(SkillsGUI.invName)) {
            SkillsGUI.clickedGUI(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void playerDamagePlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() == event.getEntity()) return;
        if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Player player) {
            if (Party.isPlayerInPlayerParty(damager, player)) event.setCancelled(true);
        }
    }
}
