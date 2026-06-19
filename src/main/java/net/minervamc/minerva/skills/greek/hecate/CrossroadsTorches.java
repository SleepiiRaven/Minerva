package net.minervamc.minerva.skills.greek.hecate;

import java.util.ArrayList;
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
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Hecate RLR — plant a torch (no real block placed; a particle pylon) at the block you aim at. Its
 * effect is fixed to the Face you were wearing when you placed it, so mixing Faces across your three
 * torches is the depth: MAIDEN pulses Speed to you + party, MOTHER heals the party, CRONE damages
 * enemies. Three independent charges.
 */
public class CrossroadsTorches extends Skill {
    /** A live torch tracked so SpectralHex L5 can trigger the closest one on a struck enemy. */
    public static final class Torch {
        public final Player owner;
        public final Face face;
        public final Location loc;
        public final int level;
        Torch(Player owner, Face face, Location loc, int level) {
            this.owner = owner;
            this.face = face;
            this.loc = loc;
            this.level = level;
        }
    }

    private static final List<Torch> ACTIVE = new ArrayList<>();

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
        long cooldown = 7000;
        int charges = level >= 2 ? 4 : 3;

        String chosen = null;
        for (int i = 0; i < charges; i++) {
            String key = "hecateTorch" + i;
            if (cooldownManager.isCooldownDone(player.getUniqueId(), key)) {
                chosen = key;
                break;
            }
        }
        if (chosen == null) {
            onCooldown(player);
            return;
        }

        Block aimed = player.getTargetBlockExact(20);
        Location loc = aimed != null
                ? aimed.getLocation().clone().add(0.5, 1, 0.5)
                : player.getLocation().clone().add(player.getLocation().getDirection().setY(0).normalize().multiply(3));

        cooldownManager.setCooldownFromNow(player.getUniqueId(), chosen, cooldown);
        cooldownAlarm(player, cooldown, "Crossroads Torch");

        Face face = TripleGoddess.getFace(player);
        int lifetime = level >= 3 ? 280 : 200;
        final int fLevel = level;

