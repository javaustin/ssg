package com.carrotguy69.ssg.game.loot;

import com.carrotguy69.cxyz.exceptions.YAMLFormatException;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.utils.objects.NumberRange;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LootTable {
    private final String name;
    private final NumberRange itemsPerChest;
    private final LootManager lootManager;

    public LootTable(String name, LootManager lootManager, NumberRange itemsPerChest) {
        this.name = name;
        this.lootManager = lootManager;
        this.itemsPerChest = itemsPerChest;
    }

    public String getName() {
        return this.name;
    }

    public LootManager getLootManager() {
        return this.lootManager;
    }

    public NumberRange getItemsPerChestRange() {
        return this.itemsPerChest;
    }

    public static List<LootTable> loadLootTables() {

        ConfigurationSection section = SpeedSG.lootYML.getConfigurationSection("loot-tables");

        if (section == null) {
            throw new YAMLFormatException("loot.yml", "loot-tables", "Could not find section!");
        }

        List<LootTable> results = new ArrayList<>();

        for (String lootTableName : section.getKeys(false)) {
            ConfigurationSection lootTableSection = section.getConfigurationSection(lootTableName);

            // Not worth doing a proper check, because we are getting the table name from the section itself. It HAS to exist.
            assert lootTableSection != null;


            boolean simpleEnchant = lootTableSection.getBoolean("simple-enchant.enabled", false);

            // Assume that every YAML key that accepts a Number also accepts a Number (not supported in YAML, so a String). So instead convert from String -> NumberRange -> Number.
            // It's better to consistently allow NumberRange than to switch every other YAML key, albeit a NumberRange doesn't entirely make sense with this setting.
            int maxEnchantsPerItem = NumberRange.fromString(lootTableSection.getString("simple-enchant.max-enchants-per-item", "2")).generateRandom(0).intValue();

            // do NOT get enchant pool if simpleEnchant is disabled
            List<LootEnchant> enchantPool = simpleEnchant ? getEnchantPool(lootTableSection): new ArrayList<>();

            List<LootItem> itemPool = getItemPool(lootTableSection);

            LootManager manager = new LootManager(itemPool, enchantPool, maxEnchantsPerItem);

            // todo: expose itemsPerChest NumberRange to loot.yml for configurability when you have thought of a decent structure
            LootTable table = new LootTable(lootTableName, manager, new NumberRange(3,  7));

            results.add(table);
        }

        return results;
    }

    private static List<LootItem> getItemPool(ConfigurationSection lootTableSection) {
        if (lootTableSection == null) {
            return new ArrayList<>();
        }

        List<Map<?, ?>> items = lootTableSection.getMapList("item-pool");

        List<LootItem> results = new ArrayList<>();

        for (Map<?, ?> item : items) {

            Object idObj = item.get("id");
            Object amountObj = item.get("amount");
            Object nameObj = item.get("name");
            Object weightObj = item.get("weight");

            Object loreObj = item.get("lore");
            Object enchantsObj = item.get("enchants");

            if (idObj == null) {
                continue;
            }

            if (amountObj == null) {
                amountObj = "1";
            }

            if (weightObj == null) {
                weightObj = "1";
            }

            String id = idObj.toString();
            NumberRange amount = NumberRange.fromString(amountObj.toString());
            NumberRange weight = NumberRange.fromString(weightObj.toString());

            String name = nameObj != null ? (String) nameObj : null;
            String[] lore = loreObj != null ? (String[]) loreObj : null;
            List<Map<?, ?>> enchantsListMap = enchantsObj != null ? (List<Map<?, ?>>) enchantsObj : null;

            // Convert enchants list map to List<LootEnchant>

            List<LootEnchant> enchants = new ArrayList<>();
            if (enchantsListMap != null)
                getEnchantPool(enchantsListMap);

            LootItem it = new LootItem(id.toUpperCase(), amount, weight.generateRandom(2).doubleValue());
            it.setDisplayName(name);
            it.setLore(lore);
            it.setEnchants(enchants);

            results.add(it);
        }

        return results;
    }

    private static List<LootEnchant> getEnchantPool(ConfigurationSection lootTableSection) {
        if (lootTableSection == null) {
            return new ArrayList<>();
        }

        List<Map<?, ?>> enchants = lootTableSection.getMapList("simple-enchant.enchant-pool");

        return getEnchantPool(enchants);
    }

    private static List<LootEnchant> getEnchantPool(List<Map<?, ?>> enchants) {
        List<LootEnchant> results = new ArrayList<>();

        for (Map<?, ?> enchant : enchants) {
            Object idObj = enchant.get("id");
            Object levelObj = enchant.get("level");
            Object weightObj = enchant.get("weight");

            if (idObj == null) {
                continue;
            }

            if (levelObj == null) {
                levelObj = "1";
            }

            if (weightObj == null) {
                weightObj = "1";
            }

            String id = idObj.toString();
            NumberRange level = NumberRange.fromString(levelObj.toString());
            NumberRange weight = NumberRange.fromString(weightObj.toString());

            results.add(new LootEnchant(id.toUpperCase(), level, weight.generateRandom(2).doubleValue()));
        }

        return results;
    }

    @Override
    public String toString() {
        return "LootTable{" +
                "name=" + name + "," +
                "lootManager=" + lootManager +
                "}";
    }
}
