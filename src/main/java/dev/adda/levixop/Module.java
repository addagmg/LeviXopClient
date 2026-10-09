package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Module {
    public final String name, desc;
    public final Category cat;
    public boolean enabled, pinned, defOn, noSave, keyDown, hidden;
    public float swAnim, hoverAnim;
    public String[] opts;
    public int opt;
    public final List<Setting> settings = new ArrayList<>();
    public final Setting.Key key = new Setting.Key("Keybind");
    public Consumer<MinecraftClient> action;

    public Module(String name, Category cat, String desc) {
        this.name = name; this.cat = cat; this.desc = desc;
    }

    public <T extends Setting> T add(T s) { settings.add(s); return s; }

    public Module on() { enabled = true; defOn = true; swAnim = 1f; return this; }
    public Module pin() { enabled = true; pinned = true; return this; }
    public Module action(Consumer<MinecraftClient> a) { action = a; pinned = true; return this; }
    public Module transientState() { noSave = true; return this; }
    public Module hide() { hidden = true; return this; }
    public Module options(int def, String... o) { opts = o; opt = def; return this; }
    public String option() { return opts == null ? "" : opts[opt]; }

    public void cycleOption(MinecraftClient mc) {
        if (opts == null) return;
        opt = (opt + 1) % opts.length;
        if (enabled && !pinned) { onDisable(mc); onEnable(mc); }
    }

    public void setEnabled(boolean b, MinecraftClient mc) {
        if (pinned || b == enabled) return;
        enabled = b;
        if (b) onEnable(mc); else onDisable(mc);
    }

    public void onEnable(MinecraftClient mc) {}
    public void onDisable(MinecraftClient mc) {}
    public void onTick(MinecraftClient mc) {}
    /** called after the user changed one of this module's settings */
    public void settingChanged(MinecraftClient mc) {}
}
