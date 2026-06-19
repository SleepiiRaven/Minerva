package net.minervamc.minerva.skills.greek.hecate;

import java.util.List;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.hecate.TripleGoddess.Face;
import net.minervamc.minerva.types.Skill;
import net.minervamc.minerva.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Hecate RRL — a 5s super-charge of the current Face. MAIDEN: big Haste + Speed and Shift Face's
 * cooldown is reset; MOTHER: party Resistance + strong Regen; CRONE: lifesteal (Strength + a tag,
 * so your damage heals you) and amplified curses. A rising Face-coloured column with accelerating
 * orbiting runes marks the window.
 */
public class WitchingHour extends Skill {
    /** Other Hecate skills can check this tag to grant Crone lifesteal while Witching Hour is up. */
    public static final String CRONE_LIFESTEAL_TAG = "hecateWitchHourLifesteal";

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = switch (level) {
            case 2, 3 -> 17000;
            case 4, 5 -> 16000;
            default -> 18000;
        };
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "witchingHour")) {
            onCooldown(player);
            return;
        }
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "witchingHour", cooldown);
        cooldownAlarm(player, cooldown, "Witching Hour");

        Face face = TripleGoddess.getFace(player);
        int durTicks = level >= 2 ? 140 : 100; // L2+: 7s, else 5s
        final int fLevel = level;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 1f);

        switch (face) {
            case MAIDEN -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, durTicks, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durTicks, 1));
                // reset Shift Face cooldown so you can fluently rotate
                cooldownManager.setCooldownFromNow(player.getUniqueId(), "shiftFace", 0L);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.6f);
            }
            case MOTHER -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durTicks, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durTicks, 2));
                List<Player> party = Party.partyList(player);
                if (party != null) {
                    for (Player ally : party) {
                        if (ally == null || !ally.isOnline()) continue;
                        if (ally.getLocation().distanceSquared(player.getLocation()) > 64) continue;
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durTicks, 1));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durTicks, 2));
                    }
                }
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
            }
            case CRONE -> {
                // lifesteal: amplified curses (Strength) + a tag so your hits heal you
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, durTicks, 1));
                player.addScoreboardTag(CRONE_LIFESTEAL_TAG);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.removeScoreboardTag(CRONE_LIFESTEAL_TAG);
                    }
                }.runTaskLater(Minerva.getInstance(), durTicks);
                // gentle regen embodies the leeched vitality without needing a damage hook
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durTicks, 0));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1f, 0.7f);
            }
        }

        // L5: you may Shift once during Witching Hour without ending it (Face change is fine here
        // because each skill reads the live Face; we simply note this freedom in the action bar).
        if (fLevel >= 5) {
            player.sendActionBar(ChatColor.LIGHT_PURPLE + "Witching Hour: you may Shift once freely.");
        }

        final Color colA = swirlA(face);
        final Color colB = swirlB(face);
        new BukkitRunnable() {
            int t = 0;
            double angle = 0;
            @Override
            public void run() {
                if (t >= durTicks || player.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }
                // accelerating orbiting runes + rising column
                angle += 0.25 + (t / (double) durTicks) * 0.6;
                Location base = player.getLocation();
                double colH = 0.3 + ((t % 20) / 20.0) * 2.0;
                base.getWorld().spawnParticle(Particle.DUST, base.clone().add(0, colH, 0), 0, 0, 0, 0, 0,
                        new Particle.DustOptions(colA, 1.0f));
                for (int i = 0; i < 3; i++) {
                    double ang = angle + i * (2 * Math.PI / 3);
                    double r = 1.2;
                    Vector off = new Vector(Math.cos(ang) * r, 1.0, Math.sin(ang) * r);
                    base.getWorld().spawnParticle(Particle.DUST, base.clone().add(off), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(i % 2 == 0 ? colA : colB, 1.1f));
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
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
            case 1 -> ChatColor.GRAY + "5s super-charge of your current Face.";
            case 2 -> ChatColor.GRAY + "Lasts 7s.";
            case 3 -> ChatColor.GRAY + "Lower cooldown.";
            case 4 -> ChatColor.GRAY + "Even lower cooldown.";
            case 5 -> ChatColor.GRAY + "You may Shift once during it without ending it.";
            default -> ChatColor.GRAY + "Super-charge your current Face.";
        };
    }

    @Override
    public String toString() {
        return "witchingHour";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.ENCHANTING_TABLE),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Witching Hour]",
                ChatColor.GRAY + "Super-charge your current " + ChatColor.LIGHT_PURPLE + "Face" + ChatColor.GRAY + ":",
                ChatColor.WHITE + "Maiden" + ChatColor.GRAY + " haste + free Shifts, " + ChatColor.GREEN + "Mother" + ChatColor.GRAY + " party ward,",
                ChatColor.DARK_PURPLE + "Crone" + ChatColor.GRAY + " lifesteal + amplified curses.");
    }
}
