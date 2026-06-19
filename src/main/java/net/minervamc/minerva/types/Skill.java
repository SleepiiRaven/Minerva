package net.minervamc.minerva.types;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.event.AttackUnregisteredEvent;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.provider.StatProvider;
import io.lumine.mythic.lib.damage.AttackMetadata;
import io.lumine.mythic.lib.damage.DamageMetadata;
import io.lumine.mythic.lib.listener.option.DamageIndicators;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.party.Party;
import net.minervamc.minerva.skills.Skills;
import net.minervamc.minerva.skills.cooldown.CooldownManager;
import net.minervamc.minerva.skills.greek.aphrodite.Doves;
import net.minervamc.minerva.skills.greek.hephaestus.Smolder;
import net.minervamc.minerva.skills.greek.hermes.FleetFootwork;
import net.minervamc.minerva.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public abstract class Skill {
    public static Skill fromString(String string) {
        return switch (string) {
            case "channelingOfTartarus" -> Skills.CHANNELING_OF_TARTARUS;
            case "umbrakinesisHades" -> Skills.UMBRAKINESIS_HADES;
            case "skeletalHands" -> Skills.SKELETAL_HANDS;
            case "shadowTravel" -> Skills.SHADOW_TRAVEL;
            case "windWall" -> Skills.WIND_WALL;
            case "soar" -> Skills.SOAR;
            case "lightningToss" -> Skills.LIGHTNING_TOSS;
            case "stormsEmbrace" -> Skills.STORMS_EMBRACE;
            case "seismicBlast" -> Skills.SEISMIC_BLAST;
            case "oceansSurge" -> Skills.OCEANS_SURGE;
            case "tidalWave" -> Skills.TIDAL_WAVE;
            case "aquaticLimbExtensions" -> Skills.AQUATIC_LIMB_EXTENSIONS;
            case "lifeSteal" -> Skills.LIFE_STEAL;
            case "oceansEmbrace" -> Skills.OCEANS_EMBRACE;
            case "protectiveCloud" -> Skills.PROTECTIVE_CLOUD;
            case "apollosHymn" -> Skills.APOLLOS_HYMN;
            case "arrowsOfTheSun" -> Skills.ARROWS_OF_THE_SUN;
            case "burningLight" -> Skills.BURNING_LIGHT;
            case "enhancedArchery" -> Skills.ENHANCED_ARCHERY;
            case "plagueVolley" -> Skills.PLAGUE_VOLLEY;
            case "callOfTheWild" -> Skills.CALL_OF_THE_WILD;
            case "huntressAgility" -> Skills.HUNTRESS_AGILITY;
            case "nimbleDash" -> Skills.NIMBLE_DASH;
            case "superCharged" -> Skills.SUPER_CHARGED;
            case "sharpshooter" -> Skills.SHARPSHOOTER;
            case "drunkenRevelry" -> Skills.DRUNKEN_REVELRY;
            case "frenziedDance" -> Skills.FRENZIED_DANCE;
            case "grapeShot" -> Skills.GRAPE_SHOT;
            case "vineWhip" -> Skills.VINE_WHIP;
            case "madGodsDrink" -> Skills.MAD_GODS_DRINK;
            case "aresBlessing" -> Skills.ARES_BLESSING;
            case "spiritOfVengeance" -> Skills.SPIRIT_OF_VENGEANCE;
            case "cleave" -> Skills.CLEAVE;
            case "primalScream" -> Skills.PRIMAL_SCREAM;
            case "tomahawkThrow" -> Skills.TOMAHAWK_THROW;
            case "groundBreaker" -> Skills.GROUND_BREAKER;
            case "livingForge" -> Skills.LIVING_FORGE;
            case "magmatism" -> Skills.MAGMATISM;
            case "shrapnelGrenade" -> Skills.SHRAPNEL_GRENADE;
            case "smolder" -> Skills.SMOLDER;
            case "doves" -> Skills.DOVES;
            case "charm" -> Skills.CHARM;
            case "serenity" -> Skills.SERENITY;
            case "mirrorImage" -> Skills.MIRROR_IMAGE;
            case "heartSeeker" -> Skills.HEART_SEEKER;
            case "parry" -> Skills.PARRY;
            case "spearOfAthena" -> Skills.SPEAR_OF_ATHENA;
            case "aegisRush" -> Skills.AEGIS_RUSH;
            case "spearRain" -> Skills.SPEAR_RAIN;
            case "tacticalAgility" -> Skills.TACTICAL_AGILITY;
            case "rootSurge" -> Skills.ROOT_SURGE;
            case "seedBarrage" -> Skills.SEED_BARRAGE;
            case "thornBlight" -> Skills.THORN_BLIGHT;
            case "harvestWrath" -> Skills.HARVEST_WRATH;
            case "harvestBlessing" -> Skills.HARVEST_BLESSING;
            case "vineGrapple" -> Skills.VINE_GRAPPLE;
            case "talariaStep" -> Skills.TALARIA_STEP;
            case "caduceusArc" -> Skills.CADUCEUS_ARC;
            case "messengerWake" -> Skills.MESSENGER_WAKE;
            case "kineticDispatch" -> Skills.KINETIC_DISPATCH;
            case "fleetFootwork" -> Skills.FLEET_FOOTWORK;
            case "glacialGlide" -> Skills.GLACIAL_GLIDE;
            case "rimeNova" -> Skills.RIME_NOVA;
            case "shatterpoint" -> Skills.SHATTERPOINT;
            case "coldSnap" -> Skills.COLD_SNAP;
            case "frostbite" -> Skills.FROSTBITE;
            case "advanceTheLine" -> Skills.ADVANCE_THE_LINE;
            case "plantWarBanner" -> Skills.PLANT_WAR_BANNER;
            case "cannonade" -> Skills.CANNONADE;
            case "standardsCall" -> Skills.STANDARDS_CALL;
            case "warFooting" -> Skills.WAR_FOOTING;
            case "emberVeil" -> Skills.EMBER_VEIL;
            case "tendTheHearth" -> Skills.TEND_THE_HEARTH;
            case "pyreRelease" -> Skills.PYRE_RELEASE;
            case "vestasVeil" -> Skills.VESTAS_VEIL;
            case "bankedEmbers" -> Skills.BANKED_EMBERS;
            case "shiftFace" -> Skills.SHIFT_FACE;
            case "crossroadsTorches" -> Skills.CROSSROADS_TORCHES;
            case "spectralHex" -> Skills.SPECTRAL_HEX;
            case "witchingHour" -> Skills.WITCHING_HOUR;
            case "tripleGoddess" -> Skills.TRIPLE_GODDESS;
            case "releaseAnima" -> Skills.RELEASE_ANIMA;
            case "soulThread" -> Skills.SOUL_THREAD;
            case "soulLance" -> Skills.SOUL_LANCE;
            case "chrysalis" -> Skills.CHRYSALIS;
            case "iridescentSoul" -> Skills.IRIDESCENT_SOUL;
            case "doorway" -> Skills.DOORWAY;
            case "reversal" -> Skills.REVERSAL;
            case "twoFacedStrike" -> Skills.TWO_FACED_STRIKE;
            case "trespass" -> Skills.TRESPASS;
            case "godOfTransitions" -> Skills.GOD_OF_TRANSITIONS;
            case "irisFlight" -> Skills.IRIS_FLIGHT;
            case "spectrumShift" -> Skills.SPECTRUM_SHIFT;
            case "refractionLance" -> Skills.REFRACTION_LANCE;
            case "chromaticBurst" -> Skills.CHROMATIC_BURST;
            case "prism" -> Skills.PRISM;
            case "reel" -> Skills.REEL;
            case "castChains" -> Skills.CAST_CHAINS;
            case "tarnishedLash" -> Skills.TARNISHED_LASH;
            case "cinchTheChains" -> Skills.CINCH_THE_CHAINS;
            case "sharedFate" -> Skills.SHARED_FATE;
            case "dreamdrift" -> Skills.DREAMDRIFT;
            case "lullaby" -> Skills.LULLABY;
            case "nightmare" -> Skills.NIGHTMARE;
            case "veilOfSomnus" -> Skills.VEIL_OF_SOMNUS;
            case "sandman" -> Skills.SANDMAN;
            case "brace" -> Skills.BRACE;
            case "scalesOfBalance" -> Skills.SCALES_OF_BALANCE;
            case "collectTheDebt" -> Skills.COLLECT_THE_DEBT;
            case "markOfHubris" -> Skills.MARK_OF_HUBRIS;
            case "ledgerOfWrongs" -> Skills.LEDGER_OF_WRONGS;
            case "descent" -> Skills.DESCENT;
            case "bloom" -> Skills.BLOOM;
            case "wither" -> Skills.WITHER;
            case "queensDecree" -> Skills.QUEENS_DECREE;
            case "seedsOfTheUnderworld" -> Skills.SEEDS_OF_THE_UNDERWORLD;
            case "shroudOfLetus" -> Skills.SHROUD_OF_LETUS;
            case "knell" -> Skills.KNELL;
            case "scytheOfLetus" -> Skills.SCYTHE_OF_LETUS;
            case "tollTheBell" -> Skills.TOLL_THE_BELL;
            case "theInevitable" -> Skills.THE_INEVITABLE;
            default -> Skills.DEFAULT;
        };
    }

    public static void cooldownAlarm(Player player, long cooldownTime, String abilityName) {
        long cooldownTimeInTicks = (cooldownTime / 50);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendActionBar(ChatColor.YELLOW + abilityName + " is no longer on cooldown!");
                }
            }
        }.runTaskLater(Minerva.getInstance(), cooldownTimeInTicks);
    }

    public static void onCooldown(Player player) {
        player.sendMessage(ChatColor.RED + "That ability is currently on cooldown.");
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
    }

    public static void skillLocked(Player player, String reason) {
        player.sendMessage(ChatColor.RED + "That skill is currently locked because " + reason + ".");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
    }

    public static void stun(Player inflictor, Entity entity, long stunTicks) {
        if (entity.hasMetadata("NPC")) return;

        if (PlayerStats.isSummoned(inflictor, entity)) return;

        Location tpLoc = entity.getLocation();

        if (entity instanceof Player player && Party.isPlayerInPlayerParty(inflictor, player)) {
            return;
        }

        if (entity instanceof Player player) {
            player.sendActionBar(Component.text("STUNNED!", TextColor.color(255,223,62)));
        }

        Color[] gradient = {
                Color.fromRGB(255,249,221),
                Color.fromRGB(255,246,198),
                Color.fromRGB(255,239,156),
                Color.fromRGB(255,223,62),
                Color.fromRGB(255,213,0)
        };

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= stunTicks || (entity instanceof LivingEntity lE && lE.getHealth() <= 0.1)) {
                    this.cancel();
                    return;
                }

                if (ticks % 3 == 0 && ticks < stunTicks - 2L) {
                    Location loc = entity.getLocation().clone().add(new Vector(0, 2.5, 0));
                    for (Vector vec : ParticleUtils.getCirclePoints(0.5)) {
                        Location particleLoc = loc.clone().add(vec);
                        loc.getWorld().spawnParticle(Particle.DUST, particleLoc, 0, 0, 0, 0, 0, ParticleUtils.getDustOptionsFromGradient(gradient, 1f));
                    }
                }

                tpLoc.setDirection(entity.getLocation().getDirection());
                entity.teleport(tpLoc);
                ticks++;
            }
        }.runTaskTimer(Minerva.getInstance(), 0L, 1L);
    }

    public abstract void cast(Player player, CooldownManager cooldownManager, int level);

    public abstract String getLevelDescription(int level);

    public abstract String toString();

    public abstract ItemStack getItem();

    public static void damage(LivingEntity livingEntity, double damage, Player damager) {
        if (livingEntity instanceof Player player && (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE)) {
            return;
        }

        if (livingEntity instanceof Player && !livingEntity.getWorld().getPVP()) {
            return;
        }

        if (PlayerStats.isSummoned(damager, livingEntity)) return;

        if (damager.getScoreboardTags().contains("charmed") && damager.getScoreboardTags().contains(livingEntity.getUniqueId().toString())) {
            return;
        }

        double magicDamage = 0;
        double magicResist = 0;
        if (MMOPlayerData.isLoaded(damager.getUniqueId())) {
            magicDamage = MMOPlayerData.get(damager.getUniqueId()).getStatMap().getStat("MAGIC_DAMAGE");
        }

        if (MMOPlayerData.isLoaded(livingEntity.getUniqueId())) {
            magicResist = MMOPlayerData.get(livingEntity.getUniqueId()).getStatMap().getStat("MAGIC_DAMAGE_REDUCTION");
        }

        damage *= (magicDamage + 100)/100;
        damage *= (100 - magicResist)/100;

        if (livingEntity.getNoDamageTicks() > 0) return;

        if (livingEntity.getHealth()-damage <= 0) {
            if (livingEntity.isDead()) {
                return;
            }
            livingEntity.setHealth(0);
            livingEntity.setKiller(damager);
            if (livingEntity instanceof Player p) {
                Bukkit.getPluginManager().callEvent(new PlayerDeathEvent(p, DamageSource.builder(DamageType.MAGIC).build(), Arrays.asList(p.getInventory().getStorageContents()), 0, Component.text(p.getName() + " was killed by " + damager.getName() + "'s heritage skill."), false));
            }
        } else {
            livingEntity.setHealth(livingEntity.getHealth() - damage);
        }
        livingEntity.damage(0, damager);

        DamageIndicators indicators = new DamageIndicators(MythicLib.plugin.getConfig().getConfigurationSection("game-indicators.damage"));
        DamageMetadata damageFinal = new DamageMetadata(damage, io.lumine.mythic.lib.damage.DamageType.MAGIC);
        AttackMetadata attack = new AttackMetadata(damageFinal, livingEntity, StatProvider.get(damager));

        indicators.displayIndicators(new AttackUnregisteredEvent(
                new EntityDamageEvent(
                        livingEntity, EntityDamageEvent.DamageCause.MAGIC, DamageSource.builder(DamageType.MAGIC).withDirectEntity(livingEntity).withCausingEntity(damager).build(),
                        damage
                ),
                attack
        ));
    }

    public static void damage(LivingEntity livingEntity, double damage, Player damager, boolean ignoreInvulnTicks, boolean addStr) {
        // if (livingEntity.hasMetadata("NPC")) return;

        if (livingEntity instanceof Player player && (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE)) {
            return;
        }

        if (livingEntity instanceof Player && !livingEntity.getWorld().getPVP() || livingEntity.isInvulnerable()) {
            return;
        }

        if (livingEntity instanceof Tameable) {
            if (((Tameable) livingEntity).getOwner() != null) {
                return;
            }
        }

        double magicDamage = 0;
        double magicResist = 0;
        if (MMOPlayerData.isLoaded(damager.getUniqueId())) {
            magicDamage = MMOPlayerData.get(damager.getUniqueId()).getStatMap().getStat("MAGIC_DAMAGE");
        }

        if (MMOPlayerData.isLoaded(livingEntity.getUniqueId())) {
            magicResist = MMOPlayerData.get(livingEntity.getUniqueId()).getStatMap().getStat("MAGIC_DAMAGE_REDUCTION");
        }

        if (damager.hasPotionEffect(PotionEffectType.STRENGTH) && addStr) {
            damage += 3 * (damager.getPotionEffect(PotionEffectType.STRENGTH).getAmplifier() + 1);
        }



        damage *= (magicDamage + 100)/100;
        damage *= (100 - magicResist)/100;

        if (livingEntity.getNoDamageTicks() > 0 && !ignoreInvulnTicks) return;

        if (livingEntity.getHealth()-damage <= 0) {
            if (livingEntity.isDead()) {
                return;
            }
            livingEntity.setHealth(0);
            livingEntity.setKiller(damager);
            if (livingEntity instanceof Player p) {
                Bukkit.getPluginManager().callEvent(new PlayerDeathEvent(p, DamageSource.builder(DamageType.MAGIC).build(), Arrays.asList(p.getInventory().getStorageContents()), 0, Component.text(p.getName() + " was killed by " + damager.getName() + "'s heritage skill."), false));
            }
        } else {
            livingEntity.setHealth(livingEntity.getHealth() - damage);
        }
        livingEntity.damage(0, damager);

        DamageIndicators indicators = new DamageIndicators(MythicLib.plugin.getConfig().getConfigurationSection("game-indicators.damage"));
        DamageMetadata damageFinal = new DamageMetadata(damage, io.lumine.mythic.lib.damage.DamageType.MAGIC);
        AttackMetadata attack = new AttackMetadata(damageFinal, livingEntity, StatProvider.get(damager));

        indicators.displayIndicators(new AttackUnregisteredEvent(
                new EntityDamageEvent(
                        livingEntity, EntityDamageEvent.DamageCause.MAGIC, DamageSource.builder(DamageType.MAGIC).withDirectEntity(livingEntity).withCausingEntity(damager).build(),
                        damage
                ),
                attack
        ));
    }

    public static void knockback(Entity entity, Vector kb) {
        if (entity.hasMetadata("NPC")) return;

        if (entity instanceof Player player && (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE)) {
            return;
        }

        if (entity instanceof Player && !entity.getWorld().getPVP()) {
            return;
        }

        entity.setVelocity(kb);
    }

    public static void stack(Player player, String ability, int increment, String abilityFormal, long timeUntilExpires) {
        if (player.hasMetadata("NPC")) return;
        Map<String, Integer> stackingAbilities = PlayerStats.getStats(player.getUniqueId()).getStackingAbilities();
        int newStacks = getStacks(player, ability) + increment;
        int maxStack = switch (ability) {
            case "smolder" -> ((Smolder) Skills.SMOLDER).stackSmolder(player, newStacks, timeUntilExpires);
            case "doves" -> ((Doves) Skills.DOVES).stackDoves(player, newStacks, timeUntilExpires);
            case "hermesMomentum" -> ((FleetFootwork) Skills.FLEET_FOOTWORK).stackMomentum(player, newStacks, timeUntilExpires);
            default -> 99;
        };
        if (maxStack < newStacks) newStacks = maxStack;

        if (newStacks <= 0) {
            stackingAbilities.remove(ability);
            Minerva.getInstance().getCdInstance().setCooldownFromNow(player.getUniqueId(), ability, 0L);
            return;
        }

        stackingAbilities.put(ability, newStacks);
        if (player.isOnline()) {
            player.sendActionBar(ChatColor.YELLOW + abilityFormal + " Stacks: " + newStacks);
        }

        Minerva.getInstance().getCdInstance().setCooldownFromNow(player.getUniqueId(), ability, timeUntilExpires);
    }

    public static int getStacks(Player player, String ability) {
        if (player.hasMetadata("NPC")) return 0;
        Map<String, Integer> stackingAbilities = PlayerStats.getStats(player.getUniqueId()).getStackingAbilities();
        if (Minerva.getInstance().getCdInstance().isCooldownDone(player.getUniqueId(), ability)) {
            stackingAbilities.remove(ability);
            return 0;
        } else {
            return stackingAbilities.getOrDefault(ability, 0);
        }
    }

    // ===================================================================================
    // Shared soft-CC: SLEEP and FEAR (used by Hypnos, Hecate, Arke, Bellona, etc.)
    // Sleep: the target is slumped and cannot act; it BREAKS instantly on damage.
    // Fear:  the target is forced to flee from the source and cannot attack.
    // Both are friendly-fire safe and obey the no-strobe particle rule.
    // ===================================================================================
    public static final Map<UUID, BukkitRunnable> SLEEP_TASKS = new HashMap<>();
    public static final Map<UUID, BukkitRunnable> FEAR_TASKS = new HashMap<>();

    public static boolean isAsleep(Entity e) {
        return e != null && e.getScoreboardTags().contains("asleep");
    }

    public static boolean isFeared(Entity e) {
        return e != null && e.getScoreboardTags().contains("feared");
    }

    private static boolean cannotSoftCC(Player inflictor, LivingEntity target) {
        if (target.hasMetadata("NPC")) return true;
        if (PlayerStats.isSummoned(inflictor, target)) return true;
        if (target instanceof Player p) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) return true;
            if (Party.isPlayerInPlayerParty(inflictor, p)) return true;
        }
        return false;
    }

    public static void sleep(Player inflictor, LivingEntity target, long sleepTicks) {
        if (cannotSoftCC(inflictor, target)) return;
        wake(target);
        target.addScoreboardTag("asleep");
        if (target instanceof Mob mob) mob.setTarget(null);
        if (target instanceof Player p) {
            p.sendActionBar(Component.text("ASLEEP", TextColor.color(150, 170, 255)));
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) sleepTicks, 6));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, (int) sleepTicks, 3));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (int) sleepTicks, 2));

        Color[] dream = {Color.fromRGB(120, 150, 255), Color.fromRGB(150, 170, 255), Color.fromRGB(95, 120, 220)};
        BukkitRunnable task = new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= sleepTicks || target.isDead() || !isAsleep(target)) {
                    wake(target);
                    cancel();
                    return;
                }
                Vector v = target.getVelocity();
                target.setVelocity(new Vector(0, Math.min(0, v.getY()), 0));
                if (target instanceof Mob mob) mob.setTarget(null);
                if (t % 6 == 0) {
                    Location loc = target.getLocation().clone().add(0, target.getHeight() + 0.4, 0);
                    target.getWorld().spawnParticle(Particle.DUST, loc, 4, 0.25, 0.12, 0.25, 0,
                            new Particle.DustOptions(dream[(int) (Math.random() * dream.length)], 1f));
                }
                t++;
            }
        };
        task.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        SLEEP_TASKS.put(target.getUniqueId(), task);
    }

    public static void wake(Entity target) {
        if (target == null) return;
        target.removeScoreboardTag("asleep");
        BukkitRunnable r = SLEEP_TASKS.remove(target.getUniqueId());
        if (r != null) {
            try { r.cancel(); } catch (IllegalStateException ignored) {}
        }
    }

    public static void fear(Player inflictor, LivingEntity target, long fearTicks) {
        if (cannotSoftCC(inflictor, target)) return;
        unfear(target);
        target.addScoreboardTag("feared");
        if (target instanceof Player p) {
            p.sendActionBar(Component.text("FEARED", TextColor.color(150, 40, 160)));
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) fearTicks, 1));
        final Location source = inflictor.getLocation().clone();
        Color[] dread = {Color.fromRGB(90, 30, 110), Color.fromRGB(60, 20, 80), Color.fromRGB(120, 50, 140)};
        BukkitRunnable task = new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= fearTicks || target.isDead() || !isFeared(target)) {
                    unfear(target);
                    cancel();
                    return;
                }
                Vector away = target.getLocation().toVector().subtract(source.toVector()).setY(0);
                if (away.lengthSquared() > 0.04) {
                    away.normalize().multiply(0.34);
                    away.setY(Math.max(target.getVelocity().getY(), -0.2));
                    target.setVelocity(away);
                }
                if (target instanceof Mob mob) mob.setTarget(null);
                if (t % 5 == 0) {
                    Location loc = target.getLocation().clone().add(0, target.getHeight() + 0.3, 0);
                    target.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.25, 0.2, 0.25, 0,
                            new Particle.DustOptions(dread[(int) (Math.random() * dread.length)], 1f));
                }
                t++;
            }
        };
        task.runTaskTimer(Minerva.getInstance(), 0L, 1L);
        FEAR_TASKS.put(target.getUniqueId(), task);
    }

    public static void unfear(Entity target) {
        if (target == null) return;
        target.removeScoreboardTag("feared");
        BukkitRunnable r = FEAR_TASKS.remove(target.getUniqueId());
        if (r != null) {
            try { r.cancel(); } catch (IllegalStateException ignored) {}
        }
    }
}
