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
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
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
            CaptureTheFlag.kitChoose(p, "scout");
            setStopClosing(false);
            close(p);
        });
        setItem(attackerIndex, getAttacker(), (p, event) -> {
            giveAttackerKit(p);
            CaptureTheFlag.kitChoose(p, "attacker");
            setStopClosing(false);
            close(p);
        });
        setItem(defenderIndex, getDefender(), (p, event) -> {
            giveDefenderKit(p);
            CaptureTheFlag.kitChoose(p, "defender");
            setStopClosing(false);
            close(p);
        });
        setStopClosing(true);
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
                        TextContext.formatLegacy("&7fight, on enemy flank turf.", false),
                        TextContext.formatLegacy("", false),
                        TextContext.formatLegacy("Ping (SNEAK + LEFT CLICK)", false).color(NamedTextColor.GOLD),
                        TextContext.formatLegacy("&7A trail of particles shows the", false),
                        TextContext.formatLegacy("&7direction to the opposing flag.", false),
                        TextContext.formatLegacy("&7Cooldown: 20 seconds.", false)
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
                        TextContext.formatLegacy("&7damaging weapons and,", false),
                        TextContext.formatLegacy("&7high armor, the attacker", false),
                        TextContext.formatLegacy("&7kit is used to quickly steal", false),
                        TextContext.formatLegacy("&7the flag, and is crucial", false),
                        TextContext.formatLegacy("&7to any team.", false),
                        TextContext.formatLegacy("", false),
                        TextContext.formatLegacy("Leap (SNEAK + LEFT CLICK)", false).color(NamedTextColor.GOLD),
                        TextContext.formatLegacy("&7Jump up and forward in a swift", false),
                        TextContext.formatLegacy("&7manner without taking fall damage.", false),
                        TextContext.formatLegacy("&7Cooldown: 6 seconds", false)
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
                        TextContext.formatLegacy("&7kits for a team.", false),
                        TextContext.formatLegacy("", false),
                        TextContext.formatLegacy("Parry (SNEAK + LEFT CLICK)", false).color(NamedTextColor.GOLD),
                        TextContext.formatLegacy("&7Block all incoming damage", false),
                        TextContext.formatLegacy("&7for 1 second, dealing half", false),
                        TextContext.formatLegacy("&7back to the perpetrator", false),
                        TextContext.formatLegacy("&7Cooldown: 15 seconds", false)
                ))
                .build();
    }

    private void giveScoutKit(Player player) {
        ItemCreator sword = ItemCreator.get(Material.GOLDEN_SWORD);
        sword.setName(Component.text("Celestial Bronze Dagger", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sword.setUnbreakable(true);

        ItemCreator bow = ItemCreator.get(Material.BOW);
        bow.setName(Component.text("Reinforced Bow", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        bow.setUnbreakable(true);
        bow.addEnchantment(Enchantment.POWER, 2);
        bow.addEnchantment(Enchantment.INFINITY, 1);

        int trapAmount = 4;
        ItemCreator trap = ItemCreator.get(Material.STONE_PRESSURE_PLATE);
        trap.setName(Component.text("Weakened Land Mine Trap", NamedTextColor.GRAY).decorate(TextDecoration.BOLD));
        ItemStack trapBuilt = trap.build();
        trapBuilt.setAmount(trapAmount);

        int foodAmount = 64;
        ItemCreator food = ItemCreator.get(Material.GOLDEN_CARROT);
        food.setName(Component.text("Ambrosia-Infused Carrot", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        ItemStack foodBuilt = food.build();
        foodBuilt.setAmount(foodAmount);

        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = ((PotionMeta) potion.getItemMeta());
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 240, 2), false);
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 240, 1), false);
        potionMeta.setItemName(ChatColor.AQUA + "" + ChatColor.BOLD + "Stealth Enhancing Potion");
        potionMeta.setColor(Color.AQUA);
        potion.setItemMeta(potionMeta);

        ItemCreator spyglass = ItemCreator.get(Material.SPYGLASS);

        ItemStack arrow = new ItemStack(Material.ARROW);

        player.getInventory().addItem(sword.build(), bow.build(), trapBuilt, foodBuilt, potion, spyglass.build(), arrow);

        ItemStack chestplate = ItemCreator.create(Material.CHAINMAIL_CHESTPLATE);
        ItemStack leggings = ItemCreator.create(Material.CHAINMAIL_LEGGINGS);
        ItemStack boots = ItemCreator.create(Material.IRON_BOOTS);

        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }

    private void giveAttackerKit(Player player) {
        ItemCreator sword = ItemCreator.get(Material.DIAMOND_SWORD);
        sword.setName(Component.text("Diamond-Infused Spatha", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sword.setUnbreakable(true);

        int foodAmount = 64;
        ItemCreator food = ItemCreator.get(Material.GOLDEN_CARROT);
        food.setName(Component.text("Ambrosia-Infused Carrot", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        ItemStack foodBuilt = food.build();
        foodBuilt.setAmount(foodAmount);

        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = ((PotionMeta) potion.getItemMeta());
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 2), false);
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 2), false);
        potionMeta.setItemName(ChatColor.GOLD + "" + ChatColor.BOLD + "Pure Nectar");
        potionMeta.setColor(Color.ORANGE);
        potion.setItemMeta(potionMeta);

        player.getInventory().addItem(sword.build(), foodBuilt, potion);

        ItemStack chestplate = ItemCreator.create(Material.DIAMOND_CHESTPLATE);
        ItemStack leggings = ItemCreator.create(Material.DIAMOND_LEGGINGS);
        ItemStack boots = ItemCreator.create(Material.DIAMOND_BOOTS);

        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }

    private void giveDefenderKit(Player player) {
        ItemCreator sword = ItemCreator.get(Material.WOODEN_SWORD);
        sword.setName(Component.text("Training Sword", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sword.setUnbreakable(true);

        int trapAmount = 8;
        ItemCreator trap = ItemCreator.get(Material.STONE_PRESSURE_PLATE);
        trap.setName(Component.text("Weakened Land Mine Trap", NamedTextColor.GRAY).decorate(TextDecoration.BOLD));
        ItemStack trapBuilt = trap.build();
        trapBuilt.setAmount(trapAmount);

        int foodAmount = 64;
        ItemCreator food = ItemCreator.get(Material.GOLDEN_CARROT);
        food.setName(Component.text("Ambrosia-Infused Carrot", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        ItemStack foodBuilt = food.build();
        foodBuilt.setAmount(foodAmount);

        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = ((PotionMeta) potion.getItemMeta());
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 2), false);
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1), false);
        potionMeta.setItemName(ChatColor.GRAY + "" + ChatColor.BOLD + "Resistance Enhancing Potion");
        potionMeta.setColor(Color.GRAY);
        potion.setItemMeta(potionMeta);

        ItemCreator blocks = ItemCreator.get(Material.BAMBOO_MOSAIC);
        ItemStack blocksBuilt = ItemCreator.getPlaceable(blocks.build()); // simply puts in none so it has can_place_on but no blocks in there
        blocksBuilt.setAmount(8);

        player.getInventory().addItem(sword.build(), trapBuilt, foodBuilt, potion, blocksBuilt);

        ItemStack chestplate = ItemCreator.create(Material.CHAINMAIL_CHESTPLATE);
        ItemStack leggings = ItemCreator.create(Material.CHAINMAIL_LEGGINGS);
        ItemStack boots = ItemCreator.create(Material.IRON_BOOTS);

        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        ItemStack totem = ItemCreator.create(Component.text("Patron's Boon"), Material.TOTEM_OF_UNDYING);
        player.getInventory().setItemInOffHand(totem);
    }
}
