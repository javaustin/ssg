package com.carrotguy69.ssg.game.loot;

import com.carrotguy69.ssg.utils.objects.NumberRange;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class LootManager {

    private final List<LootItem> itemPool; // The list of possible items to roll from.
    private final List<LootEnchant> simpleEnchantPool; // The list of possible “simple” enchants to roll from.

    private double totalItemWeight = 0; // Cached sum of all item weights in itemPool (used for weighted random selection).

    // How many items to select per chest (range).
    private final NumberRange itemsPerChest;

    // How many enchants to apply to each item (range).
    private final NumberRange enchantsPerItem;

    // Whether simple enchant logic is enabled at all.
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
        sumWeights();
    }

    public boolean removeItem(LootItem item) {
        boolean success =  itemPool.remove(item);
        sumWeights();
        return success;
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

    /**
     * Selects a single LootItem using weights, copies it, then applies simple enchants.
     */
    public LootItem selectItem() {

        // Roll in [0, totalItemWeight)
        // If totalItemWeight is 0, roll becomes 0.
        double roll = 0 != totalItemWeight ? r.nextDouble(0, totalItemWeight) : 0;

        // Pick the item based on roll.
        LootItem item = selectItem(roll);

        // After selecting an item, optionally apply enchants to it.
        applySimpleEnchants(item);

        return item;
    }

    /**
     * Weighted selection based on the given roll.
     * Uses “cumulative weight”:
     * - Keep adding weights as we iterate
     * - When roll < cumulative, we found the selected item
     */
    private LootItem selectItem(double roll) {
        double cumulative = 0;

        LootItem item = null;

        for (LootItem lootItem : itemPool) {
            cumulative += lootItem.getWeight();
            if (roll < cumulative) {
                item = lootItem;
                break;
            }
        }

        // If roll was not in any bucket, something is inconsistent (e.g., weights are all 0 or NaN).
        if (item == null) {
            throw new RuntimeException("No LootItem could be selected.");
        }


        item = item.copy(); // Important: copy so the pool’s template item isn’t modified directly.
        return item;
    }

    /**
     * Applies “simple enchants” to the item (if enabled).
     * Steps:
     * 1) Decide how many enchants to apply (biased distribution).
     * 2) Compute which enchantments are compatible with the item.
     * 3) Choose enchants from simpleEnchantPool with weights.
     * 4) Remove incompatible enchants from the selection.
     * 5) Bind the remaining enchants to the item.
     */
    private void applySimpleEnchants(LootItem item) {
        // Use simple-enchant to apply enchants.
        // simple-enchant is considered to be enabled if (enchants != null && !enchants.isEmpty())

        if (!simpleEnchantEnabled || simpleEnchantPool == null || simpleEnchantPool.isEmpty()) {
            return;
        }

        // 1. Determine the amount of enchants to apply using a distribution
        int bias = 3;

        int amount = (int) Math.ceil(Math.pow(r.nextDouble(0, 1), bias) * (enchantsPerItem.max().intValue() + 1)) + enchantsPerItem.min().intValue() - 1;

        if (amount == 0)
            return;

        // 2. Determine compatible enchants
        List<String> compatibleEnchantmentNames = getCompatibleEnchants(item).stream().map(Enchantment::getKey).map(NamespacedKey::getKey).toList();

        // 3. Select an amount of enchants from the simpleEnchantPool
        List<LootEnchant> selected = selectEnchants(simpleEnchantPool, amount);

        // 4. Filter out any incompatible enchantments
        selected = selected.stream().filter(e -> compatibleEnchantmentNames.contains(e.getID())).toList();

        // 5. Bind enchants to item
        item.setBindingEnchants(selected);
    }

    /**
     * Returns an array of selected LootItems, selecting limit times.
     */
    public LootItem[] selectItems(int limit) {
        LootItem[] items = new LootItem[limit];

        for (int i = 0; i < limit; i++) {
            items[i] = selectItem();
        }

        return items;
    }

    /**
     * Calculates which Bukkit Enchantment values are compatible with the given LootItem.
     * <p>
     * Compatibility logic:
     * - ItemStack must be convertible from item
     * - For each Enchantment:
     *   - e.canEnchantItem(stack) must be true
     *   - It must NOT conflict with any existing enchant on the stack
     * <p>
     * Also forces item amount to (1,1) before calling toItemStack().
     */
    private List<Enchantment> getCompatibleEnchants(LootItem item) {
        // Copy the item so the following logic does not affect the template.
        item = item.copy();

        // Force amount to 1 so enchant compatibility checks don’t get skewed by stack size.
        item.setAmount(new NumberRange(1, 1));

        ItemStack stack = item.toItemStack();

        if (stack == null) {
            return List.of();
        }

        Map<Enchantment, Integer> enchantmentIntegerMap = stack.getEnchantments();

        Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

        List<Enchantment> results = new ArrayList<>();

        for (Enchantment enchantment : registry) {
            boolean conflict = false;

            if (!enchantment.canEnchantItem(stack)) {
                continue;
            }

            if (enchantmentIntegerMap.isEmpty()) {
                results.add(enchantment);
                continue;
            }

            for (Enchantment stackEnchantment : enchantmentIntegerMap.keySet()) {
                if (enchantment.conflictsWith(stackEnchantment)) {
                    conflict = true;
                    break;
                }
            }

            if (!conflict) {
                results.add(enchantment);
            }
        }

        return results;
    }

    /**
     * Weighted selection of a single LootEnchant from a pool.
     * - Computes total weight ignoring negative weights
     * - Rolls in [0, totalWeight)
     * - Picks first enchant where roll < cumulative
     */
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

    /**
     * Selects <code>limit</code> LootEnchant entries from the given pool.
     * <p>
     *     Note: Some of the returned enchants may be of the same type
     */
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