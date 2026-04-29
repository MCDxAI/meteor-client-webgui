package com.cope.meteorwebgui.gui;

import com.cope.meteorwebgui.MeteorWebGUIAddon;
import com.cope.meteorwebgui.systems.WebGUIConfig;
import com.cope.meteorwebgui.util.BrowserHelper;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.client.gui.screens.Screen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WebGUITab extends Tab {
    public WebGUITab() {
        super("WebGUI");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new WebGUITabScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof WebGUITabScreen;
    }

    private static class WebGUITabScreen extends WindowTabScreen {
        public WebGUITabScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            Settings settings = WebGUIConfig.get().settings;

            // Add settings widgets
            add(theme.settings(settings)).expandX();

            // Add server control buttons
            add(theme.horizontalSeparator()).expandX();

            var controlSection = add(theme.verticalList()).expandX().widget();

            // Server status
            var statusRow = controlSection.add(theme.horizontalList()).expandX().widget();
            statusRow.add(theme.label("Server Status: ")).expandX();

            String statusText = MeteorWebGUIAddon.isServerRunning() ? "Running" : "Stopped";
            statusRow.add(theme.label(statusText)).expandX();

            // Start/Stop button
            var buttonRow = controlSection.add(theme.horizontalList()).expandX().widget();

            if (MeteorWebGUIAddon.isServerRunning()) {
                buttonRow.add(theme.button("Stop Server")).expandX().widget().action = () -> {
                    MeteorWebGUIAddon.stopServer();
                    reload();
                };

                // URL info
                String url = "http://" + WebGUIConfig.get().host.get() + ":" + WebGUIConfig.get().port.get();
                buttonRow.add(theme.button("Open in Browser")).expandX().widget().action = () -> BrowserHelper.openUrl(url);

            } else {
                buttonRow.add(theme.button("Start Server")).expandX().widget().action = () -> {
                    MeteorWebGUIAddon.startServer();
                    reload();
                };
            }

            // Info section
            add(theme.horizontalSeparator()).expandX();

            var infoSection = add(theme.verticalList()).expandX().widget();
            infoSection.add(theme.label("WebGUI allows you to control Meteor Client from a web browser."));
            infoSection.add(theme.label("Access the interface at: http://" +
                WebGUIConfig.get().host.get() + ":" + WebGUIConfig.get().port.get()));
            infoSection.add(theme.label("Enable 'Auto Start' to launch the server automatically."));
        }
    }
}
