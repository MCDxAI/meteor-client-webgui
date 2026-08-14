# Supported setting types

Back to the [README](../README.md). Protocol reference: [WEBSOCKET-PROTOCOL.md](WEBSOCKET-PROTOCOL.md).

TLDR: The web interface renders every Meteor setting type with a matching control — toggles, sliders, color pickers, dropdowns, keybind capture, list editors, and more. You never pick a type by hand. Anything the interface does not recognize falls back to a generic editor, so settings from any addon still show up.

## How type detection works

The addon never asks you to configure types. On connect, it walks every setting of every module and every HUD element and maps each one to a type in its `SettingType` enum:

1. `SettingsReflector` reads the setting's Java class name — for example `BoolSetting` becomes `BOOL`.
2. Known class names map to a dedicated type. Unrecognized names fall back to `UNKNOWN`.
3. The web interface matches the type name to one Vue component and renders it. `UNKNOWN` settings render through the generic editor.

## Setting metadata

Every setting arrives as a metadata object, both in `initial.state` and in the `setting.get` reply:

```json
{
  "name": "use-potion",
  "title": "Use potion",
  "description": "Whether to apply the night vision potion.",
  "type": "BOOL",
  "value": { "value": false },
  "defaultValue": { "value": false },
  "visible": true
}
```

| Field | Meaning |
|:---|:---|
| `name` | The stable setting ID. Use it in `setting.update` and `setting.get`. |
| `title` | The human-readable label shown in-game and in the browser. |
| `description` | The help text from the module author. |
| `type` | One of the type names in the table below. |
| `value` | The current value. Shape depends on the type — see below. |
| `defaultValue` | The value the setting had before any change. |
| `visible` | Whether the setting is currently shown. Conditions can change this at any time. |
| `typeMetadata` | Extra data, present only for types that have it — for example `min`, `max`, and slider range for numbers, or the allowed values for enums. |

## Type reference

Value shapes below are the JSON you read in `value` and write in `setting.update`. Values for `setting.update` use the same shape as the read.

### Basic values

| Type | Typical use | Web control | Value JSON |
|:---|:---|:---|:---|
| `BOOL` | On/off switches | Checkbox | `{ "value": true }` |
| `INT` | Whole numbers | Slider plus number input | `{ "value": 3 }` |
| `DOUBLE` | Decimal numbers | Slider plus number input | `{ "value": 4.5 }` |
| `STRING` | Free text | Text input | `{ "value": "hello" }` |
| `PROVIDED_STRING` | Text from a dropdown of options | Text input | `{ "value": "option" }` |
| `ENUM` | One choice from a fixed list | Dropdown | `{ "value": "Left" }` |

### Colors and keys

| Type | Typical use | Web control | Value JSON |
|:---|:---|:---|:---|
| `COLOR` | One color | Color picker with RGB, alpha, and rainbow toggle | `{ "r": 255, "g": 0, "b": 0, "a": 255, "rainbow": false }` |
| `COLOR_LIST` | Several colors | List of color pickers | `{ "items": [ { "r": 255, "g": 0, "b": 0, "a": 255, "rainbow": false } ] }` |
| `KEYBIND` | A keyboard shortcut | Key capture field | Read: `{ "isKey": true, "value": 341, "modifiers": 0, "label": "Left Ctrl" }`. Write: `isKey`, `value` (key code), optional `modifiers` |
| `FONT_FACE` | A font choice | Dropdown of installed fonts | Read: `{ "family": "<family>", "type": "<type>", "label": "<display name>" }`. Write: `family` and `type` |

### Positions and vectors

| Type | Typical use | Web control | Value JSON |
|:---|:---|:---|:---|
| `BLOCK_POS` | One block position in the world | X, Y, Z number inputs | `{ "x": 10, "y": 64, "z": -5 }` |
| `VECTOR3D` | One point in space | X, Y, Z number inputs | `{ "x": 1.5, "y": 64.0, "z": -2.5 }` |

### Registry values and lists

Registry settings pick entries from Minecraft's registries — blocks, items, entities, and more. The browser loads the registry data on demand with a `registry.request` message, then offers a searchable picker.

