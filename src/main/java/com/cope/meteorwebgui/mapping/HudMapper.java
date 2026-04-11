package com.cope.meteorwebgui.mapping;

import com.cope.meteorwebgui.hud.HudPreviewCapture;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Maps Meteor HUD elements and their settings for the WebGUI initial state payload.
 */
public final class HudMapper {
    private static final Logger LOG = LoggerFactory.getLogger("WebGUI HudMapper");
    private static final String HUD_PREFIX = "hud::";

    private HudMapper() {}

    public static JsonObject mapHudState() {
        JsonObject data = new JsonObject();
        data.add("elements", mapHudElements());
        data.add("previews", HudPreviewCapture.serializeSnapshots());
        return data;
    }

    private static JsonArray mapHudElements() {
        JsonArray elements = new JsonArray();
        try {
            Hud hud = Hud.get();
            for (HudElement element : hud) {
                elements.add(mapElement(element));
            }
            LOG.info("Mapped {} HUD elements", elements.size());
        } catch (Exception e) {
            LOG.error("Failed to map HUD elements: {}", e.getMessage(), e);
        }
        return elements;
    }

    private static JsonObject mapElement(HudElement element) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", getElementIdentifier(element));
        obj.addProperty("title", element.info != null ? element.info.title : element.getClass().getSimpleName());
        obj.addProperty("description", element.info != null ? element.info.description : "");
        obj.addProperty("group", element.info != null && element.info.group != null ? element.info.group.title() : "HUD");
        obj.addProperty("category", "HUD");
        obj.addProperty("addon", element.info != null && element.info.group != null ? element.info.group.title() : "Meteor HUD");
        obj.addProperty("active", element.isActive());
        obj.addProperty("x", element.getX());
        obj.addProperty("y", element.getY());
        obj.addProperty("width", element.getWidth());
        obj.addProperty("height", element.getHeight());

        JsonArray settingGroupsArray = new JsonArray();
        for (SettingGroup group : element.settings) {
            JsonObject groupObj = new JsonObject();
            groupObj.addProperty("name", group.name);

            JsonArray settingsArray = new JsonArray();
            for (Setting<?> setting : group) {
                settingsArray.add(SettingsReflector.getSettingMetadata(setting));
            }

            groupObj.add("settings", settingsArray);
            settingGroupsArray.add(groupObj);
        }

        obj.add("settingGroups", settingGroupsArray);
        return obj;
    }

    public static String getElementIdentifier(HudElement element) {
        HudElementInfo<?> info = element.info;
        String base = info != null ? info.name : element.getClass().getSimpleName();
        return HUD_PREFIX + base + "#" + System.identityHashCode(element);
    }

    public static boolean isHudIdentifier(String identifier) {
        return identifier != null && identifier.startsWith(HUD_PREFIX);
    }

    public static String normalizeIdentifier(String identifier) {
        if (identifier == null) return null;
        return isHudIdentifier(identifier) ? identifier.substring(HUD_PREFIX.length()) : identifier;
    }

    public static HudElement findElement(String identifier) {
        if (identifier == null) return null;
        String target = normalizeIdentifier(identifier);
        String base = target;
        Integer hash = null;
        int hashIndex = target.lastIndexOf('#');
        if (hashIndex >= 0 && hashIndex < target.length() - 1) {
            base = target.substring(0, hashIndex);
            try {
                hash = Integer.parseInt(target.substring(hashIndex + 1));
            } catch (NumberFormatException ignored) {
                hash = null;
            }
        }
        Hud hud = Hud.get();
        for (HudElement element : hud) {
            HudElementInfo<?> info = element.info;
            String name = info != null ? info.name : element.getClass().getSimpleName();
            if (name.equalsIgnoreCase(base)) {
                if (hash == null || hash == System.identityHashCode(element)) {
                    return element;
                }
            }
        }
        return null;
    }

    public static JsonObject createHudStateMessage(HudElement element) {
        JsonObject data = new JsonObject();
        data.addProperty("elementName", getElementIdentifier(element));
        data.addProperty("active", element.isActive());
        data.addProperty("x", element.getX());
        data.addProperty("y", element.getY());
        data.addProperty("width", element.getWidth());
        data.addProperty("height", element.getHeight());
        return data;
    }

    public static JsonObject createHudSettingChangeMessage(HudElement element, Setting<?> setting) {
        JsonObject data = new JsonObject();
        data.addProperty("elementName", getElementIdentifier(element));
        data.addProperty("settingName", setting.name);
        data.add("value", SettingsReflector.getSettingValue(setting, SettingsReflector.detectSettingType(setting)));
        data.add("visibility", createSettingVisibilityMap(element));
        return data;
    }

    private static JsonObject createSettingVisibilityMap(HudElement element) {
        JsonObject visibility = new JsonObject();
        for (SettingGroup group : element.settings) {
            for (Setting<?> setting : group) {
                visibility.addProperty(setting.name, setting.isVisible());
            }
        }
        return visibility;
    }
}
