<div align="center">
  <h1>Meteor WebGUI</h1>
  <img width="256" height="256" alt="icon" src="https://github.com/user-attachments/assets/273769c8-c51d-4294-9b2b-cddbac3e91ba" />
  <p><strong>Real-time, bi-directional web interface for every Meteor Client module and setting.</strong></p>
</div>

<div align="center">
  <img src="https://img.shields.io/badge/Status-Preview-purple?style=flat">
  <img src="https://img.shields.io/badge/Minecraft-26.2-0ea5e9?style=flat">
  <img src="https://img.shields.io/badge/Fabric%20Loader-0.19.3+-f59e0b?style=flat">
  <img src="https://img.shields.io/badge/Meteor%20Client-26.2-ec4899?style=flat">
  <img src="https://img.shields.io/badge/Java-25+-orange?style=flat">
</div>

<br>

<div align="center">

| | |
|:---:|:---:|
| **Module Control** | Toggle, search, and organize every module from Meteor and installed addons — grouped by category or filtered by favorites. |
| **Full Settings Editor** | 30+ setting types with type-aware UI controls: bool toggles, int/double sliders, color pickers, enum dropdowns, keybind capture, list editors, and more. |
| **Bi-directional Sync** | Changes in the browser appear in-game instantly and vice-versa, powered by a persistent WebSocket channel. |
| **Registry Streaming** | Block, item, entity, and potion registries stream on demand so list-setting editors always have up-to-date data. |
| **Live HUD Preview** | Real-time text snapshots of active HUD elements rendered directly in the browser with full settings control. |
| **Zero-config Serving** | The Vue 3 + TypeScript front-end is bundled into the JAR at build time and served from the addon's own HTTP server. |

</div>

<br>

<div align="center">
  <h1>Architecture</h1>
</div>

<div align="center">

```
 ┌──────────────┐      HTTP / static files       ┌──────────────────┐
 │              │ ◄─────────────────────────────  │                  │
 │   Vue 3 SPA  │                                │  NanoHTTPD       │
 │  (Vite + TS) │      WebSocket (port 8080)      │  HTTP + WS       │
 │              │ ◄════════════════════════════►  │                  │
 └──────────────┘    bi-directional messages      └────────┬─────────┘
                                                                     │
                                              ┌─────────────────────┘
                                              │
                                    ┌─────────▼─────────┐
                                    │  Meteor Client     │
                                    │  Module / Settings │
                                    │  HUD System        │
                                    └───────────────────┘
```

</div>

<div align="center">
  <p>The Java backend discovers <em>all</em> modules, settings, and HUD elements at runtime via reflection — no manual registration required. The Vue front-end receives a full state snapshot on connect, then streams deltas for instant responsiveness.</p>
</div>

<br>

<div align="center">
  <h1>Quick Start</h1>
</div>

<div align="center">

