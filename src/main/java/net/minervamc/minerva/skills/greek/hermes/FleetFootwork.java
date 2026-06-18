package net.minervamc.minerva.skills.greek.hermes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.format.TextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class FleetFootwork extends Skill {

    public static final int MAX_MOMENTUM = 8;
    private static final String MOMENTUM_KEY = "hermesMomentum";
    private static final long MOMENTUM_EXPIRE_MS = 3600;
    private static final long MOVE_STEP_MS = 85;
    private static final long STRIKE_COOLDOWN_MS = 650;
    private static final long RUN_STACK_MS = 420;

    private static final Map<UUID, Long> LAST_MOVE_STEP = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_RUN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_VISUAL = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> BASE_WALK_SPEED = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> DECAY_TASKS = new ConcurrentHashMap<>();

    private static final Color[] GOLD = {
        Color.fromRGB(255, 246, 190),
        Color.fromRGB(255, 214, 92),
        Color.fromRGB(245, 170, 45),
        Color.fromRGB(125, 230, 255)
    };

    @Override
    public void cast(Player player, CooldownManager cooldownManager, int level) {
    }

    public static void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isActive(player)) {
            clear(player);
            return;
        }

        Location to = event.getTo();
        if (to == null) return;

        double moved = horizontalDistance(event.getFrom(), to);
        long now = System.currentTimeMillis();
        if (now - LAST_MOVE_STEP.getOrDefault(player.getUniqueId(), 0L) < MOVE_STEP_MS) return;
        LAST_MOVE_STEP.put(player.getUniqueId(), now);

        boolean running = player.isSprinting() && !player.isSneaking() && moved > 0.025;
        if (running) {
            LAST_RUN.put(player.getUniqueId(), now);
            CooldownManager cooldownManager = Minerva.getInstance().getCdInstance();
            if (cooldownManager.isCooldownDone(player.getUniqueId(), "hermesMomentumStep")) {
                cooldownManager.setCooldownFromNow(player.getUniqueId(), "hermesMomentumStep", RUN_STACK_MS);
                addMomentum(player, 1);
            }
        }

        int momentum = getMomentumStacks(player);
        applyMomentumSpeed(player, momentum);
        ensureDecayTask(player);
        if (running && now - LAST_VISUAL.getOrDefault(player.getUniqueId(), 0L) > 130) {
            LAST_VISUAL.put(player.getUniqueId(), now);
            spawnFootwork(player, momentum);
        }
    }

    public static void onWeaponHit(Player player, EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!isActive(player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (target == player) return;
        if (target instanceof Player p && Party.isPlayerInPlayerParty(player, p)) return;
        if (PlayerStats.isSummoned(player, target)) return;

        double speedScore = getSpeedScore(player);
        if (speedScore < 0.32) return;

        CooldownManager cooldownManager = Minerva.getInstance().getCdInstance();
        if (!cooldownManager.isCooldownDone(player.getUniqueId(), "fleetFootworkStrike")) return;
        cooldownManager.setCooldownFromNow(player.getUniqueId(), "fleetFootworkStrike", STRIKE_COOLDOWN_MS);

        double bonusDamage = 1.0 + speedScore * 3.2;
        event.setDamage(event.getDamage() + bonusDamage);
        addMomentum(player, 0.45 + speedScore * 0.45);

        Location hitLoc = target.getLocation().add(0, 1, 0);
        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.65f);
        hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.45f, 1.9f);
        hitLoc.getWorld().spawnParticle(Particle.CRIT, hitLoc, 18, 0.25, 0.25, 0.25, 0.35);
        hitLoc.getWorld().spawnParticle(Particle.END_ROD, hitLoc, 10, 0.22, 0.22, 0.22, 0.08);
        for (int i = 0; i < 14; i++) {
            double angle = Math.PI * 2 * i / 14.0;
            Vector ring = new Vector(Math.cos(angle) * 0.65, 0.08 * Math.sin(angle * 2), Math.sin(angle) * 0.65);
            hitLoc.getWorld().spawnParticle(Particle.DUST, hitLoc.clone().add(ring), 0, 0, 0, 0, 0,
                new Particle.DustOptions(GOLD[i % GOLD.length], 1.15f));
        }
    }

    public static boolean shouldCancelFall(Player player) {
        if (player.getScoreboardTags().contains("hermesFallGrace") || player.getScoreboardTags().contains("hermesWake")) {
            softenLanding(player);
            return true;
        }
        if (!isActive(player) || getMomentum(player) < 1.0) return false;
        softenLanding(player);
        return true;
    }

    public static double getSpeedScore(Player player) {
        Vector velocity = player.getVelocity().clone();
        velocity.setY(0);
        double velocityScore = Math.min(1.0, velocity.length() / 0.78);
        double momentumScore = getMomentum(player) / MAX_MOMENTUM;
        return Math.min(1.0, Math.max(velocityScore, momentumScore));
    }

    public static double getMomentumRatio(Player player) {
        return getMomentumStacks(player) / (double) MAX_MOMENTUM;
    }

    public static double getMomentum(Player player) {
        return getMomentumStacks(player);
    }

    public static int getMomentumStacks(Player player) {
        return Skill.getStacks(player, MOMENTUM_KEY);
    }

    public static double addMomentum(Player player, double amount) {
        if (!isActive(player) || amount <= 0) return getMomentum(player);

        int increment = Math.max(1, (int) Math.round(amount));
        Skill.stack(player, MOMENTUM_KEY, increment, "Momentum", MOMENTUM_EXPIRE_MS);
        int momentum = getMomentumStacks(player);
        applyMomentumSpeed(player, momentum);
        ensureDecayTask(player);
        return momentum;
    }

    public static double consumeMomentum(Player player, double amount) {
        if (!isActive(player) || amount <= 0) return 0;

        int current = getMomentumStacks(player);
        int spent = Math.min(current, Math.max(1, (int) Math.round(amount)));
        if (spent <= 0) return 0;

        Skill.stack(player, MOMENTUM_KEY, -spent, "Momentum", MOMENTUM_EXPIRE_MS);
        applyMomentumSpeed(player, getMomentumStacks(player));
        ensureDecayTask(player);
        return spent;
    }

    public int stackMomentum(Player player, int stacks, long millisUntilOver) {
        int capped = Math.min(stacks, MAX_MOMENTUM);
        if (capped <= 0) {
            restoreWalkSpeed(player);
            return capped;
        }

        applyMomentumSpeed(player, capped);
        ensureDecayTask(player);

        if (capped >= MAX_MOMENTUM) {
            Location loc = player.getLocation().add(0, 1, 0);
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 8, 0.35, 0.35, 0.35, 0.08);
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.45f, 1.9f);
        }
        return capped;
    }

    public static void clear(Player player) {
        PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
        if (stats != null) stats.getStackingAbilities().remove(MOMENTUM_KEY);
        Minerva.getInstance().getCdInstance().setCooldownFromNow(player.getUniqueId(), MOMENTUM_KEY, 0L);
        LAST_MOVE_STEP.remove(player.getUniqueId());
        LAST_RUN.remove(player.getUniqueId());
        LAST_VISUAL.remove(player.getUniqueId());
        restoreWalkSpeed(player);
        BukkitTask task = DECAY_TASKS.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private static boolean isActive(Player player) {
        if (player.hasMetadata("NPC")) return false;
        PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
        return stats != null
            && stats.getPassive() != null
            && "fleetFootwork".equals(stats.getPassive().toString())
            && stats.getPassiveActive();
    }

    private static void ensureDecayTask(Player player) {
        if (DECAY_TASKS.containsKey(player.getUniqueId())) return;
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isActive(player)) {
                    clear(player);
                    cancel();
                    return;
                }

                int momentum = getMomentumStacks(player);
                if (momentum <= 0) {
                    applyMomentumSpeed(player, 0);
                    DECAY_TASKS.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                long now = System.currentTimeMillis();
                boolean recentlyRunning = now - LAST_RUN.getOrDefault(player.getUniqueId(), 0L) < 260;
                boolean wakeGrace = player.getScoreboardTags().contains("hermesWake");
                if (!recentlyRunning && (!wakeGrace || momentum > MAX_MOMENTUM / 2)) {
                    Skill.stack(player, MOMENTUM_KEY, -1, "Momentum", MOMENTUM_EXPIRE_MS);
                    momentum = getMomentumStacks(player);
                }

                applyMomentumSpeed(player, momentum);
                if (momentum > 0 && now - LAST_VISUAL.getOrDefault(player.getUniqueId(), 0L) > 210) {
                    LAST_VISUAL.put(player.getUniqueId(), now);
                    spawnFootwork(player, momentum);
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 8L, 8L);
        DECAY_TASKS.put(player.getUniqueId(), task);
    }

    private static void applyMomentumSpeed(Player player, int momentum) {
        if (momentum <= 0) {
            restoreWalkSpeed(player);
            return;
        }

        BASE_WALK_SPEED.putIfAbsent(player.getUniqueId(), player.getWalkSpeed());
        float base = BASE_WALK_SPEED.getOrDefault(player.getUniqueId(), 0.2f);
        float bonus = (float) (0.012 + 0.108 * Math.pow(momentum / (double) MAX_MOMENTUM, 0.85));
        if (player.getScoreboardTags().contains("hermesWake")) bonus += 0.018f;
        player.setWalkSpeed(Math.min(0.34f, base + bonus));
    }

    private static void restoreWalkSpeed(Player player) {
        Float base = BASE_WALK_SPEED.remove(player.getUniqueId());
        if (base != null && player.isOnline()) {
            player.setWalkSpeed(base);
        }
    }

    private static void spawnFootwork(Player player, int momentum) {
        Location base = player.getLocation().add(0, 0.12, 0);
        Vector forward = player.getVelocity().clone();
        forward.setY(0);
        if (forward.lengthSquared() < 0.001) {
            forward = player.getLocation().getDirection().setY(0);
        }
        if (forward.lengthSquared() < 0.001) return;
        forward.normalize();

        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        double intensity = 0.6 + momentum / (double) MAX_MOMENTUM;

        for (int side : new int[] {-1, 1}) {
            for (int i = 0; i < 5; i++) {
                double spread = side * (0.16 + i * 0.08);
                double back = 0.08 + i * 0.12;
                double lift = Math.sin((i + 1) / 5.0 * Math.PI) * 0.22;
                Location point = base.clone()
                    .add(right.clone().multiply(spread))
                    .subtract(forward.clone().multiply(back))
                    .add(0, lift, 0);
                player.getWorld().spawnParticle(Particle.DUST, point, 0, 0, 0, 0, 0,
                    new Particle.DustOptions(GOLD[(i + side + GOLD.length) % GOLD.length], (float) intensity));
            }
        }

        player.getWorld().spawnParticle(Particle.END_ROD, base.clone().subtract(forward.clone().multiply(0.45)),
            2, 0.08, 0.05, 0.08, 0.025);
        if (momentum >= MAX_MOMENTUM) {
            player.getWorld().spawnParticle(Particle.FIREWORK, base, 3, 0.16, 0.08, 0.16, 0.04);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.22f, 1.9f);
        }
    }

    private static void softenLanding(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 18, 0.45, 0.05, 0.45, 0.05);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc.add(0, 0.15, 0), 10, 0.35, 0.08, 0.35, 0.04);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.55f, 1.8f);
        player.setFallDistance(0);
    }

    private static double horizontalDistance(Location from, Location to) {
        double dx = from.getX() - to.getX();
        double dz = from.getZ() - to.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public String getLevelDescription(int level) {
        return "";
    }

    @Override
    public String toString() {
        return "fleetFootwork";
    }

    @Override
    public ItemStack getItem() {
        return ItemCreator.get(Material.FEATHER)
            .setName(TextContext.formatLegacy("&lFleet Footwork", false).color(TextColor.color(255, 214, 92)))
            .setLore(List.of(
                TextContext.formatLegacy("&7Sprinting and Hermes skills build", false),
                TextContext.formatLegacy("&eMomentum stacks &7(max 8).", false),
                TextContext.formatLegacy("&7Stacks directly raise walk speed;", false),
                TextContext.formatLegacy("&7when they decay, that speed is lost.", false),
                TextContext.formatLegacy("&eMomentum &7also boosts melee damage", false),
                TextContext.formatLegacy("&7and softens graceful landings.", false)
            )).build();
    }
}
