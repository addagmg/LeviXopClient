package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;

public class RecorderModule extends Module {
    private final Setting.Mode quality = add(new Setting.Mode("Quality", 1, "Low", "Medium", "High"));
    private final Setting.Slider fps = add(new Setting.Slider("FPS", 10, 30, 1, 15, ""));

    public RecorderModule() {
        super("Screen Recorder", Category.MISC, "On = start recording. F7 pause, F8 stop (rebind in Controls)");
        transientState();
    }

    @Override
    public void onEnable(MinecraftClient mc) {
        if (mc.world == null) { enabled = false; return; }
        int stride = quality.get() == 0 ? 3 : quality.get() == 1 ? 2 : 1;
        Recorder.start(mc, stride, (int) fps.get());
        if (!Recorder.active()) enabled = false;
    }

    @Override
    public void onDisable(MinecraftClient mc) { Recorder.stop(mc); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (enabled && !Recorder.active()) enabled = false;
    }
}
