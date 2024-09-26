package net.minervamc.minerva.guis;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.world.item.SplashPotionItem;
import net.minervamc.minerva.lib.menu.Menu;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.lib.util.MenuUtil;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CTFKitGUI extends Menu {
    private final int scoutIndex = 11;
    private final int attackerIndex = 13;
    private final int defenderIndex = 15;

    public CTFKitGUI() {
        super(27, Component.text("Kit Selection"));
        MenuUtil.fill(getInventory(), ItemCreator.createNameless(Material.BLACK_STAINED_GLASS_PANE));
        setItem(scoutIndex, getScout(), (p, event) -> {
            giveScoutKit(p);
            close(p);
        });
        setItem(attackerIndex, getAttacker(), (p, event) -> {
            giveAttackerKit(p);
            close(p);
        });
        setItem(defenderIndex, getDefender(), (p, event) -> {
            giveDefenderKit(p);
            close(p);
        });
    }

    private ItemStack getScout() {
        return ItemCreator.get(Material.LEATHER_BOOTS)
                .setName(TextContext.formatLegacy("&bScout", false))
                .setLore(List.of(
                        TextContext.formatLegacy("&7Using their speed", false),
                        TextContext.formatLegacy("&7and informative items,", false),
                        TextContext.formatLegacy("&7they can gather critical", false),
                        TextContext.formatLegacy("&7information about the", false),
                        TextContext.formatLegacy("&7enemy and play far from the", false),
                        TextContext.formatLegacy("&7fight, on enemy flank turf.", false)
                ))
                .addAttribute(Attribute.GENERIC_ARMOR, 7.0, AttributeModifier.Operation.ADD_NUMBER)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .build();
    }

    private ItemStack getAttacker() {
        return ItemCreator.get(Material.WOODEN_SWORD)
                .setName(TextContext.formatLegacy("&cAttacker", false))
                .setLore(List.of(
                        TextContext.formatLegacy("&7Equipped with extra", false),
                        TextContext.formatLegacy("&7offensive utilities,", false),
                        TextContext.formatLegacy("&7an attacker kit is", false),
                        TextContext.formatLegacy("&7used to attack the", false),
                        TextContext.formatLegacy("&7enemy, steal the flag,", false),
                        TextContext.formatLegacy("&7and is crucial to win.", false)
                ))
                .addAttribute(Attribute.GENERIC_ATTACK_DAMAGE, 7.0, AttributeModifier.Operation.ADD_NUMBER)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .build();
    }

    private ItemStack getDefender() {
        return ItemCreator.get(Material.SHIELD)
                .setName(TextContext.formatLegacy("&6Defender", false))
                .setLore(List.of(
                        TextContext.formatLegacy("&7Using their protective", false),
                        TextContext.formatLegacy("&7utilities such as blocks", false),
                        TextContext.formatLegacy("&7to place, shields, traps,", false),
                        TextContext.formatLegacy("&7and more, the defender", false),
                        TextContext.formatLegacy("&7kit is one of the best", false),
                        TextContext.formatLegacy("&7kits for a team.", false)
                ))
                .build();
    }

    private void giveScoutKit(Player player) {
        int swordLoc = 27;
        ItemCreator sword = ItemCreator.get(Material.GOLDEN_SWORD);
        sword.setName(Component.text("Celestial Bronze Dagger", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sword.setUnbreakable(true);

        int bowLoc = 28;
        ItemCreator bow = ItemCreator.get(Material.BOW);
        bow.setName(Component.text("Reinforced Bow", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        bow.setUnbreakable(true);
        bow.addEnchantment(Enchantment.POWER, 2);
        bow.addEnchantment(Enchantment.INFINITY, 1);

        int trapLoc = 29;
        int trapAmount = 4;
        ItemCreator trap = ItemCreator.get(Material.STONE_PRESSURE_PLATE);
        trap.setName(Component.text("Weakened Land Mine Trap", NamedTextColor.GRAY).decorate(TextDecoration.BOLD));
        ItemStack trapBuilt = trap.build();
        trapBuilt.setAmount(trapAmount);

        int foodLoc = 30;
        int foodAmount = 64;
        ItemCreator food = ItemCreator.get(Material.GOLDEN_CARROT);
        food.setName(Component.text("Ambrosia-Infused Carrot", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        ItemStack foodBuilt = food.build();
        foodBuilt.setAmount(foodAmount);

        int potLoc = 31;
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = ((PotionMeta) potion.getItemMeta());
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 2), false);
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1200, 1), false);
        potionMeta.setItemName(ChatColor.AQUA + "" + ChatColor.BOLD + "Stealth Enhancing Potion");
        potionMeta.setColor(Color.AQUA);
        potion.setItemMeta(potionMeta);

        int pearlLoc = 34;
        int pearlAmount = 4;
        ItemCreator pearl = ItemCreator.get(Material.ENDER_PEARL);
        pearl.setName(Component.text("Pearl of the Gods", NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        ItemStack pearlBuilt = pearl.build();
        pearlBuilt.setAmount(pearlAmount);

        int spyglassLoc = 35;
        ItemCreator spyglass = ItemCreator.get(Material.SPYGLASS);

        int arrowLoc = 0;
        ItemStack arrow = new ItemStack(Material.ARROW);

        player.getInventory().setItem(swordLoc, sword.build());
        player.getInventory().setItem(bowLoc, bow.build());
        player.getInventory().setItem(trapLoc, trapBuilt);
        player.getInventory().setItem(foodLoc, foodBuilt);
        player.getInventory().setItem(potLoc, potion);
        player.getInventory().setItem(pearlLoc, pearlBuilt);
        player.getInventory().setItem(spyglassLoc, spyglass.build());
        player.getInventory().setItem(arrowLoc, arrow);
    }

    private void giveAttackerKit(Player player) {
        int swordLoc = 27;
        ItemCreator sword = ItemCreator.get(Material.GOLDEN_SWORD);
        sword.setName(Component.text("Celestial Bronze Dagger", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sword.setUnbreakable(true);

        int bowLoc = 28;
        ItemCreator bow = ItemCreator.get(Material.BOW);
        bow.setName(Component.text("Reinforced Bow", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        bow.setUnbreakable(true);
        bow.addEnchantment(Enchantment.POWER, 2);
        bow.addEnchantment(Enchantment.INFINITY, 1);

        int trapLoc = 29;
        int trapAmount = 4;
        ItemCreator trap = ItemCreator.get(Material.STONE_PRESSURE_PLATE);
        trap.setName(Component.text("Weakened Land Mine Trap", NamedTextColor.GRAY).decorate(TextDecoration.BOLD));
        ItemStack trapBuilt = trap.build();
        trapBuilt.setAmount(trapAmount);

        int foodLoc = 30;
        int foodAmount = 64;
        ItemCreator food = ItemCreator.get(Material.GOLDEN_CARROT);
        food.setName(Component.text("Ambrosia-Infused Carrot", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        ItemStack foodBuilt = food.build();
        foodBuilt.setAmount(foodAmount);

        int potLoc = 31;
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = ((PotionMeta) potion.getItemMeta());
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 2), false);
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1200, 1), false);
        potionMeta.setItemName(ChatColor.AQUA + "" + ChatColor.BOLD + "Stealth Enhancing Potion");
        potionMeta.setColor(Color.AQUA);
        potion.setItemMeta(potionMeta);

        int pearlLoc = 34;
        int pearlAmount = 4;
        ItemCreator pearl = ItemCreator.get(Material.ENDER_PEARL);
        pearl.setName(Component.text("Pearl of the Gods", NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        ItemStack pearlBuilt = pearl.build();
        pearlBuilt.setAmount(pearlAmount);

        int spyglassLoc = 35;
        ItemCreator spyglass = ItemCreator.get(Material.SPYGLASS);

        int arrowLoc = 0;
        ItemStack arrow = new ItemStack(Material.ARROW);
        arrow.setAmount(64);

        player.getInventory().setItem(swordLoc, sword.build());
        player.getInventory().setItem(bowLoc, bow.build());
        player.getInventory().setItem(trapLoc, trapBuilt);
        player.getInventory().setItem(foodLoc, foodBuilt);
        player.getInventory().setItem(potLoc, potion);
        player.getInventory().setItem(pearlLoc, pearlBuilt);
        player.getInventory().setItem(spyglassLoc, spyglass.build());
        player.getInventory().setItem(arrowLoc, arrow);
    }

    private void giveDefenderKit(Player player) {
        int swordLoc = 27;
        ItemCreator sword = ItemCreator.get(Material.GOLDEN_SWORD);
        sword.setName(Component.text("Celestial Bronze Dagger", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sword.setUnbreakable(true);

        int bowLoc = 28;
        ItemCreator bow = ItemCreator.get(Material.BOW);
        bow.setName(Component.text("Reinforced Bow", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        bow.setUnbreakable(true);
        bow.addEnchantment(Enchantment.POWER, 2);
        bow.addEnchantment(Enchantment.INFINITY, 1);

        int trapLoc = 29;
        int trapAmount = 4;
        ItemCreator trap = ItemCreator.get(Material.STONE_PRESSURE_PLATE);
        trap.setName(Component.text("Weakened Land Mine Trap", NamedTextColor.GRAY).decorate(TextDecoration.BOLD));
        ItemStack trapBuilt = trap.build();
        trapBuilt.setAmount(trapAmount);

        int foodLoc = 30;
        int foodAmount = 64;
        ItemCreator food = ItemCreator.get(Material.GOLDEN_CARROT);
        food.setName(Component.text("Ambrosia-Infused Carrot", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        ItemStack foodBuilt = food.build();
        foodBuilt.setAmount(foodAmount);

        int potLoc = 31;
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = ((PotionMeta) potion.getItemMeta());
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 2), false);
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1200, 1), false);
        potionMeta.setItemName(ChatColor.AQUA + "" + ChatColor.BOLD + "Stealth Enhancing Potion");
        potionMeta.setColor(Color.AQUA);
        potion.setItemMeta(potionMeta);

        int pearlLoc = 34;
        int pearlAmount = 4;
        ItemCreator pearl = ItemCreator.get(Material.ENDER_PEARL);
        pearl.setName(Component.text("Pearl of the Gods", NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        ItemStack pearlBuilt = pearl.build();
        pearlBuilt.setAmount(pearlAmount);

        int spyglassLoc = 35;
        ItemCreator spyglass = ItemCreator.get(Material.SPYGLASS);

        int arrowLoc = 0;
        ItemStack arrow = new ItemStack(Material.ARROW);

        player.getInventory().setItem(swordLoc, sword.build());
        player.getInventory().setItem(bowLoc, bow.build());
        player.getInventory().setItem(trapLoc, trapBuilt);
        player.getInventory().setItem(foodLoc, foodBuilt);
        player.getInventory().setItem(potLoc, potion);
        player.getInventory().setItem(pearlLoc, pearlBuilt);
        player.getInventory().setItem(spyglassLoc, spyglass.build());
        player.getInventory().setItem(arrowLoc, arrow);
    }
}
