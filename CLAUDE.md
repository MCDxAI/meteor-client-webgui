# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Meteor WebGUI is a Meteor Client addon that provides a web-based GUI with real-time bi-directional control. The project consists of two main components:
- **Java Backend**: Fabric mod addon that creates a WebSocket server for communication
- **Vue Frontend**: Modern web interface for controlling Meteor Client modules and settings

## Essential Development Commands

### Java/Gradle Build
```bash
# Build the addon JAR
./gradlew build

# Run in Minecraft dev environment
./gradlew runClient

# Run tests
./gradlew test

# Clean build artifacts
./gradlew clean
```

Output JAR: `build/libs/meteor-webgui-0.3.0.jar`

### WebUI Development
```bash
cd webui

# Install dependencies (first time)
npm install

# Development server with hot-reload (connects to ws://localhost:8080)
npm run dev
# Access at: http://localhost:3000

# Production build
npm run build
# Output: webui/dist/
```

### Testing the Full Stack

**Development Mode (with Vite hot-reload):**
1. Start Minecraft with the addon: `./gradlew runClient`
2. In-game: Open Meteor GUI (Right Shift) → WebGUI tab → Start Server
3. In separate terminal: `cd webui && npm run dev`
4. Open browser: `http://localhost:3000` (Vite dev server)

