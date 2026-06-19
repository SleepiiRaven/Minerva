package net.minervamc.minerva.skills.greek.nemesis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Nemesis PASSIVE engine. Damage TAKEN banks into the Ledger: every wound is a deposit.
 * The Ledger decays slowly and, when it crosses half of its soft cap, raises a dark-gold
 * thorns aura that punishes nearby melee attackers. The other Nemesis skills read, feed,
 * and spend this bank. A black-and-gold scale/sword motif orbits the caster, sized to the
 * ledger. No strobing: steady spatial DUST gradients only.
 */
public class LedgerOfWrongs extends Skill {
    public static final Color[] BLACKGOLD = {
        Color.fromRGB(40, 34, 10), Color.fromRGB(120, 95, 20),
        Color.fromRGB(200, 165, 40), Color.fromRGB(255, 215, 80),
    };

    /** Soft cap on the ledger; thorns aura activates above half of this. */
    public static final double SOFT_CAP = 60.0;

    public static final Map<UUID, Double> LEDGER = new HashMap<>();
    // tracks the last quarter (0..4) crossed, so the bell only rings on a new threshold
    private static final Map<UUID, Integer> LAST_QUARTER = new HashMap<>();

    // ===================================================================================
    // REQUIRED HOOKS (verbatim signatures — external code calls these)
    // ===================================================================================

