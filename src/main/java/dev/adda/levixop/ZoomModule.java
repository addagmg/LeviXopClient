package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

public class ZoomModule extends Module {
    private final Setting.Slider zoomFov = add(new Setting.Slider("Zoom FOV", 30, 70, 1, 30, ""));
    private boolean zooming;
    private int orig;

    public ZoomModule() { super("Zoom", Category.RENDER, "Hold the Zoom key (C by default, rebind in Controls)"); }

    @Override
    public void onTick(MinecraftClient mc) {
        boolean down = LeviXopClient.zoomKey.isPressed() && mc.currentScreen == null;
        SimpleOption<Integer> fov = mc.options.getFov();
        if (down && !zooming) { orig = fov.getValue(); zooming = true; fov.setValue((int) zoomFov.get()); }
        else if (!down && zooming) { fov.setValue(orig); zooming = false; }
    }

    @Override
    public void onDisable(MinecraftClient mc) {
        if (zooming) { mc.options.getFov().setValue(orig); zooming = false; }
    }
}
