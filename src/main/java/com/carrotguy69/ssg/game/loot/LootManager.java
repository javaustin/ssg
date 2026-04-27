package com.carrotguy69.ssg.game.loot;

import com.carrotguy69.ssg.utils.objects.NumberRange;
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

    private final NumberRange itemsPerChest;
    private final NumberRange enchantsPerItem;

    private final boolean simpleEnchantEnabled;

    private Random r;

    public LootManager(List<LootItem> itemPool, List<LootEnchant> simpleEnchantPool, NumberRange itemsPerChest, NumberRange enchantsPerItem, boolean simpleEnchantEnabled) {
        this.itemPool = itemPool;
        this.simpleEnchantPool = simpleEnchantPool;
        this.itemsPerChest = itemsPerChest;
        this.enchantsPerItem = enchantsPerItem;
        this.simpleEnchantEnabled = simpleEnchantEnabled;
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

    public NumberRange getItemsPerChest() {
        return this.itemsPerChest;
    }

    public NumberRange getEnchantsPerItem() {
        return this.enchantsPerItem;
    }

    public boolean isSimpleEnchantEnabled() {
        return this.simpleEnchantEnabled;
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

        applySimpleEnchants(item);

        return item;
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
            throw new RuntimeException("No LootItem could be selected.");
        }

        item = item.copy();
        return item;
    }


    private void applySimpleEnchants(LootItem item) {
        // Use simple-enchant to apply enchants.
        // simple-enchant is considered to be enabled if (enchants != null && !enchants.isEmpty())

        if (!simpleEnchantEnabled || simpleEnchantPool == null || simpleEnchantPool.isEmpty()) {
            return;
        }

//        if (item.getWeightedEnchants() != null && !item.getWeightedEnchants().isEmpty()) {
//            // Skip if item is already enchanted
//            return;
//        }

        // 1. Determine the amount of enchants to apply using a distribution
        int bias = 3;

        int amount = (int) Math.ceil(Math.pow(r.nextDouble(0, 1), bias) * (enchantsPerItem.max().intValue() + 1)) + enchantsPerItem.min().intValue() - 1;

        if (amount == 0)
            return;

        // 2. Determine compatible enchants
        List<String> compatibleEnchantmentNames = getCompatibleEnchants(item).stream().map(Enchantment::getKey).map(NamespacedKey::getKey).toList();

        // 3. Select an amount of enchants from the simpleEnchantPool
        List<LootEnchant> selected = selectEnchants(simpleEnchantPool, amount);

        // 4. Filter our any incompatible enchantments
        selected = selected.stream().filter(e -> !compatibleEnchantmentNames.contains(e.getID())).toList();

        // 5. Bind enchants to item
        item.setBindingEnchants(selected);
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


    private LootEnchant selectEnchant(List<LootEnchant> pool) {
        double totalWeight = 0;

        for (LootEnchant lootEnchant : pool) {
            if (lootEnchant.getWeight() < 0)
                continue;

            totalWeight += lootEnchant.getWeight();
        }

        double roll = 0 != totalWeight ? r.nextDouble(0, totalWeight) : 0;

        double cumulative = 0;
        for (LootEnchant lootEnchant : pool) {
            cumulative += lootEnchant.getWeight();
            if (roll < cumulative) {
                return lootEnchant;
            }
        }

        if (!pool.isEmpty()) {
            return pool.getFirst();
        }

        throw new RuntimeException("LootEnchant pool cannot be empty.");
    }


    private List<LootEnchant> selectEnchants(List<LootEnchant> pool, int limit) {
        List<LootEnchant> enchants = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            LootEnchant enchant = selectEnchant(pool);

            enchants.add(enchant);
        }

        return enchants;
    }

    @Override
    public String toString() {
        return "LootManager{" +
                "itemPool=" + itemPool + "," +
                "simpleEnchantPool=" + simpleEnchantPool + "," +
                "itemsPerChest=" + itemsPerChest + "," +
                "enchantsPerItem=" + enchantsPerItem + "," +
                "simpleEnchantEnabled=" + simpleEnchantEnabled +
                "}";
    }

}