| Type | Typical use | Web control | Value JSON |
|:---|:---|:---|:---|
| `BLOCK` | One block | Searchable registry picker | `{ "id": "minecraft:stone", "value": "minecraft:stone" }` |
| `ITEM` | One item | Searchable registry picker | `{ "id": "minecraft:iron_ingot", "value": "minecraft:iron_ingot" }` |
| `POTION` | One potion | Searchable registry picker | `{ "id": "minecraft:swiftness", "value": "minecraft:swiftness" }` |
| `BLOCK_LIST` | A set of blocks | Searchable multi-select | `{ "items": [ "minecraft:stone", "minecraft:dirt" ] }` |
| `STORAGE_BLOCK_LIST` | A set of container blocks | Searchable multi-select | `{ "items": [ "minecraft:chest" ] }` |
| `ITEM_LIST` | A set of items | Searchable multi-select | `{ "items": [ "minecraft:iron_ingot" ] }` |
| `ENTITY_TYPE_LIST` | A set of entity types | Searchable multi-select | `{ "items": [ "minecraft:zombie" ] }` |
| `STATUS_EFFECT_LIST` | A set of status effects | Searchable multi-select | `{ "items": [ "minecraft:speed" ] }` |
| `ENCHANTMENT_LIST` | A set of enchantments | Generic list editor | Limited: reads always return `{ "items": [ ] }`, and updates are rejected. See the note below |
| `PARTICLE_TYPE_LIST` | A set of particle types | Generic list editor | `{ "items": [ "minecraft:flame" ] }` |
| `SOUND_EVENT_LIST` | A set of sounds | Generic list editor | `{ "items": [ "minecraft:block.note.pling" ] }` |
| `SCREEN_HANDLER_LIST` | A set of screen types | Generic list editor | `{ "items": [ "minecraft:chest" ] }` |
| `PACKET_LIST` | A set of network packets | Generic list editor | `{ "items": [ "<packet type name>" ] }` |
| `MODULE_LIST` | A set of other modules | Module multi-select | `{ "items": [ "killaura" ] }` |
| `STRING_LIST` | A set of free-text entries | Text list editor | `{ "items": [ "entry" ] }` |

Enchantment lists are the one gap. The addon detects the type but cannot read or write the values on current Minecraft versions, so the web interface shows an empty list. Everything else in this table round-trips fully.

### Maps and fallbacks

| Type | Typical use | Web control | Value JSON |
|:---|:---|:---|:---|
| `STATUS_EFFECT_AMPLIFIER_MAP` | Effect plus amplifier pairs | Map editor | `{ "entries": [ { "effect": "minecraft:speed", "amplifier": 1 } ] }` |
| `BLOCK_DATA` | Per-block data (for example block-entity NBT) | Generic editor, read-only | `{ "entries": [ { "block": "minecraft:chest", "data": "<serialized>" } ] }` — updates are rejected |
| `GENERIC` | Meteor's own generic string setting | Generic editor | `{ "value": "<string>" }` — written through Meteor's value parser |
| `UNKNOWN` | Any setting class the addon does not recognize | Generic editor | Varies. Writes attempt `{ "value": "<string>" }` through Meteor's value parser |

`UNKNOWN` is the safety net. Any setting type this addon has not seen before still renders, so settings from brand-new addons remain visible. Several list types noted above share the generic list editor for the same reason.

## Adding a new setting type

The steps below add first-class support for a type that currently falls back to `UNKNOWN`.

1. Add the case to `SettingsReflector.detectSettingType()` so the class name maps to a new enum value.
2. Add the enum value to `SettingType`.
3. Add a read case to `SettingsReflector.getSettingValue()` that serializes the value to JSON.
4. Add a write case to `SettingsReflector.setSettingValue()` that parses the JSON back.
5. Create a Vue component in `webui/src/components/settings/` named after the type.
6. Register the component in the switch inside `SettingsPanel.vue`.

If you skip steps 5 and 6, the new type still renders through the generic fallback.

## Where types live in the code

| File | Role |
|:---|:---|
| `src/main/java/com/cope/meteorwebgui/mapping/SettingType.java` | The type enum |
| `src/main/java/com/cope/meteorwebgui/mapping/SettingsReflector.java` | Detection, reads, writes, metadata |
| `webui/src/components/SettingsPanel.vue` | Type-to-component routing |
| `webui/src/components/settings/` | The 21 setting-type components |
