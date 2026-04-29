package com.cope.meteorwebgui.systems;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.CompoundTag;

public class WebGUIConfig extends System<WebGUIConfig> {
    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // Server Settings
    public final Setting<Boolean> autoStart = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-start")
        .description("Automatically start the WebGUI server when Minecraft loads.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> port = sgGeneral.add(new IntSetting.Builder()
        .name("port")
        .description("Port for the WebGUI server.")
        .defaultValue(8080)
        .min(1024)
        .max(65535)
        .sliderRange(1024, 65535)
        .build()
    );

    public final Setting<String> host = sgGeneral.add(new StringSetting.Builder()
        .name("host")
        .description("Host address to bind the server to. Use 127.0.0.1 for localhost only.")
        .defaultValue("127.0.0.1")
        .build()
    );

    public WebGUIConfig() {
        super("webgui-config");
    }

    public static WebGUIConfig get() {
        return Systems.get(WebGUIConfig.class);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("settings", settings.toTag());
        return tag;
    }

    @Override
    public WebGUIConfig fromTag(CompoundTag tag) {
        if (tag.contains("settings")) {
            settings.fromTag(tag.getCompoundOrEmpty("settings"));
        }
        return this;
    }
}
