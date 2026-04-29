package com.cope.meteorwebgui.mapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides registry data for WebUI autocomplete and list settings.
 * Extracts all blocks, items, entities, status effects, and modules with category information.
 */
public class RegistryProvider {

    /**
     * Get all blocks organized by namespace for category filtering
     */
    public static JsonObject getAllBlocks() {
        JsonObject result = new JsonObject();
        JsonArray blocks = new JsonArray();
        Map<String, JsonArray> byNamespace = new HashMap<>();

        BuiltInRegistries.BLOCK.entrySet().forEach(entry -> {
            Identifier id = entry.getKey().identifier();
            String idString = id.toString();
            String namespace = id.getNamespace();

            // Add to main array
            JsonObject blockObj = new JsonObject();
            blockObj.addProperty("id", idString);
            blockObj.addProperty("namespace", namespace);
            blocks.add(blockObj);

            // Add to namespace grouping
            byNamespace.computeIfAbsent(namespace, k -> new JsonArray()).add(idString);
        });

        result.add("blocks", blocks);
        result.add("byNamespace", toJsonObject(byNamespace));
        return result;
    }

    /**
     * Get all items organized by namespace and item group
     */
    public static JsonObject getAllItems() {
        JsonObject result = new JsonObject();
        JsonArray items = new JsonArray();
        Map<String, JsonArray> byNamespace = new HashMap<>();

        BuiltInRegistries.ITEM.entrySet().forEach(entry -> {
            Identifier id = entry.getKey().identifier();
            Item item = entry.getValue();
            String idString = id.toString();
            String namespace = id.getNamespace();

            // Add to main array
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("id", idString);
            itemObj.addProperty("namespace", namespace);
            items.add(itemObj);

            // Add to namespace grouping
            byNamespace.computeIfAbsent(namespace, k -> new JsonArray()).add(idString);
        });

        result.add("items", items);
        result.add("byNamespace", toJsonObject(byNamespace));
        return result;
    }

    /**
     * Get all potions with namespace metadata
     */
    public static JsonArray getAllPotions() {
        JsonArray potions = new JsonArray();

        BuiltInRegistries.POTION.entrySet().forEach(entry -> {
            Identifier id = entry.getKey().identifier();
            JsonObject potionObj = new JsonObject();
            potionObj.addProperty("id", id.toString());
            potionObj.addProperty("namespace", id.getNamespace());
            potions.add(potionObj);
        });

        return potions;
    }

    /**
     * Get all entity types organized by namespace
     */
    public static JsonObject getAllEntityTypes() {
        JsonObject result = new JsonObject();
        JsonArray entities = new JsonArray();
        Map<String, JsonArray> byNamespace = new HashMap<>();

        BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(entry -> {
            Identifier id = entry.getKey().identifier();
            String idString = id.toString();
            String namespace = id.getNamespace();

            // Add to main array
            JsonObject entityObj = new JsonObject();
            entityObj.addProperty("id", idString);
            entityObj.addProperty("namespace", namespace);
            entities.add(entityObj);

            // Add to namespace grouping
            byNamespace.computeIfAbsent(namespace, k -> new JsonArray()).add(idString);
        });

        result.add("entities", entities);
        result.add("byNamespace", toJsonObject(byNamespace));
        return result;
    }

    /**
     * Get all status effects
     */
    public static JsonArray getAllStatusEffects() {
        JsonArray effects = new JsonArray();

        BuiltInRegistries.MOB_EFFECT.entrySet().forEach(entry -> {
            Identifier id = entry.getKey().identifier();
            JsonObject effectObj = new JsonObject();
            effectObj.addProperty("id", id.toString());
            effectObj.addProperty("namespace", id.getNamespace());
            effects.add(effectObj);
        });

        return effects;
    }

    /**
     * Get all modules organized by category
     */
    public static JsonObject getAllModules() {
        JsonObject result = new JsonObject();
        Map<String, JsonArray> byCategory = new HashMap<>();

        for (Module module : Modules.get().getAll()) {
            // Skip HUD modules
            if (module.category.name.equals("hud")) continue;

            String categoryName = module.category.name;
            JsonObject moduleObj = new JsonObject();
            moduleObj.addProperty("name", module.name);
            moduleObj.addProperty("title", module.title);
            moduleObj.addProperty("category", categoryName);

            byCategory.computeIfAbsent(categoryName, k -> new JsonArray()).add(moduleObj);
        }

        result.add("byCategory", toJsonObject(byCategory));
        return result;
    }

    /**
     * Get all registries in one call for initial state
     */
    public static JsonObject getAllRegistries() {
        JsonObject registries = new JsonObject();
        registries.add("blocks", getAllBlocks());
        registries.add("items", getAllItems());
        registries.add("entities", getAllEntityTypes());
        registries.add("statusEffects", getAllStatusEffects());
        registries.add("potions", getAllPotions());
        registries.add("modules", getAllModules());
        return registries;
    }

    /**
     * Helper to convert Map<String, JsonArray> to JsonObject
     */
    private static JsonObject toJsonObject(Map<String, JsonArray> map) {
        JsonObject obj = new JsonObject();
        map.forEach(obj::add);
        return obj;
    }
}
