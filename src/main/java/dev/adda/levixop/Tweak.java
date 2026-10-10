package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

import java.util.LinkedHashMap;
import java.util.Map;

/** Performance module that changes video options and restores them on disable. */
public class Tweak extends Module {
    private final Map<SimpleOption<?>, Object> saved = new LinkedHashMap<>();

    public Tweak(String name, String desc) { super(name, Category.PERFORMANCE, desc); }

    protected <T> void set(SimpleOption<T> o, T v) {
        saved.putIfAbsent(o, o.getValue());
        o.setValue(v);
    }

    @SuppressWarnings("unchecked")
    protected <T> T original(SimpleOption<T> o) { return (T) saved.get(o); }

    @SuppressWarnings("unchecked")
    @Override
    public void onDisable(MinecraftClient mc) {
        for (Map.Entry<SimpleOption<?>, Object> e : saved.entrySet()) {
            ((SimpleOption<Object>) e.getKey()).setValue(e.getValue());
        }
        saved.clear();
    }

    @Override
    public void settingChanged(MinecraftClient mc) {
        if (enabled) { onDisable(mc); onEnable(mc); }
    }

    protected <T> void setNamed(SimpleOption<T> o, String name) { set(o, Lx.named(o, name)); }
}
