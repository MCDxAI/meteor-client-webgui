<div align="center">
  <h1>Meteor WebGUI</h1>
  <img width="256" height="256" alt="icon" src="https://github.com/user-attachments/assets/273769c8-c51d-4294-9b2b-cddbac3e91ba" />
  <p>Real-time, bi-directional web interface for every Meteor Client module and setting.</p>
</div>

<div align="center">
  <img src="https://img.shields.io/badge/Status-Preview-purple?style=flat">
  <img src="https://img.shields.io/badge/Minecraft-1.21.10-0ea5e9?style=flat">
  <img src="https://img.shields.io/badge/Fabric%20Loader-0.17.3+-f59e0b?style=flat">
  <img src="https://img.shields.io/badge/Meteor%20Client-1.21.10--32-ec4899?style=flat">
</div>

<div align="center">
  <p><strong>Full control of Meteor Client from within the browser</strong></p>
</div>

<div align="center">
  <h1>Feature Highlights</h1>
</div>

<div align="center">
<table>
  <tr>
    <th>Capability</th>
    <th>Details</th>
  </tr>
  <tr>
    <td>Bi-directional sync</td>
    <td>NanoHTTPD-powered WebSocket channel mirrors module toggles & setting edits in real time.</td>
  </tr>
  <tr>
    <td>Automatic catalog</td>
    <td>Dynamically maps every Meteor Client module (plus installed addons) into category groups & favorites.</td>
  </tr>
  <tr>
    <td>Type-aware settings</td>
    <td>UI components adapt to 30+ setting types including Bool, Int, Double, String, Enum, Color, Keybind, Font, Potion, List types (blocks, items, entities), and Map types with full validation and type-specific controls.</td>
  </tr>
  <tr>
    <td>Registry streaming</td>
    <td>Server streams block/item/entity registry pages on demand so advanced list editors have the data they need, without bogging down Meteor or the WebGUI</td>
  </tr>
  <tr>
    <td>Fully Bundled</td>
    <td>Gradle builds the Vue 3 + TypeScript front-end, where it's served from within the addon
  </tr>
  <tr>
    <td>Real-time HUD display</td>
    <td>Live text previews of active HUD elements with automatic updates, state synchronization, and full settings control from the browser.</td>
  </tr>
</table>
</div>

<div align="center">
  <h1>Quick Start</h1>
</div>

<div align="center">

| Step | Instructions |
|:---:|:---|
| **1. Requirements** | • Java 21+<br>• Node.js 18+ (only for web UI development)<br>• Minecraft 1.21.10 with Fabric Loader 0.17.3+<br>• Meteor Client 1.21.10-32 |
| **2. Download the addon** | Download the latest `.jar` release from the [GitHub Releases](https://github.com/GhostTypes/meteor-client-webgui/releases) page. |
| **3. Install into Minecraft** | 1. Copy the downloaded `.jar` file to `.minecraft/mods/`.<br>2. Ensure the required Meteor Client version is installed.<br>3. Launch Minecraft with your Fabric profile. |
| **4. Start the WebGUI server** | 1. Press **Right Shift** to open the Meteor GUI.<br>2. Open the **WebGUI** tab.<br>3. Configure **Host** (default `127.0.0.1`) and **Port** (default `8080`) plus **Auto Start** if desired.<br>4. Click **Start Server**.<br><br>Log output:<br><pre>[Meteor WebGUI] Starting WebGUI server on 127.0.0.1:8080<br>[Meteor WebGUI] Event monitoring started for N modules<br>[Meteor WebGUI] Access the WebGUI at: http://127.0.0.1:8080</pre> |
| **5. Open the interface** | **Production** – Visit `http://127.0.0.1:8080` (or your configured host/port). Static assets and the `/ws` WebSocket endpoint are served by the addon itself.<br><br>**Development** – For hot reloading, run:<br><pre>cd webui<br>npm install<br>npm run dev</pre><br>Visit `http://localhost:3000`. Vite proxies `/ws` to `localhost:8080`, so the development UI still talks to the in-game server. |

</div>

<div align="center">
  <h1>Development Workflow</h1>
</div>

<div align="center">

| Component | Commands / Actions |
|:---:|:---|
| **Java / Fabric side** | • `./gradlew build` – Builds the addon and packages the latest WebUI.<br>• `./gradlew runClient` – Launches a Fabric development client with the addon loaded.<br>• `./gradlew test` – Runs the JUnit test suite.<br>• `./gradlew clean` – Removes generated class files and packaged WebUI artifacts. |
| **WebGUI (Vue 3 + Vite)** | • `npm install` – Install dependencies in `webui/`.<br>• `npm run dev` – Hot-reload development server on port 3000 (with `/ws` proxying).<br>• `npm run build` – Generates the static bundle consumed by Gradle.<br>• `npm run preview` – Serve the production bundle locally for smoke tests. |

</div>
