package com.carrotguy69.ssg.game.loot;

import com.carrotguy69.cxyz.exceptions.InvalidConfigurationException;
import com.carrotguy69.ssg.SpeedSG;
import com.carrotguy69.ssg.utils.objects.NumberRange;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.carrotguy69.ssg.SpeedSG.lootTables;

public class LootTable {
    private final String name;
    private final LootManager lootManager;

    public LootTable(String name, LootManager lootManager) {
        this.name = name;
        this.lootManager = lootManager;
    }

    public String getName() {
        return this.name;
    }

    public LootManager getLootManager() {
        return this.lootManager;
    }

    public static List<LootTable> loadLootTables() {

        ConfigurationSection section = SpeedSG.lootYML.getConfigurationSection("loot-tables");

        if (section == null) {
            throw new InvalidConfigurationException("loot.yml", "loot-tables", "Could not find section!");
        }

        List<LootTable> results = new ArrayList<>();

        for (String lootTableName : section.getKeys(false)) {
            ConfigurationSection lootTableSection = section.getConfigurationSection(lootTableName);


            if (lootTableSection == null) {
                // Since we are getting the section name from the parent section itself, it HAS to exist, or else there are much larger problems.
                throw new InvalidConfigurationException("loot.yml", "loot-tables." + lootTableName, "Could not find section! Is the YAML malformed?");
            }

            NumberRange itemsPerChest = NumberRange.fromString(lootTableSection.getString("settings.items-per-chest", "3-7"));

            boolean simpleEnchant = lootTableSection.getBoolean("settings.simple-enchant", false);
            NumberRange enchantsPerItem = NumberRange.fromString(lootTableSection.getString("settings.enchants-per-item", "23"));


            // Do not get simpleEnchantPool if simple-enchant is disabled.
            List<LootEnchant> enchantPool = simpleEnchant ? getEnchantPool(lootTableSection): new ArrayList<>();

            List<LootItem> itemPool = getItemPool(lootTableSection);

            LootManager manager = new LootManager(itemPool, enchantPool, itemsPerChest, enchantsPerItem, simpleEnchant);

            LootTable table = new LootTable(lootTableName, manager);

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
                enchants = getEnchantPool(enchantsListMap);

            LootItem it = new LootItem(id.toUpperCase(), amount, weight.generateRandom(2).doubleValue());
            it.setDisplayName(name);
            it.setLore(lore);

            it.setWeightedEnchants(enchants.stream().filter(e -> e.getWeight() < 0).toList());
            it.setBindingEnchants(enchants.stream().filter(e -> e.getWeight() >= 0).toList());

            results.add(it);
        }

        return results;
    }

    private static List<LootEnchant> getEnchantPool(ConfigurationSection lootTableSection) {
        if (lootTableSection == null) {
            return new ArrayList<>();
        }

        List<Map<?, ?>> enchants = lootTableSection.getMapList("enchant-pool");

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
                weightObj = "-1";
            }

            String id = idObj.toString();
            NumberRange level = NumberRange.fromString(levelObj.toString());
            NumberRange weight = NumberRange.fromString(weightObj.toString());

            results.add(new LootEnchant(id.toUpperCase(), level, weight.generateRandom(2).doubleValue()));
        }

        return results;
    }

    public static LootTable getByName(String name) {
        for (LootTable table : lootTables) {
            if (table.getName().equalsIgnoreCase(name)) {
                return table;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "LootTable{" +
                "name=" + name + "," +
                "lootManager=" + lootManager +
                "}";
    }
}
