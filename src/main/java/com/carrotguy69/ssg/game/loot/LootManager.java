package com.carrotguy69.ssg.game.loot;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;


public class LootManager {

    private final List<LootItem> itemPool;
    private final List<LootEnchant> simpleEnchantPool;

    private double totalItemWeight = 0;

    private final int maxEnchantsPerItem;

    private Random r;

    public LootManager(List<LootItem> itemPool, List<LootEnchant> simpleEnchantPool, int maxEnchantsPerItem) {
        this.itemPool = itemPool;
        this.simpleEnchantPool = simpleEnchantPool;
        this.maxEnchantsPerItem = maxEnchantsPerItem;
        this.r = new Random();

        sumWeights();
    }

    public void setRandom(Random r) {
        this.r = r;
    }

    public void addItem(LootItem item) {
        itemPool.add(item);
    }

    public boolean removeItem(LootItem item) {
        return itemPool.remove(item);
    }

    public List<LootItem> getItemPool() {
        return this.itemPool;
    }

    public void addEnchant(LootEnchant enchant) {
        simpleEnchantPool.add(enchant);
    }

    public boolean removeEnchant(LootEnchant enchant) {
        return simpleEnchantPool.remove(enchant);
    }

    public List<LootEnchant> getSimpleEnchantPool() {
        return this.simpleEnchantPool;
    }


    private void sumWeights() {
        this.totalItemWeight = 0;

        for (LootItem item : itemPool) {
            this.totalItemWeight += item.getWeight();
        }
    }

    public LootItem selectItem() {

        double roll = 0 != totalItemWeight ? r.nextDouble(0, totalItemWeight) : 0;

        LootItem item = getLootItem(roll);

        return applyEnchants(item);
    }

    private LootItem getLootItem(double roll) {
        double cumulative = 0;

        LootItem item = null;

        for (LootItem lootItem : itemPool) {
            cumulative += lootItem.getWeight();
            if (roll < cumulative) {
                item = lootItem;
                break;
            }
        }

        if (item == null) {
            throw new RuntimeException("No item could be selected.");
        }

        // Copy so our enchant methods don't point to the original item and stack enchants recursively
        item = item.copy();
        return item;
    }

    private LootItem applyEnchants(LootItem item) {
        // Use simple-enchant to apply enchants.
        // simple-enchant is considered to be enabled if (enchants != null && !enchants.isEmpty())

        if (simpleEnchantPool == null || simpleEnchantPool.isEmpty()) {
            return item;
        }

        if (item.getEnchants() != null && !item.getEnchants().isEmpty()) {
            // Skip if item is already enchanted (why does it have enchants to begin with?)
            return item;
        }

        // 1. Determine the # of enchants to apply
        int bias = 3;

        // A loop of these creates a distribution skewed towards (1 - 1 = 0)
        int amountOfEnchants = (int) Math.ceil(Math.pow(r.nextDouble(0, 1), bias) * (maxEnchantsPerItem + 1)) - 1;

        if (amountOfEnchants == 0) {
            return item;
        }

        // 2. Determine compatible enchants
        List<String> compatibleEnchantmentNames = getCompatibleEnchants(item).stream().map(Enchantment::getKey).map(NamespacedKey::getKey).toList();

        // 3. Apply LootEnchant(s) to the LootItem.
        List<LootEnchant> selected = selectEnchants(amountOfEnchants);

        // 4. Remove incompatible enchantments
        List<LootEnchant> remove = new ArrayList<>();

        for (LootEnchant selection : selected) {
            if (!compatibleEnchantmentNames.contains(selection.getID())) {
                remove.add(selection);
            }
        }

        for (LootEnchant rm : remove) {
            selected.remove(rm);
        }

        item.getEnchants().addAll(selected);

        return item;
    }

    public LootItem[] selectItems(int limit) {
        LootItem[] items = new LootItem[limit];

        for (int i = 0; i < limit; i++) {
            items[i] = selectItem();
        }

        return items;
    }

    private List<Enchantment> getCompatibleEnchants(LootItem item) {

        Map<Enchantment, Integer> enchantmentIntegerMap = item.toItemStack().getEnchantments();

        return Arrays.stream(Enchantment.values())
                .filter(e -> e.canEnchantItem(item.toItemStack()))
                .filter(e -> enchantmentIntegerMap.keySet().stream()
                        .noneMatch(e::conflictsWith))
                .collect(Collectors.toList());
    }

    private LootEnchant selectEnchant() {
        double totalWeight = 0;

        for (LootEnchant lootEnchant : simpleEnchantPool) {
            totalWeight += lootEnchant.getWeight();
        }

        double roll = 0 != totalWeight ? r.nextDouble(0, totalWeight) : 0;

        double cumulative = 0;
        for (LootEnchant lootEnchant : simpleEnchantPool) {
            cumulative += lootEnchant.getWeight();
            if (roll < cumulative) {
                return lootEnchant;
            }
        }

        if (!simpleEnchantPool.isEmpty()) {
            return simpleEnchantPool.getFirst();
        }

        throw new RuntimeException("options cannot be empty.");
    }


    private List<LootEnchant> selectEnchants(int limit) {
        List<LootEnchant> enchants = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            LootEnchant enchant = selectEnchant();

            enchants.add(enchant);
        }

        return enchants;
    }

    @Override
    public String toString() {
        return "LootManager{" +
                "itemPool=" + itemPool + "," +
                "simpleEnchantPool=" + simpleEnchantPool + "," +
                "maxEnchantsPerItem=" + maxEnchantsPerItem +
                "}";
    }

}