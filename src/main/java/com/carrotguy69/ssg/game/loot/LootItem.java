package com.carrotguy69.ssg.game.loot;

import com.carrotguy69.cxyz.utils.NumberRange;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.carrotguy69.ssg.SpeedSG.f;

public class LootItem {
    private final String id;
    private NumberRange amount;
    private final double weight;

    private String displayName;
    private ArrayList<String> lore;

    private PotionType potionData = null;


    private List<LootEnchant> weightedEnchants = new ArrayList<>();
    private List<LootEnchant> bindingEnchants = new ArrayList<>();

    public LootItem(String id, NumberRange amount, double weight) {
        this.id = id;
        this.amount = amount;
        this.weight = weight;

        // Determine if the item is valid
        try {
            Material.valueOf(id.toUpperCase().replace("-", "_"));
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException(String.format("Could not load the item %s because it is not a valid minecraft item!", id));
        }
    }

    public void setPotionData(PotionType potionEffect) {
        if (potionEffect == null) {
            return;
        }

        this.potionData = potionEffect;

//        new PotionEffect(PotionEffectType.MY_TYPE, duration, amplifier, ambient, particles);
    }

    public PotionType getPotionData() {
        return this.potionData;
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
                Material.valueOf(id.toUpperCase().replace("-", "_")),
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

        if (bindingEnchants != null) {
            for (LootEnchant enchant : bindingEnchants) {

                Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

                Enchantment mcEnchantment = registry.get(NamespacedKey.minecraft(enchant.getID()));

                if (mcEnchantment == null) {
                    meta.setEnchantmentGlintOverride(Boolean.TRUE);
                    continue;
                }


                meta.addEnchant(mcEnchantment, enchant.getLevel().generateRandom(0).intValue(), true);
            }
        }

        if (weightedEnchants != null) {

            int bias = 3;
            int amount = (int) Math.ceil(Math.pow(new Random().nextDouble(0, 1), bias) * (weightedEnchants.size() + 1)) - 1;

            List<LootEnchant> selected = new ArrayList<>();

            double totalWeight = 0;

            for (LootEnchant lootEnchant : weightedEnchants) {
                // sum the total weight
                if (lootEnchant.getWeight() < 0)
                    continue;

                totalWeight += lootEnchant.getWeight();
            }

            double roll = 0 != totalWeight ? new Random().nextDouble(0, totalWeight) : 0;

            double cumulative = 0;
            for (int i = 0; i < Math.min(amount, weightedEnchants.size()); i++) {
                LootEnchant lootEnchant = weightedEnchants.get(i);

                cumulative += lootEnchant.getWeight();
                if (roll < cumulative) {
                    selected.add(lootEnchant);
                }
            }

            if (selected.isEmpty() && is.getType() == Material.ENCHANTED_BOOK && !weightedEnchants.isEmpty()) {
                selected.add(weightedEnchants.getFirst());
            }

            for (LootEnchant enchant : selected) {
                Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

                Enchantment mcEnchantment = registry.get(NamespacedKey.minecraft(enchant.getID()));

                if (mcEnchantment == null) {
                    meta.setEnchantmentGlintOverride(Boolean.TRUE);
                    continue;
                }

                meta.addEnchant(mcEnchantment, enchant.getLevel().generateRandom(0).intValue(), true);
            }
        }

        if (potionData != null && id.toUpperCase().contains("POTION")) {
            PotionMeta potionMeta = (PotionMeta) meta;

            potionMeta.setBasePotionType(potionData);
            is.setItemMeta(potionMeta);
        }

        is.setItemMeta(meta);

        return is;
    }

    public LootItem copy() {
        LootItem lootItem = new LootItem(this.id, this.amount, this.weight);
        lootItem.displayName = displayName;
        lootItem.lore = lore;
        lootItem.weightedEnchants = new ArrayList<>(this.weightedEnchants);
        lootItem.potionData = potionData;

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