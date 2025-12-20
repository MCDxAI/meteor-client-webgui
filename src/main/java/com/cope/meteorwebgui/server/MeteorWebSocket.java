package com.cope.meteorwebgui.server;

import com.cope.meteorwebgui.mapping.HudMapper;
import com.cope.meteorwebgui.mapping.HudMapper;
import com.cope.meteorwebgui.mapping.ModuleMapper;
import com.cope.meteorwebgui.mapping.RegistryProvider;
import com.cope.meteorwebgui.mapping.SettingsReflector;
import com.cope.meteorwebgui.protocol.MessageType;
import com.cope.meteorwebgui.protocol.WSMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fi.iki.elonen.NanoWSD;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * WebSocket implementation for handling real-time communication with WebUI clients.
 * Handles module toggles, setting updates, and broadcasts state changes.
 */
public class MeteorWebSocket extends NanoWSD.WebSocket {
    private static final Logger LOG = LoggerFactory.getLogger("WebGUI WebSocket");
    private static final Gson GSON = new Gson();

    public MeteorWebSocket(NanoWSD.IHTTPSession handshakeRequest) {
        super(handshakeRequest);
    }

    @Override
    protected void onOpen() {
        LOG.info("New WebSocket connection from {}", getHandshakeRequest().getRemoteIpAddress());

        // Send initial state to client
        try {
            JsonObject initialData = new JsonObject();
            initialData.add("modules", ModuleMapper.mapAllModulesByCategory());
            initialData.add("hud", HudMapper.mapHudState());
            initialData.add("favorites", ModuleMapper.getFavorites());

            // Registry data is now loaded on-demand via REGISTRY_REQUEST
            WSMessage message = new WSMessage(MessageType.INITIAL_STATE, initialData);
            send(GSON.toJson(message));

            LOG.info("Sent initial state to client (registries will be loaded on-demand)");
        } catch (Exception e) {
            LOG.error("Failed to send initial state: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        LOG.info("WebSocket connection closed: {} (code: {}, remote: {})", reason, code, initiatedByRemote);
    }

    @Override
    protected void onMessage(NanoWSD.WebSocketFrame frame) {
        try {
            String message = frame.getTextPayload();
            WSMessage wsMessage = GSON.fromJson(message, WSMessage.class);
            MessageType type = wsMessage.getMessageType();

            if (type == null) {
                sendError("Unknown message type: " + wsMessage.getType());
                return;
            }

            LOG.debug("Received message type: {}", type);

            switch (type) {
                case MODULE_TOGGLE -> handleModuleToggle(wsMessage);
                case MODULE_LIST -> handleModuleList(wsMessage);
                case SETTING_UPDATE -> handleSettingUpdate(wsMessage);
                case SETTING_GET -> handleSettingGet(wsMessage);
                case FAVORITES_UPDATE -> handleFavoritesUpdate(wsMessage);
                case REGISTRY_REQUEST -> handleRegistryRequest(wsMessage);
                case HUD_TOGGLE -> handleHudToggle(wsMessage);
                case PING -> handlePing(wsMessage);
                default -> sendError("Unsupported message type: " + type);
            }

        } catch (Exception e) {
            LOG.error("Error handling message: {}", e.getMessage(), e);
            sendError("Failed to process message: " + e.getMessage());
        }
    }

    @Override
    protected void onPong(NanoWSD.WebSocketFrame pong) {
        // Handle pong if needed
    }

    @Override
    protected void onException(IOException exception) {
        LOG.error("WebSocket exception: {}", exception.getMessage(), exception);
    }

    private void handleModuleToggle(WSMessage message) {
        try {
            JsonObject data = message.getData().getAsJsonObject();
            String moduleName = data.get("moduleName").getAsString();

            Module module = Modules.get().get(moduleName);
            if (module == null) {
                sendError("Module not found: " + moduleName, message.getId());
                return;
            }

            module.toggle();

            // Send success response
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("moduleName", moduleName);
            response.addProperty("active", module.isActive());

            send(GSON.toJson(new WSMessage("response", response, message.getId())));

            LOG.info("Toggled module: {} -> {}", moduleName, module.isActive());

        } catch (Exception e) {
            LOG.error("Failed to toggle module: {}", e.getMessage(), e);
            sendError("Failed to toggle module: " + e.getMessage(), message.getId());
        }
    }

    private void handleModuleList(WSMessage message) {
        try {
            JsonObject data = ModuleMapper.mapModulesLightweight();

            JsonObject response = new JsonObject();
            response.add("modules", data);

            send(GSON.toJson(new WSMessage("response", response, message.getId())));

        } catch (Exception e) {
            LOG.error("Failed to get module list: {}", e.getMessage(), e);
            sendError("Failed to get module list: " + e.getMessage(), message.getId());
        }
    }

    private void handleSettingUpdate(WSMessage message) {
        try {
            JsonObject data = message.getData().getAsJsonObject();
            String moduleName = data.get("moduleName").getAsString();
            String settingName = data.get("settingName").getAsString();
            JsonObject value = data.get("value").getAsJsonObject();

            Module module = Modules.get().get(moduleName);
            if (module != null) {
                Setting<?> setting = findSetting(module, settingName);
                if (setting == null) {
                    sendError("Setting not found: " + settingName, message.getId());
                    return;
                }

                boolean success = SettingsReflector.setSettingValue(setting, value);

                JsonObject response = new JsonObject();
                response.addProperty("success", success);
                response.addProperty("moduleName", moduleName);
                response.addProperty("settingName", settingName);

                send(GSON.toJson(new WSMessage("response", response, message.getId())));

                if (success) {
                    LOG.info("Updated setting: {}.{} = {}", moduleName, settingName, value);
                }
                return;
            }

            HudElement hudElement = HudMapper.findElement(moduleName);
            if (hudElement == null) {
                sendError("Config target not found: " + moduleName, message.getId());
                return;
            }

            Setting<?> setting = findHudSetting(hudElement, settingName);
            if (setting == null) {
                sendError("HUD setting not found: " + settingName, message.getId());
                return;
            }

            boolean success = SettingsReflector.setSettingValue(setting, value);

            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("elementName", HudMapper.getElementIdentifier(hudElement));
            response.addProperty("settingName", settingName);

            send(GSON.toJson(new WSMessage("response", response, message.getId())));

            if (success) {
                LOG.info("Updated HUD setting: {}.{} = {}", hudElement.info != null ? hudElement.info.name : hudElement.getClass().getSimpleName(), settingName, value);
            }

        } catch (Exception e) {
            LOG.error("Failed to update setting: {}", e.getMessage(), e);
            sendError("Failed to update setting: " + e.getMessage(), message.getId());
        }
    }

    private void handleSettingGet(WSMessage message) {
        try {
            JsonObject data = message.getData().getAsJsonObject();
            String moduleName = data.get("moduleName").getAsString();
            String settingName = data.get("settingName").getAsString();

            Module module = Modules.get().get(moduleName);
            if (module != null) {
                Setting<?> setting = findSetting(module, settingName);
                if (setting == null) {
                    sendError("Setting not found: " + settingName, message.getId());
                    return;
                }

                JsonObject response = new JsonObject();
                response.add("setting", SettingsReflector.getSettingMetadata(setting));

                send(GSON.toJson(new WSMessage("response", response, message.getId())));
                return;
            }

            HudElement hudElement = HudMapper.findElement(moduleName);
            if (hudElement == null) {
                sendError("Config target not found: " + moduleName, message.getId());
                return;
            }

            Setting<?> setting = findHudSetting(hudElement, settingName);
            if (setting == null) {
                sendError("HUD setting not found: " + settingName, message.getId());
                return;
            }

            JsonObject response = new JsonObject();
            response.add("setting", SettingsReflector.getSettingMetadata(setting));

            send(GSON.toJson(new WSMessage("response", response, message.getId())));

        } catch (Exception e) {
            LOG.error("Failed to get setting: {}", e.getMessage(), e);
            sendError("Failed to get setting: " + e.getMessage(), message.getId());
        }
    }

    private void handleHudToggle(WSMessage message) {
        try {
            JsonObject data = message.getData().getAsJsonObject();
            String elementName = data.get("elementName").getAsString();

            HudElement element = HudMapper.findElement(elementName);
            if (element == null) {
                sendError("HUD element not found: " + elementName, message.getId());
                return;
            }

            element.toggle();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("elementName", HudMapper.getElementIdentifier(element));
            response.addProperty("active", element.isActive());

            send(GSON.toJson(new WSMessage("response", response, message.getId())));

            LOG.info("Toggled HUD element: {} -> {}", elementName, element.isActive());
        } catch (Exception e) {
            LOG.error("Failed to toggle HUD element: {}", e.getMessage(), e);
            sendError("Failed to toggle HUD element: " + e.getMessage(), message.getId());
        }
    }

    private void handleFavoritesUpdate(WSMessage message) {
        try {
            JsonObject data = message.getData().getAsJsonObject();
            java.util.List<String> favorites = new java.util.ArrayList<>();

            if (data.has("favorites") && data.get("favorites").isJsonArray()) {
                for (var element : data.getAsJsonArray("favorites")) {
                    favorites.add(element.getAsString());
                }
            }

            ModuleMapper.setFavorites(favorites);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.add("favorites", ModuleMapper.getFavorites());

            send(GSON.toJson(new WSMessage("response", response, message.getId())));

            LOG.info("Updated favorites from WebUI: {} modules", favorites.size());
        } catch (Exception e) {
            LOG.error("Failed to update favorites: {}", e.getMessage(), e);
            sendError("Failed to update favorites: " + e.getMessage(), message.getId());
        }
    }

    private void handlePing(WSMessage message) {
        try {
            send(GSON.toJson(new WSMessage(MessageType.PONG, new JsonObject(), message.getId())));
        } catch (IOException e) {
            LOG.error("Failed to send pong: {}", e.getMessage(), e);
        }
    }

    private void handleRegistryRequest(WSMessage message) {
        try {
            JsonObject data = message.getData().getAsJsonObject();
            String registryType = data.get("registry").getAsString();

            LOG.debug("Registry request received for: {}", registryType);

            JsonObject response = new JsonObject();
            switch (registryType) {
                case "blocks" -> {
                    response.add("blocks", RegistryProvider.getAllBlocks());
                    LOG.info("Sent blocks registry to client");
                }
                case "items" -> {
                    response.add("items", RegistryProvider.getAllItems());
                    LOG.info("Sent items registry to client");
                }
                case "entities" -> {
                    response.add("entities", RegistryProvider.getAllEntityTypes());
                    LOG.info("Sent entities registry to client");
                }
                case "statusEffects" -> {
                    response.add("statusEffects", RegistryProvider.getAllStatusEffects());
                    LOG.info("Sent statusEffects registry to client");
                }
                case "potions" -> {
                    response.add("potions", RegistryProvider.getAllPotions());
                    LOG.info("Sent potions registry to client");
                }
                case "modules" -> {
                    response.add("modules", RegistryProvider.getAllModules());
                    LOG.info("Sent modules registry to client");
                }
                default -> {
                    sendError("Unknown registry type: " + registryType, message.getId());
                    return;
                }
            }

            response.addProperty("registryType", registryType);
            send(GSON.toJson(new WSMessage(MessageType.REGISTRY_DATA, response, message.getId())));

        } catch (Exception e) {
            LOG.error("Failed to send registry: {}", e.getMessage(), e);
            sendError("Failed to load registry: " + e.getMessage(), message.getId());
        }
    }

    private Setting<?> findSetting(Module module, String settingName) {
        for (SettingGroup group : module.settings) {
            for (Setting<?> setting : group) {
                if (setting.name.equals(settingName)) {
                    return setting;
                }
            }
        }
        return null;
    }

    private Setting<?> findHudSetting(HudElement element, String settingName) {
        for (SettingGroup group : element.settings) {
            for (Setting<?> setting : group) {
                if (setting.name.equals(settingName)) {
                    return setting;
                }
            }
        }
        return null;
    }

    private void sendError(String error) {
        sendError(error, null);
    }

    private void sendError(String error, String requestId) {
        try {
            JsonObject errorData = new JsonObject();
            errorData.addProperty("error", error);
            send(GSON.toJson(new WSMessage(MessageType.ERROR, errorData, requestId)));
        } catch (IOException e) {
            LOG.error("Failed to send error message: {}", e.getMessage(), e);
        }
    }
}
