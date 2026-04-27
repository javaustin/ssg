package com.carrotguy69.ssg.game.loot;

import com.carrotguy69.ssg.utils.objects.NumberRange;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.carrotguy69.ssg.SpeedSG.f;

public class LootItem {
    private final String id;
    private final NumberRange amount;
    private final double weight;

    private String displayName;
    private String[] lore;

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

    public double getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getLore() {
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

    public void setLore(String[] lore) {
        this.lore = lore;
    }

    public void setWeightedEnchants(List<LootEnchant> weightedEnchants) {
        this.weightedEnchants = weightedEnchants;
    }

    public void addEnchant(LootEnchant enchant) {
        this.weightedEnchants.add(enchant);
    }

    public boolean removeEnchant(LootEnchant enchant) {
        return this.weightedEnchants.remove(enchant);
    }

    public void setBindingEnchants(List<LootEnchant> bindingEnchants) {
        this.bindingEnchants = bindingEnchants;
    }

    public void addBindingEnchant(LootEnchant enchant) {
        this.bindingEnchants.add(enchant);
    }

    public boolean removeBindingEnchant(LootEnchant enchant) {
        return this.weightedEnchants.remove(enchant);
    }


    public ItemStack toItemStack() {
        ItemStack is = new ItemStack(
                Material.valueOf(id),
                amount.generateRandom(0).intValue()
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
            List<String> lines = new ArrayList<>();

            for (String line : lore) {
                lines.add(f(line));
            }

            meta.setLore(lines);
        }

        if (weightedEnchants != null) {
            for (LootEnchant enchant : weightedEnchants) {
                Enchantment mcEnchantment = Enchantment.getByName(enchant.getID());

                if (mcEnchantment == null) {
                    continue;
                }

                meta.addEnchant(mcEnchantment, enchant.getLevel().generateRandom(0).intValue(), true); // boolean is for allowing unsafe enchantments
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
                "lore=" + Arrays.toString(lore) + "," +
                "weightedEnchants=" + weightedEnchants + "," +
                "bindingEnchants=" + bindingEnchants +
                "}";
    }
}