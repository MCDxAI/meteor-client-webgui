<div align="center">
  <h1>Meteor WebGUI</h1>
  <img width="196" height="196" alt="Meteor WebGUI icon" src="https://github.com/user-attachments/assets/273769c8-c51d-4294-9b2b-cddbac3e91ba" />
  <p><strong>Control every Meteor Client module and setting from your web browser.</strong></p>
</div>

<div align="center">
  <img src="https://img.shields.io/badge/Status-Preview-purple?style=flat">
  <img src="https://img.shields.io/badge/Minecraft-26.2-0ea5e9?style=flat">
  <img src="https://img.shields.io/badge/Fabric%20Loader-0.19.3+-f59e0b?style=flat">
  <img src="https://img.shields.io/badge/Meteor%20Client-26.2-ec4899?style=flat">
  <img src="https://img.shields.io/badge/Java-25+-orange?style=flat">
</div>

## What is Meteor WebGUI?

Meteor WebGUI is a Meteor Client addon that puts a live control panel in your web browser. Toggle modules and edit settings in the browser, and the game updates instantly. Change something in-game, and the browser updates too.

The addon discovers every module, every setting, and every HUD element at runtime — from Meteor Client and from every addon you have installed. There is no per-module setup and nothing to register.

| Feature | What you get |
|:---|:---|
| **All modules, all addons** | Browse, search, and toggle every module, grouped by category or filtered by favorites. |
| **Full settings editor** | 30+ setting types, each with a matching control: toggles, sliders, color pickers, dropdowns, keybind capture, list editors, and more. See [supported setting types](docs/SETTINGS-TYPES.md). |
| **Two-way live sync** | Changes in the browser appear in-game instantly. Changes in-game appear in the browser instantly. |
| **Registry data on demand** | Block, item, entity, and potion lists load only when a setting needs them, so the first connection stays fast. |
| **Live HUD preview** | The browser shows what your active HUD elements render, refreshed roughly every 200 ms, with full settings control. |
| **Zero extra setup** | The web interface ships inside the addon JAR. No second download, no web server to install. |

## Requirements

| Requirement | Version |
|:---|:---|
| Java | 25 or newer |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 or newer |
| Meteor Client | 26.2 or newer |
| Browser | Any modern browser |

## Install

