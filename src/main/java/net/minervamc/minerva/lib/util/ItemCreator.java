package net.minervamc.minerva.lib.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"deprecation", "unused"})
public class ItemCreator {
    private ItemStack item;
    private ItemMeta meta;

    private ItemCreator(){}

    public static ItemStack create(Component name, Material material) {
        return get(material).setName(name).build();
    }
    public static ItemStack create(Material material) {
        return get(material).build();
    }
    public static ItemStack createNameless(Material material) {
        return get(material).setName(Component.text("")).build();
    }

    public static ItemCreator get(Material material) {
        ItemCreator creator = new ItemCreator();
        creator.item = new ItemStack(material);
        creator.meta = creator.item.getItemMeta();
        return creator;
    }

    public static ItemCreator get(ItemStack item) {
        ItemCreator creator = new ItemCreator();
        creator.item = item.clone();
        creator.meta = item.getItemMeta();
        return creator;
    }

    public static ItemStack getHead(Player player) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta playerHeadMeta = (SkullMeta) playerHead.getItemMeta();
        playerHeadMeta.setOwner(player.getName());
        playerHead.setItemMeta(playerHeadMeta);
        return playerHead;
    }

    public static ItemStack getHead(OfflinePlayer player) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta playerHeadMeta = (SkullMeta) playerHead.getItemMeta();
        playerHeadMeta.setOwner(player.getName());
        playerHead.setItemMeta(playerHeadMeta);
        return playerHead;
    }

    public static ItemStack getSkullFromUrl(URL url){
        UUID uuid = UUID.randomUUID();
        PlayerProfile profile = Bukkit.createProfile(uuid, "");
        PlayerTextures textures = profile.getTextures();

        textures.setSkin(url);
        profile.setTextures(textures);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        skullMeta.setPlayerProfile(profile);
        head.setItemMeta(skullMeta);
        return head;
    }

    public static URL getURL(String textureUrl) {
        textureUrl = "http://textures.minecraft.net/texture/" + textureUrl;
        URL url;
        try {
            url = URI.create(textureUrl).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }

    public ItemCreator setName(Component name) {
        meta.itemName(name);
        return this;
    }
    public ItemCreator setLore(List<Component> lore) {
        meta.lore(lore);
        return this;
    }
    public ItemCreator addEnchantment(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }
    public ItemCreator addItemFlags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }
    public ItemCreator addAttribute(Attribute attribute, double amount, AttributeModifier.Operation operation) {
        AttributeModifier mod = new AttributeModifier(UUID.randomUUID(), attribute.name(), amount, operation);
        meta.addAttributeModifier(attribute, mod);
        return this;
    }
    public static ItemStack getPlaceable(ItemStack item, Material material) {
        String block = "minecraft:" + material.name().toLowerCase();

        NBT.modifyComponents(item, nbt -> {
            nbt.getOrCreateCompound("can_place_on").setString("blocks", block);
        });
        return item;
    }
    public static ItemStack getBreakable(ItemStack item, Material material) {
        String block = "minecraft:" + material.name().toLowerCase();

        NBT.modifyComponents(item, nbt -> {
            nbt.getOrCreateCompound("can_break").setString("blocks", block);
        });

        return item;
    }
    public ItemCreator setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }
    public ItemCreator setCustomModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }
    public <T> ItemCreator setPDC(JavaPlugin plugin, String key, PersistentDataType<T, T> type, T value) {
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        meta.getPersistentDataContainer().set(namespacedKey, type, value);
        return this;
    }
    public <T> T getPDC(JavaPlugin plugin, String key, PersistentDataType<T, T> type) {
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().get(namespacedKey, type);
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