**Production Mode (served from addon):**
1. Build WebUI: `cd webui && npm run build`
2. Start Minecraft: `./gradlew runClient`
3. In-game: Open Meteor GUI (Right Shift) → WebGUI tab → Start Server
4. Open browser: `http://localhost:8080` (served from addon's HTTP server)

## Key Architecture Concepts

### Bi-Directional Sync Architecture
The addon maintains real-time synchronization between Minecraft and the web interface:
- **HTTP Server**: Serves the built WebUI from `webui/dist/` on port 8080 (production mode)
- **WebSocket Server**: Real-time bidirectional communication on port 8080 (same port, different protocol)
- **Java → WebUI**: Event monitoring broadcasts module/setting changes via WebSocket
- **WebUI → Java**: User interactions send commands to modify game state
- **Initial State**: Full module/settings snapshot sent on WebSocket connection
- **HUD Preview**: Periodic snapshots of HUD elements sent to WebUI for real-time preview

### Module Mapping System
The `ModuleMapper` class (src/main/java/com/cope/meteorwebgui/mapping/) dynamically discovers ALL modules from Meteor Client and installed addons at runtime. It does NOT require manual registration - it automatically iterates through `Modules.get().getAll()` and maps each module by category.

### HUD Mapping System
The `HudMapper` class provides dynamic discovery and preview of HUD elements:
- Iterates through all HUD elements from Meteor Client and addons
- Captures real-time text content from each HUD element
- Generates preview snapshots showing what will be rendered in-game
- Supports toggling HUD element visibility from the WebUI
- Uses mixins to hook into HUD lifecycle and rendering events

### Settings Reflection System
The `SettingsReflector` class provides generic read/write access to any Meteor setting type via reflection. It:
- Detects setting types using `setting.getClass().getSimpleName()`
- Extracts type-specific metadata (e.g., min/max for IntSetting, enum values for EnumSetting)
- Handles type conversion for JSON serialization/deserialization
- Uses `setting.get()` for reads and `setting.set(T)` for writes

See `ai_docs/METEOR_SETTINGS_RESEARCH.md` for comprehensive details on all 30+ setting types.

### Event Monitoring Pattern
Real-time sync is achieved by wrapping existing Meteor event handlers:
- `ActiveModulesChangedEvent`: Detects module toggles
- Setting `onChanged` callbacks: Wrapped to broadcast setting changes
- All events broadcast to connected WebSocket clients via `WSMessage` protocol

### WebSocket Protocol
Message format defined in `src/main/java/com/cope/meteorwebgui/protocol/`:
```typescript
interface WSMessage {
    type: string;  // e.g., "module.toggle", "setting.update", "initial.state"
    data: any;
    id?: string;   // Request ID for request/response pattern
}
```

**Server → Client Events:**
- `initial.state`: Full module/settings state on connection
- `module.state.changed`: Module toggled
- `setting.value.changed`: Setting value changed
- `hud.snapshot`: HUD preview snapshot with rendered text lines
- `hud.list`: List of all available HUD elements

**Client → Server Commands:**
- `module.toggle`: Toggle module on/off
- `setting.update`: Update setting value
- `module.list`: Request full module list
- `hud.list`: Request HUD elements list
- `hud.toggle`: Toggle HUD element visibility

## Critical References

### ai_reference Folder
**IMPORTANT**: The `ai_reference/` folder (git-ignored) contains high-quality example sources for Meteor Client addon development. **ALWAYS read `ai_reference/INDEX.md` first** when working on Meteor-specific features.

The INDEX.md file contains:
- Complete catalog of all reference repositories
- Quick lookup guide for specific features (modules, settings, commands, HUD, etc.)
- Repository details with star counts, update dates, and feature lists
- Usage guidelines and research strategies
- Best practices for finding relevant implementation examples

All reference addons are verified and compatible with Minecraft 1.21.11 and Meteor Client 1.21.11.

### ai_docs Folder
The `ai_docs/` folder contains detailed technical documentation about the project architecture and Meteor Client integration:

- **ARCHITECTURE.md**: Complete system design document including:
  - High-level architecture diagrams (Java backend ↔ WebSocket ↔ Vue frontend)
  - Core systems: Module Mapping, Settings Reflection, Event Monitoring, WebSocket Protocol
  - WebSocket message format and protocol specification
  - Frontend component structure and Pinia state management
  - Implementation phases and roadmap
  - Security considerations and performance optimization strategies

- **METEOR_SETTINGS_RESEARCH.md**: Comprehensive reference for Meteor Client's settings system:
  - All 30+ setting types (Bool, Int, Double, String, Enum, Color, List types, Map types, etc.)
  - Setting class structure and properties (name, title, description, value, defaultValue, visible, onChanged)
  - Module system architecture and category organization
  - Serialization patterns (NBT format)
  - Type-specific properties (e.g., min/max for IntSetting, enum values for EnumSetting)
  - WebUI component mapping requirements for each setting type
  - Real-time sync challenges and validation requirements

### Other Documentation Files
- `QUICKSTART.md`: Step-by-step testing and troubleshooting guide
- `TESTING_CHECKLIST.md`: Quality assurance checklist

## Java Package Structure

```
src/main/java/com/cope/meteorwebgui/
├── MeteorWebGUIAddon.java       # Entry point, server lifecycle management
├── server/
│   ├── MeteorHTTPServer.java    # HTTP server for serving static files
│   ├── MeteorWebServer.java     # WebSocket server implementation
│   ├── MeteorWebSocket.java     # WebSocket connection wrapper
│   └── MeteorWebSocketHandler.java # WebSocket message handler
├── mapping/
│   ├── ModuleMapper.java        # Dynamic module discovery
│   ├── HudMapper.java           # HUD element mapping
│   ├── SettingsReflector.java   # Generic settings reflection
│   ├── SettingType.java         # Setting type enumeration
│   └── RegistryProvider.java    # Minecraft registry access
├── events/
│   └── EventMonitor.java        # Real-time event broadcasting
├── protocol/
│   ├── WSMessage.java           # Message model
│   └── MessageType.java         # Message type enum
├── systems/
│   └── WebGUIConfig.java        # Persistent config (port, host, auto-start)
├── gui/
│   └── WebGUITab.java           # In-game configuration tab
├── hud/
│   ├── HudPreviewService.java   # HUD preview rendering
│   ├── HudPreviewCapture.java   # HUD screen capture
│   ├── HudPreviewSnapshot.java  # HUD snapshot data
│   └── HudTextLine.java         # HUD text line model
├── mixin/
│   ├── HudMixin.java            # HUD lifecycle hooks
│   └── HudRendererMixin.java    # HUD rendering hooks
└── util/
    └── (utility classes)
```

## Frontend Structure

```
webui/src/
├── main.ts                      # App entry point
├── App.vue                      # Root component
├── stores/
│   ├── modules.ts               # Pinia store for module state
│   ├── websocket.ts             # WebSocket connection management
│   └── hud.ts                   # HUD state management
└── components/
    ├── ModuleList.vue           # Category-organized module list
    ├── ModuleCard.vue           # Individual module card
    ├── ModuleCardCompact.vue    # Compact module card view
    ├── ModuleSettingsDialog.vue # Modal for module settings
    ├── ModuleToolbar.vue        # Module toolbar controls
    ├── SettingsPanel.vue        # Settings display
    ├── layout/                  # Layout components
    ├── modals/                  # Modal dialogs
    ├── ui/                      # Reusable UI components
    ├── hud/                     # HUD-related components
    └── settings/                # Type-specific setting components
        ├── BoolSetting.vue
        ├── IntSetting.vue
        ├── DoubleSetting.vue
        ├── StringSetting.vue
        ├── StringListSetting.vue
        ├── EnumSetting.vue
        ├── ColorSetting.vue
        ├── KeybindSetting.vue
        ├── BlockPosSetting.vue
        ├── Vector3dSetting.vue
        ├── BlockListSetting.vue
        ├── ItemListSetting.vue
        ├── EntityTypeListSetting.vue
        ├── ColorListSetting.vue
        ├── ModuleListSetting.vue
        ├── PotionSetting.vue
        ├── StatusEffectAmplifierMapSetting.vue
        ├── FontFaceSetting.vue
        ├── RegistryValueSetting.vue
        ├── GenericListSetting.vue
        └── GenericSetting.vue
```

## Dependencies

Dependencies are managed via `gradle/libs.versions.toml`:

### Java
- **Minecraft**: 1.21.11
- **Fabric Loader**: 0.18.2
- **Meteor Client**: 1.21.11-SNAPSHOT (from Maven)
- **Orbit**: 0.2.4 (event system)
- **NanoHTTPD**: 2.3.1 (HTTP and WebSocket server)
- **Gson**: 2.11.0 (JSON serialization)

### Frontend (webui/package.json)
- **Vue**: 3.5.13 (reactive framework)
- **Pinia**: 2.3.0 (state management)
- **Vite**: 6.0.7 (build tool with hot-reload)
- **TypeScript**: 5.7.3

## Development Workflow

### Adding New Setting Types
1. Add type detection in `SettingsReflector.detectSettingType()`
2. Implement value extraction in `SettingsReflector.getSettingValue()`
3. Implement value setting in `SettingsReflector.setSettingValue()`
4. Create Vue component in `webui/src/components/settings/`
5. Register component in `SettingsPanel.vue`

### Meteor Client Integration Patterns
When working with Meteor Client APIs, prefer these utility classes:
- **EntityUtils**: Entity filtering, targeting, damage calculations
- **TargetUtils**: Target selection with priority sorting
- **ChatUtils**: Formatted chat messages
- **BlockUtils**: Block queries, placement, breaking
- **RenderUtils**: Rendering helpers, frustum culling

Use `@EventHandler` on methods to subscribe to events (requires `MeteorClient.EVENT_BUS.subscribe(this)`).

### Configuration Storage
User preferences are stored via the `WebGUIConfig` System class:
- Automatically saved to `.minecraft/meteor-client/webgui.nbt`
- Uses Meteor's `System` base class for NBT serialization
- Access via `WebGUIConfig.get().setting.get()`

## Common Patterns

### Module Discovery
```java
// Iterate all modules from Meteor + addons
for (Module module : Modules.get().getAll()) {
    if (module.category.name.equals("hud")) continue; // Exclude HUD
    // Process module...
}
```

### Setting Reflection
```java
// Generic setting access
Setting<?> setting = module.settings.get(settingName);
Object value = setting.get();
setting.set(newValue); // Type must match setting type
```

### Event Broadcasting
```java
// Broadcast to all WebSocket clients
WSMessage msg = new WSMessage("module.state.changed", data);
server.broadcast(gson.toJson(msg));
```

## Current Implementation Status

✅ **Fully Working:**
- Module listing by category
- Module toggle (on/off)
- Real-time module state sync
- WebSocket auto-reconnect with connection status indicator
- HUD element preview and rendering
- **Primitive Settings:** Bool, Int, Double, String, StringList, Enum
- **Visual Settings:** Color, ColorList, Keybind, FontFace
- **Spatial Settings:** BlockPos, Vector3d
- **Registry Settings:** Block, Item, EntityType, Potion, StatusEffectAmplifierMap
- **List Settings:** BlockList, ItemList, EntityTypeList, ModuleList, GenericList
- **Advanced Settings:** RegistryValue, Generic fallback

🎨 **UI Features:**
- Compact and expanded module card views
- Modal dialogs for module settings
- Module toolbar with search and filters
- Real-time HUD preview snapshots

See `ai_docs/ARCHITECTURE.md` for architecture details and future enhancements.

## Troubleshooting Tips

### "Cannot find Meteor classes" compilation error
- Gradle will automatically fetch Meteor Client from Maven
- Run `./gradlew clean build --refresh-dependencies`

### WebSocket connection fails
- Check server is running in Meteor GUI → WebGUI tab
- Verify port matches in both Java config and WebUI connection
- Default: `ws://localhost:8080`

### Changes not reflecting in WebUI
- Check if event monitoring is active (logs should show "Event monitoring started")
- Verify WebSocket connection (green dot in UI header)
- Check browser console (F12) for errors

### Frontend TypeScript errors
```bash
cd webui
rm -rf node_modules package-lock.json
npm install
```

## Testing Approach

Always test both directions:
1. **WebUI → Minecraft**: Toggle module in browser, verify in-game
2. **Minecraft → WebUI**: Toggle module in-game, verify browser updates
3. **Settings sync**: Change setting in either direction, verify sync

See `TESTING_CHECKLIST.md` for comprehensive QA checklist.

## Desktop Notifications

**IMPORTANT**: Use the `mcp__desktop-notifications__send-notification` tool to get the user's attention when:
- Completing significant tasks or milestones
- Finishing autonomous work sessions
- Encountering blockers that need user input
- Completing builds, tests, or other long-running operations

Be proactive but don't spam - use notifications for meaningful updates that warrant interrupting the user's workflow.

Example usage:
```
mcp__desktop-notifications__send-notification({
  title: "Build Complete",
  message: "meteor-webgui-0.1.0.jar compiled successfully with no errors",
  priority: "normal",
  category: "success",
  sound: true
})
```
