package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Profiles {
    public static final String[] NAMES = {"Default", "PVP", "LowFPS", "Streaming", "Custom"};
    public static int selected = 0;

    private static Path file(String n) {
        return n.equals("Default") ? Config.main() : Config.dir().resolve("levixopclient-profile-" + n + ".properties");
    }

    public static void save(String n) { Config.write(file(n)); }

    public static void load(MinecraftClient mc, String n) {
        Path f = file(n);
        if (Files.exists(f)) { Config.read(f, mc); return; }
        Set<String> on = new HashSet<>();
        switch (n) {
            case "PVP" -> on.addAll(Arrays.asList("CPS Counter", "Combo Counter", "Reach Display", "Keystrokes",
                    "Armor Status", "Target HUD", "Hit Sounds", "FPS Display", "Ping Display", "Potion Status"));
            case "LowFPS" -> on.addAll(Arrays.asList("FPS Boost", "Entity Culling", "Fast Render", "Fast World",
                    "Particle Optimizer", "Memory Optimizer", "Smart Render", "Dynamic FPS", "FPS Display"));
            default -> { return; }
        }
        for (Module m : Modules.ALL) {
            if (m.pinned || m.noSave) continue;
            m.setEnabled(on.contains(m.name), mc);
        }
    }

    public static void disableAll(MinecraftClient mc) {
        for (Module m : Modules.ALL) m.setEnabled(false, mc);
    }

    public static void reset(MinecraftClient mc) {
        for (Module m : Modules.ALL) m.setEnabled(m.defOn, mc);
    }
}
