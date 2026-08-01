package com.carrotguy69.ssg.game.loot;

import com.carrotguy69.ssg.utils.objects.NumberRange;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

public class LootEnchant {
    private final String id;
    private final NumberRange level;
    private final double weight;

    public LootEnchant(String id, NumberRange level, double weight) {
        this.id = id;
        this.level = level;
        this.weight = weight;

        // determine if the enchantment actually exists

        Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

        Enchantment enchant = registry.get(NamespacedKey.minecraft(id));

        if (enchant == null) {
            throw new RuntimeException("Enchantment by name of '" + id + "' does not exist!");
        }
    }

    public String getID() {
        return id;
    }

    public NumberRange getLevel() {
        return level;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return "LootEnchant{" +
                "id=" + id + "," +
                "level=" + level + "," +
                "weight=" + weight +
                "}";
    }
}