1. Download `meteor-webgui-0.5.0.jar` from the [Releases page](https://github.com/MCDxAI/meteor-client-webgui/releases).
2. Copy the JAR into your `.minecraft/mods` folder.
3. Start Minecraft with your Fabric profile.

## Start the server

1. Press **Right Shift** to open the Meteor GUI.
2. Open the **WebGUI** tab.
3. Click **Start Server**.
4. Click **Open in Browser**. You can also visit `http://127.0.0.1:8080` yourself.

The tab shows the server status while the game runs. The server uses two settings by default:

| Setting | Default | Meaning |
|:---|:---|:---|
| Host | `127.0.0.1` | The network address the server binds to. The default limits access to your own machine. |
| Port | `8080` | The port for the web page and the WebSocket connection. Range: 1024–65535. |

The tab also has an **Auto Start** setting. Enable it, and the server starts every time Minecraft loads. Your choices persist with your Meteor Client config.

### Security

The server has no login and no password. Anyone who can reach the port can control your client. The default host `127.0.0.1` keeps the server private to your machine. Only change the host if you trust every device on that network.

## How it works

The addon runs a small web server (NanoHTTPD) inside your game. It serves the bundled web interface as static files and keeps a live WebSocket connection open on the same port.

```
┌──────────────┐   HTTP: web page + assets   ┌──────────────────┐
│              │ ◄──────────────────────────  │  Your game       │
│   Browser    │                              │  Meteor WebGUI   │
│  (Vue app)   │ ◄═════ WebSocket /ws ═════►  │  addon server    │
│              │     live two-way messages    │                  │
└──────────────┘                              └────────┬─────────┘
                                                       │
                                             ┌─────────▼─────────┐
                                             │  Meteor Client    │
                                             │  modules, settings│
                                             │  and HUD elements │
                                             └───────────────────┘
```

On connect, the browser receives a full snapshot of your modules, settings, HUD elements, and favorites. After that, both sides send only changes. The Java side reads and writes settings through Meteor's own APIs. Everything you do in the browser behaves exactly as if you did it in the Meteor GUI.

Want to script against the addon or build your own interface? Read the [WebSocket protocol reference](docs/WEBSOCKET-PROTOCOL.md).

## Documentation

| Document | What it covers |
|:---|:---|
| [WebSocket protocol](docs/WEBSOCKET-PROTOCOL.md) | Every message type, payload shape, and a minimal client example. For custom clients and scripts. |
| [Supported setting types](docs/SETTINGS-TYPES.md) | Every setting type the web interface renders, its control, and its JSON value shape. Includes a guide for adding new types. |

## Build from source

You need a JDK 25 and Node.js 18 or newer with npm. Node is required because the build compiles the front-end.

1. Clone the repository.
2. Run `./gradlew build`.
3. Take the JAR from `build/libs/meteor-webgui-0.5.0.jar`.

One command builds both parts. Gradle installs the front-end dependencies, compiles the Vue app, and bundles it into the JAR.

Other useful tasks:

| Task | Command |
|:---|:---|
| Run the game with the addon in a dev environment | `./gradlew runClient` |
| Run the Java tests | `./gradlew test` |
| Remove all build artifacts | `./gradlew clean` |

### Front-end development

The web interface is a Vue 3 app in `webui/`. During development, Vite serves it with hot reload and proxies WebSocket traffic to the game.

1. Run `./gradlew runClient` to start the game with the addon.
2. Start the server from the WebGUI tab in-game.
3. Change to the `webui` folder: `cd webui`.
4. Run `npm install` (first time only).
5. Run `npm run dev`.
6. Open `http://localhost:3000` in your browser.

Changes to Vue components now appear without a reload or a game restart.

## Project structure

Two codebases live in this repository: the Java addon and the Vue front-end.

```
src/main/java/com/cope/meteorwebgui/
├── MeteorWebGUIAddon.java         # Addon entry point; server start/stop
├── server/                        # HTTP + WebSocket server (NanoHTTPD)
│   ├── MeteorHTTPServer.java      # Serves the bundled web interface; accepts /ws upgrades
│   ├── MeteorWebServer.java       # Server lifecycle; broadcasts to all clients
│   ├── MeteorWebSocket.java       # Per-connection message handling
│   └── MeteorWebSocketHandler.java # Connection pool; broadcast helpers
├── mapping/                       # Runtime discovery
│   ├── ModuleMapper.java          # All modules, grouped by category
│   ├── HudMapper.java             # All HUD elements
│   ├── SettingsReflector.java     # Reads and writes any setting by type
│   ├── SettingType.java           # Setting type enum
│   └── RegistryProvider.java      # Block, item, entity, and potion registry data
├── events/EventMonitor.java       # Watches the game; broadcasts module and setting changes
├── protocol/                      # WSMessage model + MessageType enum
├── systems/WebGUIConfig.java      # Host, port, and auto-start settings (persisted)
├── gui/WebGUITab.java             # The in-game WebGUI tab
├── hud/                           # HUD preview snapshots, ~200 ms cadence
├── mixin/                         # HUD lifecycle and render hooks
└── util/BrowserHelper.java        # Opens the default browser

webui/src/
├── main.ts                        # Front-end entry point
├── App.vue                        # Root component
├── stores/                        # Pinia stores (modules, websocket, hud)
└── components/
    ├── ModuleList.vue             # Category list with search
    ├── ModuleCard.vue             # Module card
    ├── ModuleCardCompact.vue      # Compact card variant
    ├── ModuleSettingsDialog.vue   # Settings modal for one module
    ├── ModuleToolbar.vue          # Toolbar controls
    ├── SettingsPanel.vue          # Routes each setting type to its component
    ├── hud/                       # HUD dashboard and settings dialog
    └── settings/                  # 21 setting-type components
```

## Tech stack

| Layer | Technology |
|:---|:---|
| Game addon | Fabric · Minecraft 26.2 · Meteor Client 26.2 |
| Server | NanoHTTPD 2.3.1 (HTTP + WebSocket) · Gson 2.11.0 |
| Front-end | Vue 3.5 · Pinia 2.3 · TypeScript 5.7 · Vite 6.0 |
| Build | Gradle with Fabric Loom · npm · GitHub Actions |