| Step | Instructions |
|:---:|:---|
| **1. Requirements** | • Java 25+<br>• Minecraft 26.2 with Fabric Loader 0.19.3+<br>• Meteor Client 26.2+ |
| **2. Install** | Download the latest `.jar` from [Releases](https://github.com/MCDxAI/meteor-client-webgui/releases) and drop it into `.minecraft/mods/`. |
| **3. Launch** | Start Minecraft with your Fabric profile. Open Meteor GUI (**Right Shift**) → **WebGUI** tab. |
| **4. Configure** | Set **Host** (default `127.0.0.1`) and **Port** (default `8080`). Enable **Auto Start** if desired. Click **Start Server**. |
| **5. Open** | Visit `http://127.0.0.1:8080` in any browser on the same machine. |

</div>

<br>

<div align="center">
  <h1>WebSocket Protocol</h1>
</div>

<div align="center">

Messages are JSON objects: `{ "type": "...", "data": { … }, "id": "optional-request-id" }`

</div>

<div align="center">

| Direction | Type | Description |
|:---:|:---|:---|
| Server → Client | `initial.state` | Full module/settings snapshot sent on connection |
| Server → Client | `module.state.changed` | Module was toggled in-game |
| Server → Client | `setting.value.changed` | A setting was modified in-game |
| Server → Client | `registry.data` | Paginated block/item/entity/potion registry data |
| Server → Client | `hud.preview.update` | Live HUD text snapshot |
| Server → Client | `favorites.state.changed` | Favorites list updated |
| Client → Server | `module.toggle` | Toggle a module on/off |
| Client → Server | `setting.update` | Change a setting value |
| Client → Server | `registry.request` | Request paginated registry data |
| Client → Server | `hud.toggle` | Toggle HUD element visibility |
| Client → Server | `favorites.update` | Update favorites list |

</div>

<br>

<div align="center">
  <h1>Supported Setting Types</h1>
</div>

<div align="center">

| Category | Types |
|:---|:---|
| **Primitives** | Bool, Int, Double, String, StringList, Enum |
| **Visual** | Color, ColorList, Keybind, FontFace |
| **Spatial** | BlockPos, Vector3d |
| **Registry** | BlockList, ItemList, EntityTypeList, Potion, StatusEffectAmplifierMap, RegistryValue |
| **Lists** | ModuleList, EnchantmentList, ParticleTypeList, SoundEventList, StatusEffectList, StorageBlockList, PacketList, ScreenHandlerList, GenericList |
| **Fallback** | Generic (auto-renders any unrecognized type) |

</div>

<br>

<div align="center">
  <h1>Development</h1>
</div>

<div align="center">

| Task | Command |
|:---|:---|
| Build the addon JAR (includes front-end) | `./gradlew build` |
| Run in Minecraft dev environment | `./gradlew runClient` |
| Run tests | `./gradlew test` |
| Clean all build artifacts | `./gradlew clean` |
| Install WebUI deps | `cd webui && npm install` |
| Dev server with hot-reload (port 3000) | `cd webui && npm run dev` |
| Production front-end build | `cd webui && npm run build` |

</div>

<div align="center">
  <p><strong>Dev mode:</strong> Run <code>npm run dev</code> in <code>webui/</code>. Vite serves on port 3000 and proxies WebSocket traffic to the in-game server on port 8080. Changes to Vue components reflect instantly.</p>
</div>

<div align="center">
  <p><strong>Production mode:</strong> <code>./gradlew build</code> compiles the Vue front-end, copies it into <code>src/main/resources/webui/</code>, and bundles everything into a single JAR. The addon serves both static files and the WebSocket endpoint on the configured port.</p>
</div>

<br>

<div align="center">
  <h1>Project Structure</h1>
</div>

<div align="center">

```
src/main/java/com/cope/meteorwebgui/
├── MeteorWebGUIAddon.java            # Entry point, server lifecycle
├── server/                           # HTTP + WebSocket servers (NanoHTTPD)
├── mapping/                          # Module, HUD, Settings, Registry discovery
├── events/                           # Real-time event broadcasting
├── protocol/                         # WSMessage model + MessageType enum
├── systems/                          # Persistent config (host, port, auto-start)
├── gui/                              # In-game WebGUI tab
├── hud/                              # HUD preview service + snapshot models
├── mixin/                            # HUD lifecycle + rendering hooks
└── util/                             # Utility classes

webui/src/
├── main.ts                           # App entry point
├── App.vue                           # Root component
├── stores/                           # Pinia stores (modules, websocket, hud)
├── components/
│   ├── ModuleList.vue                # Category-organized module list
│   ├── ModuleCard.vue                # Individual module card
│   ├── ModuleCardCompact.vue         # Compact card view
│   ├── ModuleSettingsDialog.vue      # Modal for module settings
│   ├── SettingsPanel.vue             # Settings display + type routing
│   ├── hud/                          # HUD dashboard + settings
│   ├── layout/                       # Layout components
│   ├── modals/                       # Modal dialogs
│   ├── ui/                           # Reusable UI primitives
│   └── settings/                     # 21 type-specific setting components
```

</div>

<br>

<div align="center">
  <h1>Tech Stack</h1>
</div>

<div align="center">

| Layer | Technology |
|:---|:---|
| **Game mod** | Fabric 0.19.3 · Minecraft 26.2 · Meteor Client 26.2 |
| **Server** | NanoHTTPD 2.3.1 (HTTP + WebSocket) · Gson 2.11.0 |
| **Front-end** | Vue 3.5 · Pinia 2.3 · TypeScript 5.7 · Vite 6.0 |
| **Build** | Gradle (Fabric Loom) · npm · CI via GitHub Actions |

</div>

<br>

<div align="center">
  <h1>Adding a New Setting Type</h1>
</div>

<div align="center">

1. Add type detection in `SettingsReflector.detectSettingType()`
2. Implement value extraction in `SettingsReflector.getSettingValue()`
3. Implement value writing in `SettingsReflector.setSettingValue()`
4. Add enum value in `SettingType`
5. Create a Vue component in `webui/src/components/settings/`
6. Register the component in `SettingsPanel.vue`

</div>

<br>

<div align="center">
  <h1>License</h1>
</div>

<div align="center">
  <p>This project is provided as-is. See the repository for license details.</p>
</div>
