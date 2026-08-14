# WebSocket protocol

Back to the [README](../README.md).

TLDR: Meteor WebGUI serves its web interface over HTTP and keeps one WebSocket connection open for live data. Every message is a JSON object with a `type` and a `data` field. This page lists every message, its payload, and a minimal client example. Use it to script the addon or build your own interface.

## Connect

| Item | Value |
|:---|:---|
| URL | `ws://<host>:<port>/ws` — default `ws://127.0.0.1:8080/ws` |
| Subprotocol | None |
| Authentication | None — see the security note below |

Facts to know:

- The WebSocket endpoint lives at the `/ws` path on the same host and port as the web page. Connections to any other path are rejected.
- The server serves static files for `GET` requests only. It answers other HTTP methods with `405 Method Not Allowed`.
- If you serve the web interface behind HTTPS yourself, connect with `wss:` instead of `ws:`.
- There is **no authentication**. Anyone who can reach the port can read and change your client. Keep the default host `127.0.0.1` unless you trust your network.

## Message envelope

Every message — in both directions — uses the same three fields:

```json
{
  "type": "module.toggle",
  "data": { "moduleName": "fullbright" },
  "id": "req-42"
}
```

| Field | Type | Required | Meaning |
|:---|:---|:---|:---|
| `type` | string | Yes | The message name, for example `module.toggle`. Unknown types get an `error` reply. |
| `data` | object | Yes | The payload. May be empty (`{}`) when there is nothing to send. |
| `id` | string | No | Your request ID. The server copies it into the matching reply. |

### Replies

Commands you send get one of two replies:

- **`"type": "response"`** — success. The payload depends on the command. See each command below.
- **`"type": "error"`** — failure. The payload is `{ "error": "<reason>" }`.

Both replies echo your `id` when you sent one. Replies go only to the connection that asked.

## Connection lifecycle

