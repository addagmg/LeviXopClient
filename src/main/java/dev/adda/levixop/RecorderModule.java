package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Hidden from the Modules list: has its own Recorder page. */
public class RecorderModule extends Module {
    public final Setting.Mode quality = add(new Setting.Mode("Quality", 1, "Low", "Medium", "High"));
    public final Setting.Number fps = add(new Setting.Number("FPS", 10, 30, 1, 15));
    public final Setting.Key startKey = add(new Setting.Key("Start Key"));
    public final Setting.Key pauseKey = add(new Setting.Key("Pause / Resume Key"));
    public final Setting.Key stopKey = add(new Setting.Key("Stop Key"));
    private boolean ks, kp, kt;

    public RecorderModule() {
        super("Screen Recorder", Category.MISC, "Records your gameplay to .avi");
        transientState();
        hide();
        startKey.code = GLFW.GLFW_KEY_F9;
        pauseKey.code = GLFW.GLFW_KEY_F7;
        stopKey.code = GLFW.GLFW_KEY_F8;
    }

    @Override
    public void onEnable(MinecraftClient mc) {
        if (mc.world == null) { enabled = false; return; }
        int stride = quality.get() == 0 ? 3 : quality.get() == 1 ? 2 : 1;
        Recorder.start(mc, stride, fps.get());
        if (!Recorder.active()) enabled = false;
    }

    @Override
    public void onDisable(MinecraftClient mc) { Recorder.stop(mc); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (enabled && !Recorder.active()) enabled = false;
    }

    public void stopAll(MinecraftClient mc) {
        Recorder.stop(mc);
        enabled = false;
    }

    private static boolean down(MinecraftClient mc, Setting.Key k) { return k.code > 0 && Compat.keyDown(mc, k.code); }

    /** runs every tick, even while the module is off (so the Start key works) */
    public void pollKeys(MinecraftClient mc) {
        boolean s = down(mc, startKey), p = down(mc, pauseKey), t = down(mc, stopKey);
        if (mc.currentScreen == null && mc.world != null) {
            if (s && !ks && !enabled) setEnabled(true, mc);
            if (p && !kp) Recorder.togglePause(mc);
            if (t && !kt) stopAll(mc);
        }
        ks = s; kp = p; kt = t;
    }
}
