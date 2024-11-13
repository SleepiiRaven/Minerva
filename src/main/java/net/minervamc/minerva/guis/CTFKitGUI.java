package net.minervamc.minerva.guis;

import java.util.List;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minervamc.minerva.lib.menu.Menu;
import net.minervamc.minerva.lib.text.TextContext;
import net.minervamc.minerva.lib.util.ItemCreator;
import net.minervamc.minerva.lib.util.MenuUtil;
import net.minervamc.minerva.minigames.ctf.CaptureTheFlag;
import net.minervamc.minerva.utils.SkillUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
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
    private final Type tSword = MMOItems.plugin.getTypes().get("SWORD");
    private final Type tAxe = MMOItems.plugin.getTypes().get("GREATAXE");
    private final Type tCons = MMOItems.plugin.getTypes().get("CONSUMABLE");
    private final Type tBow = MMOItems.plugin.getTypes().get("BOW");
    private final Type tHelm = MMOItems.plugin.getTypes().get("HEADWEAR");
    private final Type tChest = MMOItems.plugin.getTypes().get("CHESTWEAR");
    private final Type tLegs = MMOItems.plugin.getTypes().get("LEGWEAR");
    private final Type tBoots = MMOItems.plugin.getTypes().get("FOOTWEAR");

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
                        TextContext.formatLegacy("&7direction to the opposing flag,", false),
                        TextContext.formatLegacy("&7or the direction to your own", false),
                        TextContext.formatLegacy("&7flag if the opposing flag", false),
                        TextContext.formatLegacy("&7has been taken.", false),
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
        ItemStack sword = MMOItems.plugin.getItem(tSword, "CTF_SCT_SWORD");
        assert sword != null;
        SkillUtils.setFocus(sword);

        ItemStack bow = MMOItems.plugin.getItem(tBow, "CTF_SCT_BOW");
        assert bow != null;
        SkillUtils.setFocus(bow);

        int trapAmount = 4;
        ItemCreator trap = ItemCreator.get(Material.STONE_PRESSURE_PLATE);
        trap.setName(Component.text("Weakened Land Mine Trap", NamedTextColor.GRAY).decorate(TextDecoration.BOLD));
        ItemStack trapBuilt = trap.build();
        trapBuilt.setAmount(trapAmount);

        ItemStack food = MMOItems.plugin.getItem(tCons, "SWEET_BERRIES");
        food.setAmount(64);

        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = ((PotionMeta) potion.getItemMeta());
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 240, 2), false);
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 240, 1), false);
        potionMeta.setItemName(ChatColor.AQUA + "" + ChatColor.BOLD + "Stealth Enhancing Potion");
        potionMeta.setColor(Color.AQUA);
        potion.setItemMeta(potionMeta);

        ItemCreator spyglass = ItemCreator.get(Material.SPYGLASS);

        ItemStack arrow = new ItemStack(Material.ARROW);

        player.getInventory().addItem(sword, bow, trapBuilt, food, potion, spyglass.build(), arrow);

        ItemStack chestplate = MMOItems.plugin.getItem(tChest, "CTF_SCT_CHEST");
        ItemStack leggings = MMOItems.plugin.getItem(tLegs, "CTF_SCT_LEGS");
        ItemStack boots = MMOItems.plugin.getItem(tBoots, "CTF_ARC_BOOTS");

        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }

    private void giveAttackerKit(Player player) {
        ItemStack sword = MMOItems.plugin.getItem(tSword, "CTF_ATT_SWORD");
        assert sword != null;
        SkillUtils.setFocus(sword);

        ItemStack food = MMOItems.plugin.getItem(tCons, "SWEET_BERRIES");
        food.setAmount(64);

        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = ((PotionMeta) potion.getItemMeta());
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 2), false);
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 2), false);
        potionMeta.setItemName(ChatColor.GOLD + "" + ChatColor.BOLD + "Pure Nectar");
        potionMeta.setColor(Color.ORANGE);
        potion.setItemMeta(potionMeta);

        player.getInventory().addItem(sword, food, potion);

        ItemStack chestplate = MMOItems.plugin.getItem(tChest, "CTF_ATT_CHEST");
        ItemStack leggings = MMOItems.plugin.getItem(tLegs, "CTF_ATT_LEGS");
        ItemStack boots = MMOItems.plugin.getItem(tBoots, "CTF_ATT_BOOTS");

        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }

    private void giveDefenderKit(Player player) {
        ItemStack axe = MMOItems.plugin.getItem(tAxe, "CTF_DEF_AXE");
        assert axe != null;
        SkillUtils.setFocus(axe);

        int trapAmount = 8;
        ItemCreator trap = ItemCreator.get(Material.STONE_PRESSURE_PLATE);
        trap.setName(Component.text("Weakened Land Mine Trap", NamedTextColor.GRAY).decorate(TextDecoration.BOLD));
        ItemStack trapBuilt = trap.build();
        trapBuilt.setAmount(trapAmount);

        ItemStack food = MMOItems.plugin.getItem(tCons, "SWEET_BERRIES");
        food.setAmount(64);

        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = ((PotionMeta) potion.getItemMeta());
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 2), false);
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1), false);
        potionMeta.setItemName(ChatColor.GRAY + "" + ChatColor.BOLD + "Resistance Enhancing Potion");
        potionMeta.setColor(Color.GRAY);
        potion.setItemMeta(potionMeta);

        ItemCreator blocks = ItemCreator.get(Material.BAMBOO_MOSAIC);
        ItemStack blocksBuilt = ItemCreator.getPlaceable(blocks.build()); // simply puts in none so it has can_place_on but no blocks in there
        blocksBuilt.setAmount(4);

        player.getInventory().addItem(axe, trapBuilt, food, potion, blocksBuilt);

        ItemStack chestplate = MMOItems.plugin.getItem(tChest, "CTF_DEF_CHEST");
        ItemStack leggings = MMOItems.plugin.getItem(tLegs, "CTF_DEF_LEGS");
        ItemStack boots = MMOItems.plugin.getItem(tBoots, "CTF_DEF_BOOTS");

        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        ItemStack totem = ItemCreator.create(Component.text("Patron's Boon"), Material.TOTEM_OF_UNDYING);
        player.getInventory().setItemInOffHand(totem);
    }
}
