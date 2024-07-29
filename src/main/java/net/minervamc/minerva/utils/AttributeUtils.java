package net.minervamc.minerva.utils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class AttributeUtils {

    public static double getMaxHealth(LivingEntity entity) {
       AttributeInstance instance = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        assert instance != null;
        return instance.getValue();
    }
}
