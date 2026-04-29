package com.cope.meteorwebgui.mapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.MyPotion;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

/**
 * Reflects on Meteor Client settings to extract metadata and manipulate values
 */
public class SettingsReflector {
    private static final Logger LOG = LoggerFactory.getLogger("WebGUI Settings Reflector");

    /**
     * Detect the type of a setting
     */
    public static SettingType detectSettingType(Setting<?> setting) {
        String className = setting.getClass().getSimpleName();

        return switch (className) {
            case "BoolSetting" -> SettingType.BOOL;
            case "IntSetting" -> SettingType.INT;
            case "DoubleSetting" -> SettingType.DOUBLE;
            case "StringSetting" -> SettingType.STRING;
            case "EnumSetting" -> SettingType.ENUM;
            case "ColorSetting" -> SettingType.COLOR;
            case "KeybindSetting" -> SettingType.KEYBIND;
            case "BlockSetting" -> SettingType.BLOCK;
            case "ItemSetting" -> SettingType.ITEM;
            case "PotionSetting" -> SettingType.POTION;
            case "BlockListSetting" -> SettingType.BLOCK_LIST;
            case "ItemListSetting" -> SettingType.ITEM_LIST;
            case "EntityTypeListSetting" -> SettingType.ENTITY_TYPE_LIST;
            case "ModuleListSetting" -> SettingType.MODULE_LIST;
            case "EnchantmentListSetting" -> SettingType.ENCHANTMENT_LIST;
            case "ParticleTypeListSetting" -> SettingType.PARTICLE_TYPE_LIST;
            case "SoundEventListSetting" -> SettingType.SOUND_EVENT_LIST;
            case "MobEffectListSetting" -> SettingType.STATUS_EFFECT_LIST;
            case "StorageBlockListSetting" -> SettingType.STORAGE_BLOCK_LIST;
            case "StringListSetting" -> SettingType.STRING_LIST;
            case "ColorListSetting" -> SettingType.COLOR_LIST;
            case "PacketListSetting" -> SettingType.PACKET_LIST;
            case "BlockPosSetting" -> SettingType.BLOCK_POS;
            case "Vector3dSetting" -> SettingType.VECTOR3D;
            case "FontFaceSetting" -> SettingType.FONT_FACE;
            case "ProvidedStringSetting" -> SettingType.PROVIDED_STRING;
            case "StatusEffectAmplifierMapSetting" -> SettingType.STATUS_EFFECT_AMPLIFIER_MAP;
            case "BlockDataSetting" -> SettingType.BLOCK_DATA;
            case "ScreenHandlerListSetting" -> SettingType.SCREEN_HANDLER_LIST;
            case "GenericSetting" -> SettingType.GENERIC;
            default -> SettingType.UNKNOWN;
        };
    }

    /**
     * Get setting metadata as JSON
     */
    public static JsonObject getSettingMetadata(Setting<?> setting) {
        JsonObject metadata = new JsonObject();
        SettingType type = detectSettingType(setting);

        metadata.addProperty("name", setting.name);
        metadata.addProperty("title", setting.title);
        metadata.addProperty("description", setting.description);
        metadata.addProperty("type", type.name());
        metadata.add("value", getSettingValue(setting, type));
        metadata.add("defaultValue", getDefaultValue(setting, type));
        metadata.addProperty("visible", setting.isVisible());

        // Add type-specific metadata
        JsonObject typeMetadata = getTypeSpecificMetadata(setting, type);
        if (typeMetadata.size() > 0) {
            metadata.add("typeMetadata", typeMetadata);
        }

        return metadata;
    }

    /**
     * Get current setting value as JSON
     */
    public static JsonObject getSettingValue(Setting<?> setting, SettingType type) {
        Object value = null;
        try {
            value = setting.get();
        } catch (Exception e) {
            LOG.error("Failed to read current value for setting {}: {}", setting.name, e.getMessage());
        }

        if (value == null) {
            try {
                value = setting.getDefaultValue();
            } catch (Exception e) {
                LOG.warn("Failed to read default value for setting {}: {}", setting.name, e.getMessage());
            }
        }

        return serializeValue(setting, type, value);
    }

    /**
     * Get default setting value as JSON
     */
    private static JsonObject getDefaultValue(Setting<?> setting, SettingType type) {
        Object defaultValue = null;
        try {
            defaultValue = setting.getDefaultValue();
        } catch (Exception e) {
            LOG.error("Failed to read default value for setting {}: {}", setting.name, e.getMessage());
        }

        return serializeValue(setting, type, defaultValue);
    }