1. You connect to `/ws`.
2. The server immediately sends [`initial.state`](#initialstate) with your full module, HUD, and favorites snapshot.
3. You request registry data with [`registry.request`](#registryrequest) when a setting needs it. Registries are never pushed unsolicited.
4. The server pushes change events (module toggles, setting changes, HUD updates) as they happen in-game.
5. You can send [`ping`](#ping) at any time. The server answers with `pong`.
6. The bundled web interface reconnects every 3 seconds after a drop. If you write your own client, handle reconnection yourself. After a reconnect you receive a fresh `initial.state`.

## Server-to-client messages

| Type | Sent when |
|:---|:---|
| [`initial.state`](#initialstate) | A client connects |
| [`module.state.changed`](#modulestatechanged) | A module was toggled in-game |
| [`setting.value.changed`](#settingvaluechanged) | A module setting changed in-game |
| [`favorites.state.changed`](#favoritesstatechanged) | The Meteor favorites list changed |
| [`registry.data`](#registrydata) | Reply to `registry.request` |
| [`hud.preview.update`](#hudpreviewupdate) | Active HUD elements rendered new text (~200 ms cadence, changed elements only) |
| [`hud.state.changed`](#hudstatechanged) | A HUD element was toggled or moved in-game |
| [`hud.setting.value.changed`](#hudsettingvaluechanged) | A HUD element setting changed in-game |
| [`error`](#error) | A request failed, or a message could not be parsed |

### `initial.state`

Sent once, right after the connection opens.

```json
{
  "type": "initial.state",
  "data": {
    "modules": { "<category>": [ "<module object>" ] },
    "hud": { "elements": [ "<element object>" ], "previews": [ "<snapshot object>" ] },
    "favorites": [ "killaura", "fullbright" ]
  }
}
```

Module object:

```json
{
  "name": "fullbright",
  "title": "Fullbright",
  "description": "Removes darkness from the world.",
  "category": "Render",
  "active": false,
  "addon": "Meteor Client",
  "settingGroups": [
    { "name": "general", "settings": [ "<setting metadata object>" ] }
  ]
}
```

HUD element objects use the same shape plus position and size fields (`x`, `y`, `width`, `height`) and a `group` field. Their `name` is an identifier of the form `hud:<element>#<instance>`. Setting metadata objects are documented in [SETTINGS-TYPES.md](SETTINGS-TYPES.md#setting-metadata).

### `module.state.changed`

```json
{ "type": "module.state.changed", "data": { "moduleName": "fullbright", "active": true } }
```

### `setting.value.changed`

```json
{
  "type": "setting.value.changed",
  "data": {
    "moduleName": "fullbright",
    "settingName": "use-potion",
    "value": { "value": true },
    "visibility": { "use-potion": true, "potion": false }
  }
}
```

`visibility` maps every setting name of that module to its current visible state. The bundled interface uses it to show and hide conditional settings.

### `favorites.state.changed`

```json
{ "type": "favorites.state.changed", "data": { "favorites": [ "killaura" ] } }
```

### `registry.data`

Reply to [`registry.request`](#registryrequest). The payload contains `registryType` plus one data key that matches the request.

```json
{
  "type": "registry.data",
  "data": {
    "registryType": "blocks",
    "blocks": {
      "blocks": [ { "id": "minecraft:stone", "namespace": "minecraft" } ],
      "byNamespace": { "minecraft": [ "minecraft:stone" ] }
    }
  }
}
```

`entities`, `statusEffects`, and `potions` use flat arrays of `{ id, namespace }` objects. `items` matches the `blocks` shape. `modules` returns `byCategory`, a map of category name to lightweight module objects.

### `hud.preview.update`

Sent about every 200 ms, but only with elements whose rendered text changed since the last send.

```json
{
  "type": "hud.preview.update",
  "data": { "elements": [ { "name": "hud:compass#12345", "lines": [ "<text line object>" ] } ] }
}
```

Each text line carries `text`, `x`, `y`, `color`, `shadow`, and `scale`.

### `hud.state.changed`

```json
{
  "type": "hud.state.changed",
  "data": { "elementName": "hud:compass#12345", "active": true, "x": 4, "y": 4, "width": 80, "height": 12 }
}
```

### `hud.setting.value.changed`

```json
{
  "type": "hud.setting.value.changed",
  "data": { "elementName": "hud:compass#12345", "settingName": "text", "value": { "value": "Compass" } }
}
```

### `error`

```json
{ "type": "error", "data": { "error": "Module not found: fullbrigth" }, "id": "req-42" }
```

## Client-to-server messages

| Type | Purpose | Reply |
|:---|:---|:---|
| [`module.toggle`](#moduletoggle) | Toggle a module on or off | `response` |
| [`module.list`](#modulelist) | Request the lightweight module list | `response` |
| [`setting.update`](#settingupdate) | Change a module or HUD setting | `response` |
| [`setting.get`](#settingget) | Request metadata for one setting | `response` |
| [`favorites.update`](#favoritesupdate) | Replace the favorites list | `response` |
| [`registry.request`](#registryrequest) | Request registry data | `registry.data` |
| [`hud.toggle`](#hudtoggle) | Toggle a HUD element on or off | `response` |
| [`ping`](#ping) | Check that the connection is alive | `pong` |

Unknown or unsupported types get an `error` reply.

### `module.toggle`

Request:

```json
{ "type": "module.toggle", "data": { "moduleName": "fullbright" }, "id": "req-1" }
```

Reply:

```json
{ "type": "response", "data": { "success": true, "moduleName": "fullbright", "active": true }, "id": "req-1" }
```

The toggle also triggers a `module.state.changed` broadcast to every connected client, including you.

### `module.list`

Request:

```json
{ "type": "module.list", "data": {}, "id": "req-2" }
```

Reply:

```json
{ "type": "response", "data": { "modules": { "<category>": [ "<module object>" ] } }, "id": "req-2" }
```

The lightweight module objects contain `name`, `title`, `description`, `active`, and `addon` — but no `settingGroups`. Use `initial.state` or `setting.get` for settings.

### `setting.update`

Request:

```json
{
  "type": "setting.update",
  "data": {
    "moduleName": "fullbright",
    "settingName": "use-potion",
    "value": { "value": true }
  },
  "id": "req-3"
}
```

Reply:

```json
{ "type": "response", "data": { "success": true, "moduleName": "fullbright", "settingName": "use-potion" }, "id": "req-3" }
```

The server looks up `moduleName` as a module first. If no module matches, it tries HUD elements. For a HUD element, pass its identifier — for example `hud:compass#12345` — as `moduleName`, and the reply returns `elementName` instead of `moduleName`.

The `value` object's shape depends on the setting type. Every shape is listed in [SETTINGS-TYPES.md](SETTINGS-TYPES.md#type-reference). A successful update also triggers a `setting.value.changed` broadcast.

### `setting.get`

Request:

```json
{ "type": "setting.get", "data": { "moduleName": "fullbright", "settingName": "use-potion" }, "id": "req-4" }
```

Reply:

```json
{ "type": "response", "data": { "setting": "<setting metadata object>" }, "id": "req-4" }
```

The metadata object shape is documented in [SETTINGS-TYPES.md](SETTINGS-TYPES.md#setting-metadata). As with `setting.update`, a HUD element identifier works as the `moduleName`.

### `favorites.update`

Request:

```json
{ "type": "favorites.update", "data": { "favorites": [ "killaura", "fullbright" ] }, "id": "req-5" }
```

Reply:

```json
{ "type": "response", "data": { "success": true, "favorites": [ "killaura", "fullbright" ] }, "id": "req-5" }
```

The list replaces the Meteor favorites list completely. A successful update also triggers a `favorites.state.changed` broadcast.

### `registry.request`

Request:

```json
{ "type": "registry.request", "data": { "registry": "blocks" }, "id": "req-6" }
```

Valid `registry` values:

| Value | Contents |
|:---|:---|
| `blocks` | Every registered block, with a by-namespace index |
| `items` | Every registered item, with a by-namespace index |
| `entities` | Every registered entity type |
| `statusEffects` | Every registered status effect |
| `potions` | Every registered potion |
| `modules` | Every module, grouped by category |

The reply is a [`registry.data`](#registrydata) message. Unknown registry names get an `error` reply. The payloads can be large — request a registry only when you need it.

### `hud.toggle`

Request:

```json
{ "type": "hud.toggle", "data": { "elementName": "hud:compass#12345" }, "id": "req-7" }
```

Reply:

```json
{ "type": "response", "data": { "success": true, "elementName": "hud:compass#12345", "active": true }, "id": "req-7" }
```

The toggle also triggers an `hud.state.changed` broadcast.

### `ping`

Request:

```json
{ "type": "ping", "data": {}, "id": "req-8" }
```

Reply:

```json
{ "type": "pong", "data": {}, "id": "req-8" }
```

## Minimal client example

This Node.js script connects, waits for `initial.state`, toggles a module, and prints every broadcast:

```js
const socket = new WebSocket("ws://127.0.0.1:8080/ws");

socket.onopen = () => {
  socket.send(JSON.stringify({
    type: "module.toggle",
    data: { moduleName: "fullbright" },
    id: "req-1"
  }));
};

socket.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log(message.type, message.data);
};
```

The `WebSocket` API is the same in any browser. In Node.js, the global `WebSocket` exists on Node 22 and newer — on older versions, install the `ws` package.
