package com.cope.meteorwebgui.events;

import com.cope.meteorwebgui.mapping.HudMapper;
import com.cope.meteorwebgui.server.MeteorWebServer;
import meteordevelopment.meteorclient.events.meteor.ActiveModulesChangedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Monitors module and setting changes and broadcasts them to WebSocket clients
 */
public class EventMonitor {
    private static final Logger LOG = LoggerFactory.getLogger("WebGUI Event Monitor");

    private final MeteorWebServer server;
    private final Map<Setting<?>, Consumer<?>> originalCallbacks = new HashMap<>();
    private final Map<String, Boolean> moduleStates = new HashMap<>();
    private final Map<String, Boolean> favoriteStates = new HashMap<>();
    private final Map<String, Boolean> hudStates = new HashMap<>();

    public EventMonitor(MeteorWebServer server) {
        this.server = server;
    }

    /**
     * Start monitoring all modules and settings
     */
    public void startMonitoring() {
        LOG.info("Starting event monitoring");

        // Monitor all existing modules and initialize state tracking
        for (Module module : Modules.get().getAll()) {
            moduleStates.put(module.name, module.isActive());
            favoriteStates.put(module.name, module.favorite);
            monitorModuleSettings(module);
        }

        monitorHudElements();

        LOG.info("Event monitoring started for {} modules", Modules.get().getCount());
    }

    /**
     * Handle module toggle events
     * ActiveModulesChangedEvent is a singleton event that fires whenever any module toggles,
     * but doesn't tell us which one. So we compare current state to our tracked state.
     * We also check for favorite changes here since there's no dedicated event for them.
     */
    @EventHandler
    private void onModuleToggle(ActiveModulesChangedEvent event) {
        boolean favoritesChanged = false;

        // Find which module(s) changed state
        for (Module module : Modules.get().getAll()) {
            // Check active state
            Boolean previousState = moduleStates.get(module.name);
            boolean currentState = module.isActive();

            if (previousState == null || previousState != currentState) {
                // State changed!
                moduleStates.put(module.name, currentState);

                if (server.isRunning()) {
                    server.broadcastModuleStateChange(module);
                    LOG.debug("Module state changed: {} -> {}", module.name, currentState);
                }
            }

            // Check favorite state
            Boolean previousFavorite = favoriteStates.get(module.name);
            boolean currentFavorite = module.favorite;

            if (previousFavorite == null || previousFavorite != currentFavorite) {
                favoriteStates.put(module.name, currentFavorite);
                favoritesChanged = true;
                LOG.debug("Module favorite changed: {} -> {}", module.name, currentFavorite);
            }
        }

        // Broadcast favorites change if any module's favorite status changed
        if (favoritesChanged && server.isRunning()) {
            server.broadcastFavoritesChanged();
        }
    }

    @EventHandler
    private void onHudRender(Render2DEvent event) {
        for (HudElement element : Hud.get()) {
            String id = HudMapper.getElementIdentifier(element);
            boolean currentState = element.isActive();
            Boolean previousState = hudStates.get(id);

            if (previousState == null || previousState != currentState) {
                hudStates.put(id, currentState);
                if (server.isRunning()) {
                    server.broadcastHudStateChange(element);
                    LOG.debug("HUD state changed: {} -> {}", id, currentState);
                }
            }
        }
    }

    /**
     * Wrap setting callbacks
     */
    private void monitorModuleSettings(Module module) {
        for (SettingGroup group : module.settings) {
            for (Setting<?> setting : group) {
                wrapSettingCallback(module, setting);
            }
        }
    }

    private void monitorHudElements() {
        for (HudElement element : Hud.get()) {
            String id = HudMapper.getElementIdentifier(element);
            hudStates.put(id, element.isActive());
            monitorHudSettings(element);
        }
    }

    private void monitorHudSettings(HudElement element) {
        for (SettingGroup group : element.settings) {
            for (Setting<?> setting : group) {
                wrapHudSettingCallback(element, setting);
            }
        }
    }

    /**
     * Wrap a setting's onChanged callback to broadcast value changes
     */
    @SuppressWarnings("unchecked")
    private <T> void wrapSettingCallback(Module module, Setting<T> setting) {
        try {
            // Get the existing onChanged callback via reflection
            var onChangedField = Setting.class.getDeclaredField("onChanged");
            onChangedField.setAccessible(true);
            Consumer<T> originalCallback = (Consumer<T>) onChangedField.get(setting);

            // Store original for potential unwrapping later
            if (originalCallback != null) {
                originalCallbacks.put(setting, originalCallback);
            }

            // Create wrapped callback
            Consumer<T> wrappedCallback = value -> {
                // Call original callback if it exists
                if (originalCallback != null) {
                    try {
                        originalCallback.accept(value);
                    } catch (Exception e) {
                        LOG.error("Error in original callback for {}.{}: {}",
                            module.name, setting.name, e.getMessage());
                    }
                }

                // Broadcast to WebSocket clients
                if (server.isRunning()) {
                    server.broadcastSettingChange(module, setting);
                }
            };

            // Set the wrapped callback
            onChangedField.set(setting, wrappedCallback);

        } catch (Exception e) {
            LOG.error("Failed to wrap callback for setting {}.{}: {}",
                module.name, setting.name, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void wrapHudSettingCallback(HudElement element, Setting<T> setting) {
        try {
            var onChangedField = Setting.class.getDeclaredField("onChanged");
            onChangedField.setAccessible(true);
            Consumer<T> originalCallback = (Consumer<T>) onChangedField.get(setting);

            if (originalCallback != null) {
                originalCallbacks.put(setting, originalCallback);
            }

            Consumer<T> wrappedCallback = value -> {
                if (originalCallback != null) {
                    try {
                        originalCallback.accept(value);
                    } catch (Exception e) {
                        LOG.error("Error in original HUD callback for {}.{}: {}",
                            element.info != null ? element.info.name : element.getClass().getSimpleName(), setting.name, e.getMessage());
                    }
                }

                if (server.isRunning()) {
                    server.broadcastHudSettingChange(element, setting);
                }
            };

            onChangedField.set(setting, wrappedCallback);

        } catch (Exception e) {
            LOG.error("Failed to wrap callback for HUD setting {}.{}: {}",
                element.info != null ? element.info.name : element.getClass().getSimpleName(), setting.name, e.getMessage());
        }
    }

    /**
     * Stop monitoring (cleanup)
     */
    public void stopMonitoring() {
        LOG.info("Stopping event monitoring");
        // Could restore original callbacks here if needed
        originalCallbacks.clear();
        moduleStates.clear();
        favoriteStates.clear();
        hudStates.clear();
    }
}
