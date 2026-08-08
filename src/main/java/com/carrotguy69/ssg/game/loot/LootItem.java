package com.carrotguy69.ssg.game.loot;

import com.carrotguy69.ssg.utils.objects.NumberRange;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static com.carrotguy69.ssg.SpeedSG.f;

public class LootItem {
    private final String id;
    private NumberRange amount;
    private final double weight;

    private String displayName;
    private ArrayList<String> lore;

    private List<LootEnchant> weightedEnchants = new ArrayList<>();
    private List<LootEnchant> bindingEnchants = new ArrayList<>();

    public LootItem(String id, NumberRange amount, double weight) {
        this.id = id;
        this.amount = amount;
        this.weight = weight;

        // Determine if the item is valid
        Material.valueOf(id.toUpperCase());
    }

    public String getID() {
        return id;
    }

    public NumberRange getAmount() {
        return amount;
    }

    public void setAmount(NumberRange amount) {
        this.amount = amount;
    }

    public double getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ArrayList<String> getLore() {
        return lore;
    }

    public List<LootEnchant> getWeightedEnchants() {
        return weightedEnchants;
    }

    public List<LootEnchant> getBindingEnchants() {
        return bindingEnchants;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setLore(ArrayList<String> lore) {
        this.lore = lore;
    }

    public void setWeightedEnchants(List<LootEnchant> weightedEnchants) {
        this.weightedEnchants = weightedEnchants;
    }

    public void setBindingEnchants(List<LootEnchant> bindingEnchants) {
        this.bindingEnchants = bindingEnchants;
    }


    public ItemStack toItemStack() {
        int stackAmount = amount.generateRandom(0).intValue();

        if (stackAmount == 0) {
            return null;
        }

        ItemStack is = new ItemStack(
                Material.valueOf(id),
                stackAmount
        );

        ItemMeta meta = is.getItemMeta();

        if (meta == null) {
            return is;
        }

        if (displayName != null) {
            // Sorry paper, I like my coloring better
            meta.setDisplayName(f(displayName));
        }

        if (lore != null) {
            List<String> coloredLore = new ArrayList<>();

            for (String line : lore) {
                coloredLore.add(f(line));
            }

            meta.setLore(coloredLore);
        }

        if (weightedEnchants != null) {
            for (LootEnchant enchant : weightedEnchants) {

                Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

                Enchantment mcEnchantment = registry.get(NamespacedKey.minecraft(enchant.getID()));

                if (mcEnchantment == null) {
                    continue;
                }

                is.addUnsafeEnchantment(mcEnchantment, enchant.getLevel().generateRandom(0).intValue());

//                meta.addEnchant(mcEnchantment, enchant.getLevel().generateRandom(0).intValue(), true); // commented out in favor of the above line ^
            }
        }

        is.setItemMeta(meta);

        return is;
    }

    public LootItem copy() {
        LootItem lootItem = new LootItem(this.id, this.amount, this.weight);
        lootItem.displayName = displayName;
        lootItem.lore = lore;
        lootItem.weightedEnchants = new ArrayList<>(this.weightedEnchants);

        return lootItem;
    }

    @Override
    public String toString() {
        return "LootItem{" +
                "id=" + id + "," +
                "amount=" + amount + "," +
                "weight=" + weight + "," +
                "displayName=" + displayName + "," +
                "lore=" + (lore != null ? lore.toString() : null) + "," +
                "weightedEnchants=" + weightedEnchants + "," +
                "bindingEnchants=" + bindingEnchants +
                "}";
    }
}