    private static JsonObject serializeValue(Setting<?> setting, SettingType type, Object rawValue) {
        JsonObject valueObj = new JsonObject();
        Object value = rawValue;

        if (value == null) {
            addEmptyValueContainer(valueObj, type);
            return valueObj;
        }

        try {
            switch (type) {
                case BOOL -> valueObj.addProperty("value", (Boolean) value);
                case INT -> valueObj.addProperty("value", (Integer) value);
                case DOUBLE -> valueObj.addProperty("value", (Double) value);
                case STRING, PROVIDED_STRING -> valueObj.addProperty("value", (String) value);
                case ENUM -> valueObj.addProperty("value", value.toString());
                case COLOR -> {
                    SettingColor color = (SettingColor) value;
                    valueObj.addProperty("r", color.r);
                    valueObj.addProperty("g", color.g);
                    valueObj.addProperty("b", color.b);
                    valueObj.addProperty("a", color.a);
                    valueObj.addProperty("rainbow", color.rainbow);
                }
                case BLOCK_POS -> {
                    BlockPos pos = (BlockPos) value;
                    valueObj.addProperty("x", pos.getX());
                    valueObj.addProperty("y", pos.getY());
                    valueObj.addProperty("z", pos.getZ());
                }
                case VECTOR3D -> {
                    try {
                        if (value instanceof Vec3 vec) {
                            valueObj.addProperty("x", vec.x);
                            valueObj.addProperty("y", vec.y);
                            valueObj.addProperty("z", vec.z);
                        } else {
                            double x = (double) value.getClass().getField("x").get(value);
                            double y = (double) value.getClass().getField("y").get(value);
                            double z = (double) value.getClass().getField("z").get(value);
                            valueObj.addProperty("x", x);
                            valueObj.addProperty("y", y);
                            valueObj.addProperty("z", z);
                        }
                    } catch (Exception e) {
                        valueObj.addProperty("error", "Unsupported vector type: " + value.getClass().getName());
                    }
                }
                case KEYBIND -> writeKeybindValue(valueObj, (Keybind) value);
                case BLOCK -> {
                    Identifier id = BuiltInRegistries.BLOCK.getKey((Block) value);
                    if (id != null) {
                        valueObj.addProperty("id", id.toString());
                        valueObj.addProperty("value", id.toString());
                    }
                }
                case ITEM -> {
                    Identifier id = BuiltInRegistries.ITEM.getKey((Item) value);
                    if (id != null) {
                        valueObj.addProperty("id", id.toString());
                        valueObj.addProperty("value", id.toString());
                    }
                }
                case POTION -> {
                    Identifier id = null;
                    if (value instanceof Potion potion) {
                        id = BuiltInRegistries.POTION.getKey(potion);
                    } else if (value instanceof MyPotion myPotion) {
                        id = extractPotionId(myPotion.potion.get());
                    }
                    if (id != null) {
                        valueObj.addProperty("id", id.toString());
                        valueObj.addProperty("value", id.toString());
                    }
                }
                case BLOCK_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<Block> blocks = (List<Block>) value;
                    JsonArray array = new JsonArray();
                    for (Block block : blocks) {
                        array.add(BuiltInRegistries.BLOCK.getKey(block).toString());
                    }
                    valueObj.add("items", array);
                }
                case ITEM_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<Item> items = (List<Item>) value;
                    JsonArray array = new JsonArray();
                    for (Item item : items) {
                        array.add(BuiltInRegistries.ITEM.getKey(item).toString());
                    }
                    valueObj.add("items", array);
                }
                case ENTITY_TYPE_LIST -> {
                    @SuppressWarnings("unchecked")
                    Set<EntityType<?>> entities = (Set<EntityType<?>>) value;
                    JsonArray array = new JsonArray();
                    for (EntityType<?> entity : entities) {
                        array.add(BuiltInRegistries.ENTITY_TYPE.getKey(entity).toString());
                    }
                    valueObj.add("items", array);
                }
                case MODULE_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<Module> modules = (List<Module>) value;
                    JsonArray array = new JsonArray();
                    for (Module module : modules) {
                        array.add(module.name);
                    }
                    valueObj.add("items", array);
                }
                case STRING_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<String> strings = (List<String>) value;
                    JsonArray array = new JsonArray();
                    strings.forEach(array::add);
                    valueObj.add("items", array);
                }
                case COLOR_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<SettingColor> colors = (List<SettingColor>) value;
                    JsonArray array = new JsonArray();
                    for (SettingColor color : colors) {
                        JsonObject colorObj = new JsonObject();
                        colorObj.addProperty("r", color.r);
                        colorObj.addProperty("g", color.g);
                        colorObj.addProperty("b", color.b);
                        colorObj.addProperty("a", color.a);
                        colorObj.addProperty("rainbow", color.rainbow);
                        array.add(colorObj);
                    }
                    valueObj.add("items", array);
                }
                case ENCHANTMENT_LIST -> {
                    JsonArray array = new JsonArray();
                    valueObj.add("items", array);
                }
                case STATUS_EFFECT_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<MobEffect> effects = (List<MobEffect>) value;
                    JsonArray array = new JsonArray();
                    for (MobEffect effect : effects) {
                        array.add(BuiltInRegistries.MOB_EFFECT.getKey(effect).toString());
                    }
                    valueObj.add("items", array);
                }
                case PARTICLE_TYPE_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<ParticleType<?>> particles = (List<ParticleType<?>>) value;
                    JsonArray array = new JsonArray();
                    for (ParticleType<?> particle : particles) {
                        array.add(BuiltInRegistries.PARTICLE_TYPE.getKey(particle).toString());
                    }
                    valueObj.add("items", array);
                }
                case SOUND_EVENT_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<SoundEvent> sounds = (List<SoundEvent>) value;
                    JsonArray array = new JsonArray();
                    for (SoundEvent sound : sounds) {
                        Identifier id = BuiltInRegistries.SOUND_EVENT.getKey(sound);
                        if (id != null) array.add(id.toString());
                    }
                    valueObj.add("items", array);
                }
                case STORAGE_BLOCK_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<BlockEntityType<?>> blockEntityTypes = (List<BlockEntityType<?>>) value;
                    JsonArray array = new JsonArray();
                    for (BlockEntityType<?> blockEntityType : blockEntityTypes) {
                        Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType);
                        if (id != null) array.add(id.toString());
                    }
                    valueObj.add("items", array);
                }
                case SCREEN_HANDLER_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<MenuType<?>> handlers = (List<MenuType<?>>) value;
                    JsonArray array = new JsonArray();
                    for (MenuType<?> handler : handlers) {
                        array.add(BuiltInRegistries.MENU.getKey(handler).toString());
                    }
                    valueObj.add("items", array);
                }
                case STATUS_EFFECT_AMPLIFIER_MAP -> {
                    @SuppressWarnings("unchecked")
                    Map<MobEffect, Integer> map = (Map<MobEffect, Integer>) value;
                    JsonArray entries = new JsonArray();
                    for (Map.Entry<MobEffect, Integer> entry : map.entrySet()) {
                        JsonObject entryObj = new JsonObject();
                        entryObj.addProperty("effect", BuiltInRegistries.MOB_EFFECT.getKey(entry.getKey()).toString());
                        entryObj.addProperty("amplifier", entry.getValue());
                        entries.add(entryObj);
                    }
                    valueObj.add("entries", entries);
                }
                case PACKET_LIST -> {
                    @SuppressWarnings("unchecked")
                    Set<Class<? extends Packet<?>>> packets = (Set<Class<? extends Packet<?>>>) value;
                    JsonArray array = new JsonArray();
                    for (Class<? extends Packet<?>> packet : packets) {
                        array.add(PacketUtils.getName(packet));
                    }
                    valueObj.add("items", array);
                }
                case FONT_FACE -> {
                    FontFace fontFace = (FontFace) value;
                    if (fontFace != null) {
                        valueObj.addProperty("family", fontFace.info.family());
                        valueObj.addProperty("type", fontFace.info.type().name());
                        valueObj.addProperty("label", fontFace.info.toString());
                    }
                }
                case BLOCK_DATA -> {
                    @SuppressWarnings("unchecked")
                    Map<Block, ?> blockData = (Map<Block, ?>) value;
                    JsonArray entries = new JsonArray();
                    for (Map.Entry<Block, ?> entry : blockData.entrySet()) {
                        Identifier id = BuiltInRegistries.BLOCK.getKey(entry.getKey());
                        if (id == null) continue;
                        JsonObject entryObj = new JsonObject();
                        entryObj.addProperty("block", id.toString());
                        Object data = entry.getValue();
                        if (data instanceof ISerializable<?> serializable) {
                            entryObj.addProperty("data", serializable.toTag().toString());
                        } else {
                            entryObj.addProperty("data", String.valueOf(data));
                        }
                        entries.add(entryObj);
                    }
                    valueObj.add("entries", entries);
                }
                default -> valueObj.addProperty("value", value.toString());
            }
        } catch (Exception e) {
            LOG.error("Failed to get value for setting {}: {}", setting.name, e.getMessage());
            valueObj.addProperty("error", e.getMessage());
        }

        return valueObj;
    }

    private static void addEmptyValueContainer(JsonObject valueObj, SettingType type) {
        switch (type) {
            case BLOCK_LIST, ITEM_LIST, ENTITY_TYPE_LIST, MODULE_LIST,
                    ENCHANTMENT_LIST, PARTICLE_TYPE_LIST, SOUND_EVENT_LIST,
                    STATUS_EFFECT_LIST, STORAGE_BLOCK_LIST, STRING_LIST,
                    COLOR_LIST, PACKET_LIST, SCREEN_HANDLER_LIST -> valueObj.add("items", new JsonArray());
            case STATUS_EFFECT_AMPLIFIER_MAP, BLOCK_DATA -> valueObj.add("entries", new JsonArray());
            case KEYBIND -> addDefaultKeybindValue(valueObj);
            case BLOCK, ITEM, POTION -> {
                valueObj.add("value", JsonNull.INSTANCE);
                valueObj.addProperty("id", "");
            }
            case FONT_FACE -> {
                valueObj.addProperty("family", "");
                valueObj.addProperty("type", "");
                valueObj.addProperty("label", "");
            }
            default -> valueObj.add("value", JsonNull.INSTANCE);
        }
    }
    /**
     * Get type-specific metadata (min/max for numbers, enum values, etc.)
     */
    private static JsonObject getTypeSpecificMetadata(Setting<?> setting, SettingType type) {
        JsonObject meta = new JsonObject();

        try {
            switch (type) {
                case INT -> {
                    IntSetting intSetting = (IntSetting) setting;
                    // Handle Integer.MAX_VALUE/MIN_VALUE which can't be serialized to JSON
                    meta.addProperty("min", intSetting.min == Integer.MIN_VALUE ? -999999999 : intSetting.min);
                    meta.addProperty("max", intSetting.max == Integer.MAX_VALUE ? 999999999 : intSetting.max);
                    meta.addProperty("sliderMin", intSetting.sliderMin);
                    meta.addProperty("sliderMax", intSetting.sliderMax);
                    meta.addProperty("noSlider", intSetting.noSlider);
                }
                case DOUBLE -> {
                    DoubleSetting doubleSetting = (DoubleSetting) setting;
                    // Handle Double.MAX_VALUE/MIN_VALUE which can't be serialized to JSON
                    double min = Double.isInfinite(doubleSetting.min) ? -999999999.0 : doubleSetting.min;
                    double max = Double.isInfinite(doubleSetting.max) ? 999999999.0 : doubleSetting.max;
                    meta.addProperty("min", min);
                    meta.addProperty("max", max);
                    meta.addProperty("sliderMin", doubleSetting.sliderMin);
                    meta.addProperty("sliderMax", doubleSetting.sliderMax);
                    meta.addProperty("noSlider", doubleSetting.noSlider);
                    meta.addProperty("decimalPlaces", doubleSetting.decimalPlaces);
                }
                case ENUM -> {
                    List<String> suggestions = setting.getSuggestions();
                    JsonArray values = new JsonArray();
                    suggestions.forEach(values::add);
                    meta.add("values", values);
                }
                case PROVIDED_STRING -> {
                    List<String> suggestions = setting.getSuggestions();
                    JsonArray values = new JsonArray();
                    suggestions.forEach(values::add);
                    meta.add("suggestions", values);
                }
                case COLOR -> {
                    meta.addProperty("format", "rgba");
                    meta.addProperty("minValue", 0);
                    meta.addProperty("maxValue", 255);
                    meta.addProperty("supportsRainbow", true);
                }
                case KEYBIND -> {
                    meta.addProperty("supportsMouse", true);
                    meta.addProperty("supportsModifiers", true);
                }
                case BLOCK -> {
                    meta.addProperty("registry", "blocks");
                    meta.addProperty("searchable", true);
                }
                case ITEM -> {
                    meta.addProperty("registry", "items");
                    meta.addProperty("searchable", true);
                }
                case POTION -> {
                    JsonArray values = new JsonArray();
                    for (MyPotion value : MyPotion.values()) {
                        JsonObject obj = new JsonObject();
                        Identifier id = extractPotionId(value.potion.get());
                        obj.addProperty("id", id != null ? id.toString() : value.name());
                        obj.addProperty("label", value.name());
                        values.add(obj);
                    }
                    meta.add("values", values);
                }
                case BLOCK_LIST -> {
                    meta.addProperty("registry", "blocks");
                    meta.addProperty("searchable", true);
                }
                case ITEM_LIST -> {
                    meta.addProperty("registry", "items");
                    meta.addProperty("searchable", true);
                }
                case ENTITY_TYPE_LIST -> {
                    meta.addProperty("registry", "entities");
                    meta.addProperty("searchable", true);
                }
                case MODULE_LIST -> {
                    meta.addProperty("registry", "modules");
                    meta.addProperty("searchable", true);
                }
                case STATUS_EFFECT_LIST -> {
                    meta.addProperty("registry", "statusEffects");
                    meta.addProperty("searchable", true);
                }
                case ENCHANTMENT_LIST -> {
                    meta.addProperty("registry", "enchantments");
                    meta.addProperty("searchable", true);
                }
                case PARTICLE_TYPE_LIST, SOUND_EVENT_LIST, SCREEN_HANDLER_LIST -> {
                    meta.addProperty("searchable", true);
                }
                case STRING_LIST -> {
                    meta.addProperty("freeInput", true);
                }
                case COLOR_LIST -> {
                    meta.addProperty("itemType", "color");
                    meta.addProperty("supportsRainbow", true);
                }
                case BLOCK_POS -> {
                    meta.addProperty("minY", -64);
                    meta.addProperty("maxY", 319);
                }
                case STATUS_EFFECT_AMPLIFIER_MAP -> {
                    meta.addProperty("keyRegistry", "statusEffects");
                    meta.addProperty("valueType", "integer");
                }
                case FONT_FACE -> meta.add("families", buildFontFamiliesMetadata());
                case BLOCK_DATA -> meta.addProperty("editable", false);
            }
        } catch (Exception e) {
            LOG.error("Failed to get type metadata for setting {}: {}", setting.name, e.getMessage());
        }

        return meta;
    }

    /**
     * Set a setting value from JSON
     */
    @SuppressWarnings("unchecked")
    public static boolean setSettingValue(Setting<?> setting, JsonObject valueData) {
        SettingType type = detectSettingType(setting);

        try {
            switch (type) {
                case BOOL -> {
                    Setting<Boolean> boolSetting = (Setting<Boolean>) setting;
                    return boolSetting.set(valueData.get("value").getAsBoolean());
                }
                case INT -> {
                    Setting<Integer> intSetting = (Setting<Integer>) setting;
                    return intSetting.set(valueData.get("value").getAsInt());
                }
                case DOUBLE -> {
                    Setting<Double> doubleSetting = (Setting<Double>) setting;
                    return doubleSetting.set(valueData.get("value").getAsDouble());
                }
                case STRING, PROVIDED_STRING -> {
                    Setting<String> stringSetting = (Setting<String>) setting;
                    return stringSetting.set(valueData.get("value").getAsString());
                }
                case ENUM -> {
                    return setting.parse(valueData.get("value").getAsString());
                }
                case COLOR -> {
                    Setting<SettingColor> colorSetting = (Setting<SettingColor>) setting;
                    SettingColor color = colorSetting.get();
                    color.r = valueData.get("r").getAsInt();
                    color.g = valueData.get("g").getAsInt();
                    color.b = valueData.get("b").getAsInt();
                    color.a = valueData.get("a").getAsInt();
                    color.rainbow = valueData.has("rainbow") && valueData.get("rainbow").getAsBoolean();
                    color.validate();
                    colorSetting.onChanged();
                    return true;
                }
                case KEYBIND -> {
                    Setting<Keybind> keybindSetting = (Setting<Keybind>) setting;
                    Keybind keybind = keybindSetting.get();
                    boolean isKey = valueData.get("isKey").getAsBoolean();
                    int inputValue = valueData.get("value").getAsInt();
                    int modifiers = valueData.has("modifiers") ? valueData.get("modifiers").getAsInt() : 0;
                    if (keybind != null) {
                        keybind.set(isKey, inputValue, modifiers);
                        keybindSetting.onChanged();
                        return true;
                    }
                    return false;
                }
                case BLOCK -> {
                    Setting<Block> blockSetting = (Setting<Block>) setting;
                    Identifier id = readIdentifier(valueData, setting, "id");
                    if (id == null) return false;
                    Block block = BuiltInRegistries.BLOCK.getValue(id);
                    if (block == null || !BuiltInRegistries.BLOCK.getKey(block).equals(id)) {
                        LOG.warn("Invalid block ID '{}' for setting {}", id, setting.name);
                        return false;
                    }
                    return blockSetting.set(block);
                }
                case ITEM -> {
                    Setting<Item> itemSetting = (Setting<Item>) setting;
                    Identifier id = readIdentifier(valueData, setting, "id");
                    if (id == null) return false;
                    Item item = BuiltInRegistries.ITEM.getValue(id);
                    if (item == null || !BuiltInRegistries.ITEM.getKey(item).equals(id)) {
                        LOG.warn("Invalid item ID '{}' for setting {}", id, setting.name);
                        return false;
                    }
                    return itemSetting.set(item);
                }
                case POTION -> {
                    Setting<Potion> potionSetting = (Setting<Potion>) setting;
                    Identifier id = readIdentifier(valueData, setting, "id");
                    if (id == null) return false;
                    Potion potion = BuiltInRegistries.POTION.getValue(id);
                    if (potion == null || !BuiltInRegistries.POTION.getKey(potion).equals(id)) {
                        LOG.warn("Invalid potion ID '{}' for setting {}", id, setting.name);
                        return false;
                    }
                    return potionSetting.set(potion);
                }
                case BLOCK_POS -> {
                    Setting<BlockPos> posSetting = (Setting<BlockPos>) setting;
                    int x = valueData.get("x").getAsInt();
                    int y = valueData.get("y").getAsInt();
                    int z = valueData.get("z").getAsInt();
                    return posSetting.set(new BlockPos(x, y, z));
                }
                case VECTOR3D -> {
                    Setting<Vec3> vecSetting = (Setting<Vec3>) setting;
                    double x = valueData.get("x").getAsDouble();
                    double y = valueData.get("y").getAsDouble();
                    double z = valueData.get("z").getAsDouble();
                    return vecSetting.set(new Vec3(x, y, z));
                }
                case BLOCK_LIST -> {
                    Setting<List<Block>> blockList = (Setting<List<Block>>) setting;
                    JsonArray items = valueData.getAsJsonArray("items");
                    List<Block> blocks = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        try {
                            Identifier id = Identifier.parse(items.get(i).getAsString());
                            Block block = BuiltInRegistries.BLOCK.getValue(id);
                            if (block != null && BuiltInRegistries.BLOCK.getKey(block).equals(id)) {
                                blocks.add(block);
                            }
                        } catch (Exception e) {
                            LOG.warn("Invalid block ID in list: {}", items.get(i).getAsString());
                        }
                    }
                    return blockList.set(blocks);
                }
                case ITEM_LIST -> {
                    Setting<List<Item>> itemList = (Setting<List<Item>>) setting;
                    JsonArray items = valueData.getAsJsonArray("items");
                    List<Item> itemsList = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        try {
                            Identifier id = Identifier.parse(items.get(i).getAsString());
                            Item item = BuiltInRegistries.ITEM.getValue(id);
                            if (item != null && BuiltInRegistries.ITEM.getKey(item).equals(id)) {
                                itemsList.add(item);
                            }
                        } catch (Exception e) {
                            LOG.warn("Invalid item ID in list: {}", items.get(i).getAsString());
                        }
                    }
                    return itemList.set(itemsList);
                }
                case ENTITY_TYPE_LIST -> {
                    Setting<Set<EntityType<?>>> entityList = (Setting<Set<EntityType<?>>>) setting;
                    JsonArray items = valueData.getAsJsonArray("items");
                    Set<EntityType<?>> entities = new ObjectOpenHashSet<>();
                    for (int i = 0; i < items.size(); i++) {
                        try {
                            Identifier id = Identifier.parse(items.get(i).getAsString());
                            EntityType<?> entity = BuiltInRegistries.ENTITY_TYPE.getValue(id);
                            if (entity != null && BuiltInRegistries.ENTITY_TYPE.getKey(entity).equals(id)) {
                                entities.add(entity);
                            }
                        } catch (Exception e) {
                            LOG.warn("Invalid entity type ID in list: {}", items.get(i).getAsString());
                        }
                    }
                    return entityList.set(entities);
                }
                case MODULE_LIST -> {
                    Setting<List<Module>> moduleList = (Setting<List<Module>>) setting;
                    JsonArray items = valueData.getAsJsonArray("items");
                    List<Module> modules = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        try {
                            String moduleName = items.get(i).getAsString();
                            Module module = meteordevelopment.meteorclient.systems.modules.Modules.get().get(moduleName);
                            if (module != null) {
                                modules.add(module);
                            }
                        } catch (Exception e) {
                            LOG.warn("Invalid module name in list: {}", items.get(i).getAsString());
                        }
                    }
                    return moduleList.set(modules);
                }
                case STRING_LIST -> {
                    Setting<List<String>> stringList = (Setting<List<String>>) setting;
                    JsonArray items = valueData.getAsJsonArray("items");
                    List<String> strings = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        strings.add(items.get(i).getAsString());
                    }
                    return stringList.set(strings);
                }
                case COLOR_LIST -> {
                    Setting<List<SettingColor>> colorList = (Setting<List<SettingColor>>) setting;
                    JsonArray items = valueData.getAsJsonArray("items");
                    List<SettingColor> colors = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        JsonObject colorObj = items.get(i).getAsJsonObject();
                        SettingColor color = new SettingColor(
                            colorObj.get("r").getAsInt(),
                            colorObj.get("g").getAsInt(),
                            colorObj.get("b").getAsInt(),
                            colorObj.get("a").getAsInt()
                        );
                        if (colorObj.has("rainbow")) {
                            color.rainbow = colorObj.get("rainbow").getAsBoolean();
                        }
                        colors.add(color);
                    }
                    return colorList.set(colors);
                }
                case ENCHANTMENT_LIST -> {
                    // In 1.21, enchantments may not be in Registries - skip if not available
                    LOG.warn("ENCHANTMENT_LIST setting type not fully supported in 1.21");
                    return false;
                }
                case STATUS_EFFECT_LIST -> {
                    Setting<List<MobEffect>> effectList = (Setting<List<MobEffect>>) setting;
                    JsonArray items = valueData.getAsJsonArray("items");
                    List<MobEffect> effects = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        try {
                            Identifier id = Identifier.parse(items.get(i).getAsString());
                            MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(id);
                            if (effect != null) {
                                effects.add(effect);
                            }
                        } catch (Exception e) {
                            LOG.warn("Invalid status effect ID in list: {}", items.get(i).getAsString());
                        }
                    }
                    return effectList.set(effects);
                }
                case PARTICLE_TYPE_LIST -> {
                    Setting<List<ParticleType<?>>> particleList = (Setting<List<ParticleType<?>>>) setting;
                    JsonArray items = valueData.getAsJsonArray("items");
                    List<ParticleType<?>> particles = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        try {
                            Identifier id = Identifier.parse(items.get(i).getAsString());
                            ParticleType<?> particle = BuiltInRegistries.PARTICLE_TYPE.getValue(id);
                            if (particle != null) {
                                particles.add(particle);
                            }
                        } catch (Exception e) {
                            LOG.warn("Invalid particle type ID in list: {}", items.get(i).getAsString());
                        }
                    }
                    return particleList.set(particles);
                }
                case SOUND_EVENT_LIST -> {
                    Setting<List<SoundEvent>> soundList = (Setting<List<SoundEvent>>) setting;
                    JsonArray items = valueData.getAsJsonArray("items");
                    List<SoundEvent> sounds = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        try {
                            Identifier id = Identifier.parse(items.get(i).getAsString());
                            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(id);
                            if (sound != null) {
                                sounds.add(sound);
                            }
                        } catch (Exception e) {
                            LOG.warn("Invalid sound event ID in list: {}", items.get(i).getAsString());
                        }
                    }
                    return soundList.set(sounds);
                }
                case STORAGE_BLOCK_LIST -> {
                    // StorageBlockListSetting stores BlockEntityTypes, not Blocks
                    Setting<List<BlockEntityType<?>>> blockEntityList = (Setting<List<BlockEntityType<?>>>) setting;
                    JsonArray items = valueData.getAsJsonArray("items");
                    List<BlockEntityType<?>> blockEntityTypes = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        try {
                            Identifier id = Identifier.parse(items.get(i).getAsString());
                            BlockEntityType<?> blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(id);
                            if (blockEntityType != null && BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType).equals(id)) {
                                blockEntityTypes.add(blockEntityType);
                            }
                        } catch (Exception e) {
                            LOG.warn("Invalid storage block entity ID in list: {}", items.get(i).getAsString());
                        }
                    }
                    return blockEntityList.set(blockEntityTypes);
                }
                case SCREEN_HANDLER_LIST -> {
                    Setting<List<MenuType<?>>> handlerList = (Setting<List<MenuType<?>>>) setting;
                    JsonArray items = valueData.getAsJsonArray("items");
                    List<MenuType<?>> handlers = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        try {
                            Identifier id = Identifier.parse(items.get(i).getAsString());
                            MenuType<?> handler = BuiltInRegistries.MENU.getValue(id);
                            if (handler != null) {
                                handlers.add(handler);
                            }
                        } catch (Exception e) {
                            LOG.warn("Invalid screen handler ID in list: {}", items.get(i).getAsString());
                        }
                    }
                    return handlerList.set(handlers);
                }
                case STATUS_EFFECT_AMPLIFIER_MAP -> {
                    Setting<Map<MobEffect, Integer>> mapSetting = (Setting<Map<MobEffect, Integer>>) setting;
                    JsonArray entries = valueData.getAsJsonArray("entries");
                    Map<MobEffect, Integer> map = new HashMap<>();
                    for (int i = 0; i < entries.size(); i++) {
                        try {
                            JsonObject entry = entries.get(i).getAsJsonObject();
                            Identifier id = Identifier.parse(entry.get("effect").getAsString());
                            MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(id);
                            int amplifier = entry.get("amplifier").getAsInt();
                            if (effect != null) {
                                map.put(effect, amplifier);
                            }
                        } catch (Exception e) {
                            LOG.warn("Invalid status effect amplifier map entry: {}", e.getMessage());
                        }
                    }
                    return mapSetting.set(map);
                }
                case PACKET_LIST -> {
                    Setting<Set<Class<? extends Packet<?>>>> packetSetting = (Setting<Set<Class<? extends Packet<?>>>>) setting;
                    JsonArray items = valueData.getAsJsonArray("items");
                    Set<Class<? extends Packet<?>>> packets = new ObjectOpenHashSet<>();
                    for (int i = 0; i < items.size(); i++) {
                        try {
                            String name = items.get(i).getAsString();
                            Class<? extends Packet<?>> packet = PacketUtils.getPacket(name);
                            if (packet != null) packets.add(packet);
                            else LOG.warn("Unknown packet '{}' in setting {}", name, setting.name);
                        } catch (Exception e) {
                            LOG.warn("Invalid packet entry in setting {}: {}", setting.name, e.getMessage());
                        }
                    }
                    return packetSetting.set(packets);
                }
                case FONT_FACE -> {
                    Setting<FontFace> fontFaceSetting = (Setting<FontFace>) setting;
                    if (!valueData.has("family") || !valueData.has("type")) return false;
                    String family = valueData.get("family").getAsString();
                    String typeName = valueData.get("type").getAsString();
                    FontFace fontFace = findFontFace(family, typeName);
                    if (fontFace == null) {
                        LOG.warn("Invalid font face {}-{} for setting {}", family, typeName, setting.name);
                        return false;
                    }
                    return fontFaceSetting.set(fontFace);
                }
                default -> {
                    // Try parse for other types
                    if (valueData.has("value")) {
                        return setting.parse(valueData.get("value").getAsString());
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to set value for setting {}: {}", setting.name, e.getMessage());
            return false;
        }

        return false;
    }


    private static void writeKeybindValue(JsonObject valueObj, Keybind keybind) {
        if (keybind == null) {
            addDefaultKeybindValue(valueObj);
            return;
        }

        CompoundTag tag = keybind.toTag();
        valueObj.addProperty("isKey", tag.getBooleanOr("isKey", false));
        valueObj.addProperty("value", tag.getIntOr("value", -1));
        valueObj.addProperty("modifiers", tag.getIntOr("modifiers", 0));
        valueObj.addProperty("label", keybind.toString());
    }

    private static void addDefaultKeybindValue(JsonObject valueObj) {
        valueObj.addProperty("isKey", true);
        valueObj.addProperty("value", -1);
        valueObj.addProperty("modifiers", 0);
        valueObj.addProperty("label", "None");
    }

    private static JsonArray buildFontFamiliesMetadata() {
        JsonArray families = new JsonArray();
        for (FontFamily family : Fonts.FONT_FAMILIES) {
            JsonObject familyObj = new JsonObject();
            familyObj.addProperty("name", family.getName());
            JsonArray types = new JsonArray();
            for (FontInfo.Type type : FontInfo.Type.values()) {
                if (family.hasType(type)) types.add(type.name());
            }
            familyObj.add("types", types);
            families.add(familyObj);
        }
        return families;
    }

    private static FontFace findFontFace(String familyName, String typeName) {
        if (familyName == null || typeName == null) return null;
        String normalizedType = typeName.toUpperCase(Locale.ROOT);
        for (FontFamily family : Fonts.FONT_FAMILIES) {
            if (!family.getName().equalsIgnoreCase(familyName)) continue;
            try {
                FontInfo.Type type = FontInfo.Type.valueOf(normalizedType);
                FontFace fontFace = family.get(type);
                if (fontFace != null) return fontFace;
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }


    private static Identifier extractPotionId(ItemStack stack) {
        if (stack == null) return null;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return null;
        var entryOpt = contents.potion();
        if (entryOpt.isEmpty()) return null;
        return entryOpt.get().unwrapKey().map(ResourceKey::identifier).orElse(null);
    }

    private static Identifier readIdentifier(JsonObject valueData, Setting<?> setting, String key) {
        if (!valueData.has(key)) {
            LOG.warn("Missing '{}' value for setting {}", key, setting.name);
            return null;
        }
        try {
            String raw = valueData.get(key).getAsString();
            return Identifier.parse(raw);
        } catch (Exception e) {
            LOG.warn("Invalid identifier for setting {}: {}", setting.name, e.getMessage());
            return null;
        }
    }

}