    /** Damage taken banks a fraction into the Ledger. Bracing doubles the deposit. */
    public static void onTakeDamage(Player player, EntityDamageEvent event, int level) {
        if (player == null || event == null) return;
        double bankPct = 0.3 + 0.1 * (level >= 2 ? 1 : 0); // 30% base, 40% at L2+
        double banked = event.getFinalDamage() * bankPct;
        if (player.getScoreboardTags().contains("nemesisBracing")) {
            banked *= 2; // Brace doubles the absorbed deposit
        }
        addToLedger(player, banked);

        // bell chime when the ledger crosses each quarter of the soft cap
        double cap = softCap(level);
        int quarter = (int) Math.min(4, Math.floor((getLedger(player) / cap) * 4));
        int last = LAST_QUARTER.getOrDefault(player.getUniqueId(), 0);
        if (quarter > last) {
            LAST_QUARTER.put(player.getUniqueId(), quarter);
            float pitch = 0.7f + 0.18f * quarter;
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, pitch);
        } else if (quarter < last) {
            LAST_QUARTER.put(player.getUniqueId(), quarter);
        }
    }

    /** 1/sec maintenance: decay the ledger and, above the thorns threshold, punish nearby melee. */
    public static void tick(Player player, int level) {
        if (player == null || !player.isOnline()) return;
        double cur = getLedger(player);
        if (cur > 0) {
            cur = Math.max(0, cur - cur * 0.02); // decay 2% of current per second
            if (cur <= 0.01) {
                LEDGER.remove(player.getUniqueId());
                LAST_QUARTER.remove(player.getUniqueId());
                cur = 0;
            } else {
                LEDGER.put(player.getUniqueId(), cur);
            }
        }

        double cap = softCap(level);
        boolean thorns = cur > cap * 0.5;

        // orbiting scale/sword motif sized to the ledger (steady, single per-tick sample)
        drawMotif(player, cur, cap, thorns);

        if (thorns) {
            double thornsDmg = 1.5 + 0.4 * level;
            for (Entity e : player.getNearbyEntities(2.4, 2.4, 2.4)) {
                if (!(e instanceof LivingEntity le) || e == player) continue;
                if (le instanceof Player p && Party.isPlayerInPlayerParty(player, p)) continue;
                if (PlayerStats.isSummoned(player, le)) continue;
                if (le.getNoDamageTicks() > 0) continue;
                damage(le, thornsDmg, player);
                if (level >= 4) {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 30, 0));
                }
                Location m = le.getLocation().clone().add(0, le.getHeight() / 2, 0);
                le.getWorld().spawnParticle(Particle.DUST, m, 4, 0.25, 0.3, 0.25, 0,
                        new Particle.DustOptions(BLACKGOLD[3], 1f));
            }

            // L5: a portion of damage taken by nearby party allies also banks (you bear grievances)
            if (level >= 5) {
                java.util.List<Player> party = Party.partyList(player);
                if (party != null) {
                    for (Player ally : party) {
                        if (ally == null || ally == player || !ally.isOnline()) continue;
                        if (ally.getWorld() != player.getWorld()) continue;
                        if (ally.getLocation().distanceSquared(player.getLocation()) > 100) continue;
                        ally.addScoreboardTag("nemesisGrieved");
                    }
                }
            }
        }
    }

    public static double getLedger(Player player) {
        return LEDGER.getOrDefault(player.getUniqueId(), 0.0);
    }

    public static void spend(Player player) {
        LEDGER.remove(player.getUniqueId());
        LAST_QUARTER.remove(player.getUniqueId());
    }

    public static void addToLedger(Player player, double amt) {
        if (player == null || amt <= 0) return;
        // hard ceiling based on the highest possible soft cap, so the bank can't run away
        double cur = Math.min(softCap(5) * 2, getLedger(player) + amt);
        LEDGER.put(player.getUniqueId(), cur);
    }

    // ===================================================================================
    // Helpers
    // ===================================================================================

    /** Soft cap grows with level (L3 raises it). */
    public static double softCap(int level) {
        return level >= 3 ? 80.0 : SOFT_CAP;
    }

    private static void drawMotif(Player player, double cur, double cap, boolean thorns) {
        if (cur <= 0) return;
        double fill = Math.min(1.0, cur / cap);
        Location c = player.getLocation().clone().add(0, 1.1, 0);
        double radius = 0.8 + 0.7 * fill; // beam between the pans widens as the ledger fills
        long phase = player.getWorld().getFullTime();
        double baseAngle = (phase % 80) / 80.0 * Math.PI * 2;
        // two opposing scale pans + a vertical sword line through the centre
        for (int i = 0; i < 2; i++) {
            double a = baseAngle + i * Math.PI;
            Vector v = new Vector(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            Color col = BLACKGOLD[2 + (int) Math.round(fill)]; // brighter when fuller, spatially fixed
            c.getWorld().spawnParticle(Particle.DUST, c.clone().add(v), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(col, 1.1f));
        }
        // sword blade rising through the motif
        for (double y = -0.4; y <= 0.9; y += 0.25) {
            Color col = y > 0.4 ? BLACKGOLD[3] : BLACKGOLD[1];
            c.getWorld().spawnParticle(Particle.DUST, c.clone().add(0, y, 0), 0, 0, 0, 0, 0,
                    new Particle.DustOptions(col, 0.9f));
        }
        if (thorns) {
            for (Vector v : ParticleUtils.getCirclePoints(2.0, 12)) {
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().clone().add(v).add(0, 0.1, 0),
                        0, 0, 0, 0, 0, new Particle.DustOptions(BLACKGOLD[0], 1f));
            }
        }
    }

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) { }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "30% of damage you take banks into the Ledger.";
            case 2 -> ChatColor.GRAY + "Bank 40% of damage taken.";
            case 3 -> ChatColor.GRAY + "Higher Ledger cap.";
            case 4 -> ChatColor.GRAY + "Thorns aura also weakens attackers.";
            case 5 -> ChatColor.GRAY + "Damage to nearby allies also banks for you.";
            default -> ChatColor.GRAY + "Damage you take banks into the Ledger.";
        };
    }

    @Override
    public String toString() {
        return "ledgerOfWrongs";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.WRITABLE_BOOK),
                ChatColor.GOLD + "" + ChatColor.BOLD + "[Ledger of Wrongs]",
                ChatColor.GRAY + "Every wound is a deposit. " + ChatColor.GOLD + "30%" + ChatColor.GRAY + " of all",
                ChatColor.GRAY + "damage you take banks into your " + ChatColor.GOLD + "Ledger" + ChatColor.GRAY + ".",
                ChatColor.GRAY + "A full Ledger raises a " + ChatColor.GOLD + "thorns aura" + ChatColor.GRAY + " — then",
                ChatColor.GRAY + "spend it as " + ChatColor.WHITE + "retribution" + ChatColor.GRAY + ".");
    }
}
