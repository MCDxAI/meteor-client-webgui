package com.cope.meteorwebgui.server;

import com.cope.meteorwebgui.mapping.HudMapper;
import com.cope.meteorwebgui.mapping.ModuleMapper;
import com.cope.meteorwebgui.protocol.MessageType;
import com.cope.meteorwebgui.protocol.WSMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fi.iki.elonen.NanoHTTPD;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main server class that manages both HTTP (static file serving) and WebSocket connections.
 * Provides a unified interface for starting/stopping the server and broadcasting messages.
 */
public class MeteorWebServer {
    private static final Logger LOG = LoggerFactory.getLogger("WebGUI Server");
    private static final Gson GSON = new Gson();

    private final String host;
    private final int port;
    private MeteorHTTPServer httpServer;
    private MeteorWebSocketHandler webSocketHandler;
    private boolean running = false;

    public MeteorWebServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Starts the HTTP/WebSocket server.
     */
    public void start() throws IOException {
        if (running) {
            LOG.warn("Server already running");
            return;
        }

        // Create WebSocket handler
        webSocketHandler = new MeteorWebSocketHandler();

        // Create and start HTTP server (which also handles WebSocket upgrades)
        httpServer = new MeteorHTTPServer(host, port, webSocketHandler);
        // Use 0 timeout for WebSocket connections (persistent connections don't need read timeout)
        httpServer.start(0, false);

        running = true;
        LOG.info("WebGUI server started on {}:{}", host, port);
        LOG.info("Open http://{}:{} in your browser", host, port);
    }

    /**
     * Stops the HTTP/WebSocket server.
     */
    public void shutdown() {
        if (!running) {
            return;
        }

        try {
            running = false;
            if (httpServer != null) {
                httpServer.stop();
            }
            LOG.info("WebGUI server stopped");
        } catch (Exception e) {
            LOG.error("Error stopping server: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast module state change to all connected clients.
     */
    public void broadcastModuleStateChange(Module module) {
        if (!running) return;

        try {
            JsonObject data = ModuleMapper.createModuleStateMessage(module);
            WSMessage message = new WSMessage(MessageType.MODULE_STATE_CHANGED, data);
            webSocketHandler.broadcast(GSON.toJson(message));

            LOG.debug("Broadcast module state: {} -> {}", module.name, module.isActive());
        } catch (Exception e) {
            LOG.error("Failed to broadcast module state: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast setting value change to all connected clients.
     */
    public void broadcastSettingChange(Module module, Setting<?> setting) {
        if (!running) return;

        try {
            JsonObject data = ModuleMapper.createSettingChangeMessage(module, setting);
            WSMessage message = new WSMessage(MessageType.SETTING_VALUE_CHANGED, data);
            webSocketHandler.broadcast(GSON.toJson(message));

            LOG.debug("Broadcast setting change: {}.{}", module.name, setting.name);
        } catch (Exception e) {
            LOG.error("Failed to broadcast setting change: {}", e.getMessage(), e);
        }
    }

    public void broadcastHudStateChange(HudElement element) {
        if (!running) return;
        try {
            JsonObject data = HudMapper.createHudStateMessage(element);
            WSMessage message = new WSMessage(MessageType.HUD_STATE_CHANGED, data);
            webSocketHandler.broadcast(GSON.toJson(message));
            LOG.debug("Broadcast HUD state: {} -> {}", data.get("elementName").getAsString(), element.isActive());
        } catch (Exception e) {
            LOG.error("Failed to broadcast HUD state change: {}", e.getMessage(), e);
        }
    }

    public void broadcastHudSettingChange(HudElement element, Setting<?> setting) {
        if (!running) return;
        try {
            JsonObject data = HudMapper.createHudSettingChangeMessage(element, setting);
            WSMessage message = new WSMessage(MessageType.HUD_SETTING_VALUE_CHANGED, data);
            webSocketHandler.broadcast(GSON.toJson(message));
            LOG.debug("Broadcast HUD setting change: {}.{}", data.get("elementName").getAsString(), setting.name);
        } catch (Exception e) {
            LOG.error("Failed to broadcast HUD setting change: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast favorites state change to all connected clients.
     */
    public void broadcastFavoritesChanged() {
        if (!running) return;
        try {
            JsonObject data = ModuleMapper.createFavoritesStateMessage();
            WSMessage message = new WSMessage(MessageType.FAVORITES_STATE_CHANGED, data);
            webSocketHandler.broadcast(GSON.toJson(message));
            LOG.debug("Broadcast favorites change: {} favorites", data.getAsJsonArray("favorites").size());
        } catch (Exception e) {
            LOG.error("Failed to broadcast favorites change: {}", e.getMessage(), e);
        }
    }

    public void broadcast(String jsonPayload) {
        if (!running || webSocketHandler == null) return;
        try {
            webSocketHandler.broadcast(jsonPayload);
        } catch (Exception e) {
            LOG.error("Failed to broadcast custom payload: {}", e.getMessage(), e);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    /**
     * Returns the number of active WebSocket connections.
     */
    public int getConnectionCount() {
        return webSocketHandler != null ? webSocketHandler.getConnectionCount() : 0;
    }
}
