package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;

public class Module {
    public final String name, desc;
    public final Category cat;
    public boolean enabled, pinned;
    public String[] opts;
    public int opt;

    public Module(String name, Category cat, String desc) {
        this.name = name; this.cat = cat; this.desc = desc;
    }

    public Module on() { enabled = true; return this; }
    public Module pin() { enabled = true; pinned = true; return this; }
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
}