        player.getWorld().playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.8f, 1.2f);

        Torch torch = new Torch(player, face, loc.clone(), fLevel);
        ACTIVE.add(torch);

        new BukkitRunnable() {
            int t = 0;
            double spin = 0;
            @Override
            public void run() {
                if (t >= lifetime || !player.isOnline()) {
                    ACTIVE.remove(torch);
                    if (fLevel >= 5) detonate(torch);
                    cancel();
                    return;
                }
                spin += 0.4;
                drawTorch(torch, spin, t);

                if (t % 20 == 0 && t > 0) {
                    applyTorchTick(torch, fLevel);
                }
                t++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    /** Per-second torch effect, by Face. */
    private static void applyTorchTick(Torch torch, int level) {
        Player owner = torch.owner;
        Location loc = torch.loc;
        switch (torch.face) {
            case MAIDEN -> {
                // SPEED pulses to owner + party within 4
                owner.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.6f);
                if (loc.getWorld().equals(owner.getWorld()) && owner.getLocation().distanceSquared(loc) <= 16) {
                    owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50, 0));
                    if (level >= 4) owner.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 50, 1));
                }
                List<Player> party = Party.partyList(owner);
                if (party != null) {
                    for (Player ally : party) {
                        if (ally == null || !ally.isOnline()) continue;
                        if (ally.getLocation().distanceSquared(loc) > 16) continue;
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50, 0));
                        if (level >= 4) ally.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 50, 1));
                    }
                }
            }
            case MOTHER -> {
                // heal party within 4
                owner.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, 1.2f);
                healWithin(owner, loc, owner, level);
                List<Player> party = Party.partyList(owner);
                if (party != null) {
                    for (Player ally : party) {
                        if (ally == null || !ally.isOnline()) continue;
                        healWithin(owner, loc, ally, level);
                    }
                }
            }
            case CRONE -> {
                // 2 dmg/sec to enemies within 4
                owner.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.7f);
                for (Entity e : nearbyEntities(loc, 4)) {
                    if (!(e instanceof LivingEntity le) || e == owner) continue;
                    if (le instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) continue;
                    if (PlayerStats.isSummoned(owner, le)) continue;
                    damage(le, 2, owner, true, true);
                    if (level >= 4) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0));
                }
            }
        }
    }

    private static void healWithin(Player owner, Location loc, Player ally, int level) {
        if (!ally.getWorld().equals(loc.getWorld())) return;
        if (ally.getLocation().distanceSquared(loc) > 16) return;
        double heal = level >= 4 ? 3 : 2;
        double max = ally.getMaxHealth();
        ally.setHealth(Math.min(max, ally.getHealth() + heal));
        ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().clone().add(0, 1.2, 0), 1, 0.2, 0.2, 0.2, 0);
    }

    private static List<Entity> nearbyEntities(Location loc, double r) {
        return new ArrayList<>(loc.getWorld().getNearbyEntities(loc, r, r, r));
    }

    /** Very different per-Face visuals. Steady gradients, smooth motion, no strobe. */
    private static void drawTorch(Torch torch, double spin, int t) {
        Location loc = torch.loc;
        switch (torch.face) {
            case MAIDEN -> {
                // slim silver vertical beacon + small orbiting ring
                for (double y = 0; y <= 2.2; y += 0.25) {
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(TripleGoddess.MAIDEN_SILVER, 0.9f));
                }
                for (int i = 0; i < 4; i++) {
                    double a = spin + i * (Math.PI / 2);
                    Vector off = new Vector(Math.cos(a) * 0.5, 1.1, Math.sin(a) * 0.5);
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(off), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(200, 212, 255), 0.8f));
                }
            }
            case MOTHER -> {
                // fat green-gold bloom: spore + cherry leaves rising
                loc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc.clone().add(0, 0.8, 0), 3, 0.5, 0.6, 0.5, 0.01);
                if (t % 2 == 0) {
                    loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc.clone().add(0, 1.4, 0), 2, 0.4, 0.4, 0.4, 0.02);
                }
                for (int i = 0; i < 3; i++) {
                    double a = spin + i * (2 * Math.PI / 3);
                    Vector off = new Vector(Math.cos(a) * 0.7, 0.6 + (i * 0.3), Math.sin(a) * 0.7);
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(off), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(i % 2 == 0 ? TripleGoddess.MOTHER_GREEN : TripleGoddess.MOTHER_GOLD, 1.1f));
                }
            }
            case CRONE -> {
                // low violet-black pyre: dark dust + witch motes curling up
                for (double y = 0; y <= 1.2; y += 0.3) {
                    double a = spin + y * 3;
                    Vector off = new Vector(Math.cos(a) * (0.4 - y * 0.2), y, Math.sin(a) * (0.4 - y * 0.2));
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(off), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(y < 0.6 ? TripleGoddess.CRONE_VIOLET : TripleGoddess.CRONE_BLACK, 1.2f));
                }
                if (t % 3 == 0) {
                    loc.getWorld().spawnParticle(Particle.WITCH, loc.clone().add(0, 0.7, 0), 1, 0.25, 0.4, 0.25, 0);
                }
            }
        }
    }

    /** L5: a Face-flavoured burst when the torch expires. */
    private static void detonate(Torch torch) {
        Player owner = torch.owner;
        Location loc = torch.loc;
        switch (torch.face) {
            case MAIDEN -> {
                owner.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
                for (Vector v : ParticleUtils.getSpherePoints(2.0, 6)) {
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(v), 0, 0, 0, 0, 0,
                            new Particle.DustOptions(TripleGoddess.MAIDEN_SILVER, 1.0f));
                }
                if (owner.isOnline() && owner.getLocation().distanceSquared(loc) <= 36) {
                    owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 1));
                }
            }
            case MOTHER -> {
                owner.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.4f);
                loc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc.clone().add(0, 1, 0), 30, 1.5, 1, 1.5, 0.02);
                List<Player> party = Party.partyList(owner);
                healWithin(owner, loc, owner, torch.level);
                if (party != null) {
                    for (Player ally : party) {
                        if (ally == null || !ally.isOnline()) continue;
                        healWithin(owner, loc, ally, torch.level);
                        healWithin(owner, loc, ally, torch.level);
                    }
                }
            }
            case CRONE -> {
                owner.getWorld().playSound(loc, Sound.ENTITY_WITCH_AMBIENT, 0.9f, 0.7f);
                loc.getWorld().spawnParticle(Particle.WITCH, loc.clone().add(0, 0.8, 0), 30, 1.5, 0.8, 1.5, 0.05);
                for (Entity e : nearbyEntities(loc, 4)) {
                    if (!(e instanceof LivingEntity le) || e == owner) continue;
                    if (le instanceof Player p && Party.isPlayerInPlayerParty(owner, p)) continue;
                    if (PlayerStats.isSummoned(owner, le)) continue;
                    damage(le, 6, owner);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
                }
            }
        }
    }

    /**
     * Find this owner's torch closest to a location within range, and immediately fire its
     * per-Face effect there (used by SpectralHex L5 when hitting an enemy near a torch).
     * Returns true if a torch was found and triggered.
     */
    public static boolean triggerNearest(Player owner, Location at, double range) {
        Torch best = null;
        double bestSq = range * range;
        for (Torch torch : ACTIVE) {
            if (torch.owner != owner) continue;
            if (!torch.loc.getWorld().equals(at.getWorld())) continue;
            double d = torch.loc.distanceSquared(at);
            if (d <= bestSq) {
                bestSq = d;
                best = torch;
            }
        }
        if (best == null) return false;
        applyTorchTick(best, best.level);
        // a small accent burst at the torch so the trigger is visible
        best.loc.getWorld().spawnParticle(Particle.DUST, best.loc.clone().add(0, 1.1, 0), 6, 0.3, 0.4, 0.3, 0,
                new Particle.DustOptions(TripleGoddess.faceColor(best.face), 1.0f));
        return true;
    }

    @Override
    public String getLevelDescription(int level) {
        return switch (level) {
            case 1 -> ChatColor.GRAY + "Plant a torch fixed to your current Face (3 charges).";
            case 2 -> ChatColor.GRAY + "Gain a 4th torch charge.";
            case 3 -> ChatColor.GRAY + "Torches last longer.";
            case 4 -> ChatColor.GRAY + "Each Face's torch gains a secondary effect.";
            case 5 -> ChatColor.GRAY + "Torches detonate on expiry for a Face-flavoured burst.";
            default -> ChatColor.GRAY + "Plant a Face-locked torch.";
        };
    }

    @Override
    public String toString() {
        return "crossroadsTorches";
    }

    @Override
    public ItemStack getItem() {
        return ItemUtils.getItem(new ItemStack(Material.SOUL_TORCH),
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Crossroads Torches]",
                ChatColor.GRAY + "Plant a torch locked to your current " + ChatColor.LIGHT_PURPLE + "Face" + ChatColor.GRAY + ":",
                ChatColor.WHITE + "Maiden" + ChatColor.GRAY + " speeds, " + ChatColor.GREEN + "Mother" + ChatColor.GRAY + " heals,",
                ChatColor.DARK_PURPLE + "Crone" + ChatColor.GRAY + " burns. Mix Faces across your charges.");
    }
}
