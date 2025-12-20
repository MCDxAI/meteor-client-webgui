package com.cope.meteorwebgui.mapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Maps all Meteor Client modules and their settings to JSON structure
 */
public class ModuleMapper {
    private static final Logger LOG = LoggerFactory.getLogger("WebGUI Module Mapper");

    /**
     * Map all modules organized by category
     */
    public static JsonObject mapAllModulesByCategory() {
        JsonObject categoriesObj = new JsonObject();

        try {
            for (Category category : Modules.loopCategories()) {
                // Skip HUD category
                if (category.name.equalsIgnoreCase("hud")) {
                    continue;
                }

                List<Module> modules = Modules.get().getGroup(category);
                JsonArray modulesArray = new JsonArray();

                for (Module module : modules) {
                    modulesArray.add(mapModule(module));
                }

                categoriesObj.add(category.name, modulesArray);
                LOG.info("Mapped {} modules in category '{}'", modules.size(), category.name);
            }
        } catch (Exception e) {
            LOG.error("Failed to map modules by category: {}", e.getMessage(), e);
        }

        return categoriesObj;
    }

    /**
     * Map a single module to JSON
     */
    public static JsonObject mapModule(Module module) {
        JsonObject moduleObj = new JsonObject();

        try {
            moduleObj.addProperty("name", module.name);
            moduleObj.addProperty("title", module.title);
            moduleObj.addProperty("description", module.description);
            moduleObj.addProperty("category", module.category.name);
            moduleObj.addProperty("active", module.isActive());
            moduleObj.addProperty("addon", module.addon != null ? module.addon.name : "Meteor Client");

            // Map setting groups
            JsonArray settingGroupsArray = new JsonArray();
            for (SettingGroup group : module.settings) {
                JsonObject groupObj = new JsonObject();
                groupObj.addProperty("name", group.name);

                JsonArray settingsArray = new JsonArray();
                for (Setting<?> setting : group) {
                    settingsArray.add(SettingsReflector.getSettingMetadata(setting));
                }

                groupObj.add("settings", settingsArray);
                settingGroupsArray.add(groupObj);
            }

            moduleObj.add("settingGroups", settingGroupsArray);

        } catch (Exception e) {
            LOG.error("Failed to map module {}: {}", module.name, e.getMessage());
        }

        return moduleObj;
    }

    /**
     * Get a lightweight module list (without settings) for faster initial load
     */
    public static JsonObject mapModulesLightweight() {
        JsonObject categoriesObj = new JsonObject();

        try {
            for (Category category : Modules.loopCategories()) {
                if (category.name.equalsIgnoreCase("hud")) {
                    continue;
                }

                List<Module> modules = Modules.get().getGroup(category);
                JsonArray modulesArray = new JsonArray();

                for (Module module : modules) {
                    JsonObject moduleObj = new JsonObject();
                    moduleObj.addProperty("name", module.name);
                    moduleObj.addProperty("title", module.title);
                    moduleObj.addProperty("description", module.description);
                    moduleObj.addProperty("active", module.isActive());
                    moduleObj.addProperty("addon", module.addon != null ? module.addon.name : "Meteor Client");

                    modulesArray.add(moduleObj);
                }

                categoriesObj.add(category.name, modulesArray);
            }
        } catch (Exception e) {
            LOG.error("Failed to map modules (lightweight): {}", e.getMessage(), e);
        }

        return categoriesObj;
    }

    /**
     * Get module state change message
     */
    public static JsonObject createModuleStateMessage(Module module) {
        JsonObject data = new JsonObject();
        data.addProperty("moduleName", module.name);
        data.addProperty("active", module.isActive());
        return data;
    }

    /**
     * Get setting value change message
     */
    public static JsonObject createSettingChangeMessage(Module module, Setting<?> setting) {
        JsonObject data = new JsonObject();
        data.addProperty("moduleName", module.name);
        data.addProperty("settingName", setting.name);
        data.add("value", SettingsReflector.getSettingValue(setting, SettingsReflector.detectSettingType(setting)));
        return data;
    }

    /**
     * Get list of all favorite module names
     */
    public static JsonArray getFavorites() {
        JsonArray favorites = new JsonArray();
        try {
            for (Module module : Modules.get().getAll()) {
                if (module.favorite) {
                    favorites.add(module.name);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to get favorites: {}", e.getMessage(), e);
        }
        return favorites;
    }

    /**
     * Set favorites from a list of module names
     * @param favoriteNames List of module names to mark as favorites
     */
    public static void setFavorites(List<String> favoriteNames) {
        try {
            Set<String> favoriteSet = new HashSet<>(favoriteNames);
            for (Module module : Modules.get().getAll()) {
                module.favorite = favoriteSet.contains(module.name);
            }
            LOG.info("Updated favorites: {} modules marked as favorite", favoriteNames.size());
        } catch (Exception e) {
            LOG.error("Failed to set favorites: {}", e.getMessage(), e);
        }
    }

    /**
     * Create favorites state changed message
     */
    public static JsonObject createFavoritesStateMessage() {
        JsonObject data = new JsonObject();
        data.add("favorites", getFavorites());
        return data;
    }
}
