package net.minervamc.minerva.skills;

import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.PlayerStats;
import net.minervamc.minerva.skills.greek.hecate.TripleGoddess;
import net.minervamc.minerva.skills.greek.hestia.BankedEmbers;
import net.minervamc.minerva.skills.greek.nemesis.LedgerOfWrongs;
import net.minervamc.minerva.skills.greek.persephone.SeedsOfTheUnderworld;
import net.minervamc.minerva.types.Skill;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Drives the once-per-second passive engines for the new heritages whose resource accrues over
 * time (Hestia Embers, Persephone Seeds, Nemesis Ledger decay, Hecate Face auras). Started once
 * from {@link Minerva#onEnable()}.
 */
public final class HeritageTick {
    private HeritageTick() {}

    public static void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasMetadata("NPC")) continue;
                    PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
                    if (stats == null || !stats.getPassiveActive()) continue;
                    Skill passive = stats.getPassive();
                    int level = stats.getPassiveLevel();
                    try {
                        if (passive == Skills.BANKED_EMBERS) {
                            BankedEmbers.tick(player, level);
                        } else if (passive == Skills.SEEDS_OF_THE_UNDERWORLD) {
                            SeedsOfTheUnderworld.tick(player, level);
                        } else if (passive == Skills.LEDGER_OF_WRONGS) {
                            LedgerOfWrongs.tick(player, level);
                        } else if (passive == Skills.TRIPLE_GODDESS) {
                            TripleGoddess.tick(player, level);
                        }
                    } catch (Exception e) {
                        Minerva.getInstance().getSLF4JLogger().warn("HeritageTick error for " + player.getName() + ": " + e.getMessage());
                    }
                }
            }
        }.runTaskTimer(Minerva.getInstance(), 20L, 20L);
    }
}
