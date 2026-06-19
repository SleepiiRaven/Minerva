package net.minervamc.minerva.skills.greek.hecate;

import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.hecate.TripleGoddess.Face;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Hecate RRR — the meta-decision. Cycles the current Face (Maiden -> Mother -> Crone) and emits a
 * small Face pulse on arrival: MAIDEN dashes forward, MOTHER wards the party, CRONE fears nearby
 * enemies. Three distinct bell pitches read out the new Face.
 */
public class ShiftFace extends Skill {
    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 3700;
            case 4, 5 -> 3400;
            default -> 4000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "shiftFace")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "shiftFace", cooldown);
        cooldownAlarm(player, cooldown, "Shift Face");

        TripleGoddess.cycle(player);
        Face face = TripleGoddess.getFace(player);
        final int fLevel = level;

        // smooth swirl that blends the two Face colours spatially (no strobe)
        Color a = swirlA(face);
        Color b = swirlB(face);
        Location center = player.getLocation().clone().add(0, 1.0, 0);
        int idx = 0;
        for (Vector v : ParticleUtils.getCirclePoints(1.2, 24)) {
            Color c = (idx % 2 == 0) ? a : b;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(c, 1.2f));
            idx++;
        }

        switch (face) {
            case MAIDEN -> {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.6f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.6f);
                Vector dash = player.getLocation().getDirection().normalize().multiply(level >= 4 ? 1.25 : 1.05);
                dash.setY(Math.max(0.35, dash.getY() + 0.32));
                player.setVelocity(dash);
                if (fLevel >= 5) {
                    // cleanse: strip negative effects from the caster
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                    player.removePotionEffect(PotionEffectType.WEAKNESS);
                    player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                    player.removePotionEffect(PotionEffectType.WITHER);
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                    player.removePotionEffect(PotionEffectType.NAUSEA);
                    wake(player);
                    unfear(player);
                }
            }
            case MOTHER -> {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.4f);
                List<Player> party = Party.partyList(player);
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, fLevel >= 5 ? 200 : 120, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                if (party != null) {
                    for (Player ally : party) {
                        if (ally == null || !ally.isOnline()) continue;
                        if (ally.getLocation().distanceSquared(player.getLocation()) > 36) continue;
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, fLevel >= 5 ? 200 : 120, 1));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                        if (fLevel >= 5) ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 0));
                    }
                }
            }
            case CRONE -> {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 0.7f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 0.7f, 0.7f);
                for (Entity e : player.getNearbyEntities(5, 5, 5)) {
                    if (!(e instanceof LivingEntity le) || e == player) continue;
                    if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                    if (PlayerStats.isSummoned(player, le)) continue;
                    fear(player, le, 15);
                    if (fLevel >= 5) le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
                }
            }
        }

        // L3+: leave a faint dual-Face aura for ~2s (steady, slow swirl)
        if (fLevel >= 3) {
            final Color auraA = a;
            final Color auraB = b;
            new BukkitRunnable() {
                int t = 0;
                double angle = 0;
                @Override
                public void run() {
                    if (t >= 40 || player.isDead() || !player.isOnline()) {
                        cancel();
                        return;
                    }
                    angle += 0.3;
                    Location c = player.getLocation().clone().add(0, 0.2 + (t / 40.0) * 1.6, 0);
                    for (int i = 0; i < 2; i++) {
                        double ang = angle + i * Math.PI;
                        Vector off = new Vector(Math.cos(ang) * 0.6, 0, Math.sin(ang) * 0.6);
                        c.getWorld().spawnParticle(Particle.DUST, c.clone().add(off), 0, 0, 0, 0, 0,
                                new Particle.DustOptions(i == 0 ? auraA : auraB, 0.9f));
                    }
                    t++;
                }
            }.runTaskTimer(Minerva.getInstance(), 2L, 2L);
        }
    }

    private static Color swirlA(Face face) {
        return switch (face) {
            case MAIDEN -> TripleGoddess.MAIDEN_SILVER;
            case MOTHER -> TripleGoddess.MOTHER_GREEN;
            case CRONE -> TripleGoddess.CRONE_VIOLET;
        };
    }

    private static Color swirlB(Face face) {
        return switch (face) {
            case MAIDEN -> Color.fromRGB(180, 195, 240);
            case MOTHER -> TripleGoddess.MOTHER_GOLD;
            case CRONE -> TripleGoddess.CRONE_BLACK;
        };
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Cycle Face: Maiden dashes, Mother wards, Crone fears.";
            case 2 -> ChatColor.GRAY + "Lower cooldown.";
            case 3 -> ChatColor.GRAY + "Leaves a faint dual-Face aura for 2s.";
            case 4 -> ChatColor.GRAY + "Longer Maiden dash and faster shifts.";
            case 5 -> ChatColor.GRAY + "Shift also cleanses (Maiden), shields (Mother), or weakens (Crone).";
            default -> ChatColor.GRAY + "Cycle to the next Face.";
        };
    }

    @Override
    public String toString() {
        return "shiftFace";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.AMETHYST_SHARD),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Shift Face]",
                ChatColor.GRAY + "Turn to the next " + ChatColor.LIGHT_PURPLE + "Face" + ChatColor.GRAY + ", releasing a pulse:",
                ChatColor.WHITE + "Maiden" + ChatColor.GRAY + " dash, " + ChatColor.GREEN + "Mother" + ChatColor.GRAY + " ward,",
                ChatColor.DARK_PURPLE + "Crone" + ChatColor.GRAY + " fear. Fluency between Faces is the game.");
    }
}
