package dev.adda.levixop;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;

/** Version specific glue (v2). */
public final class Compat {
    public static void pushScale(DrawContext ctx, float cx, float cy, float s) {
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(cx, cy, 0f);
        m.scale(s, s, 1f);
        m.translate(-cx, -cy, 0f);
    }
    public static void pop(DrawContext ctx) { ctx.getMatrices().pop(); }

    public static KeyBinding bind(String id, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(id, InputUtil.Type.KEYSYM, key, "category.levixopclient"));
    }
    public static boolean matches(KeyBinding kb, int key, int scan) { return kb.matchesKey(key, scan); }
    public static boolean keyDown(MinecraftClient mc, int code) { return InputUtil.isKeyPressed(mc.getWindow().getHandle(), code); }
    public static String keyName(int code) {
        try { return InputUtil.fromKeyCode(code, 0).getLocalizedText().getString(); }
        catch (Exception e) { return "Key " + code; }
    }

    public static boolean debugOpen(MinecraftClient mc) { return mc.getDebugHud().shouldShowDebugHud(); }

    public static boolean captureSupported() { return true; }
    public static int[] fbSize(MinecraftClient mc) {
        Framebuffer fb = mc.getFramebuffer();
        return new int[]{fb.textureWidth, fb.textureHeight};
    }
    public static byte[] capture(MinecraftClient mc, int stride, int w, int h) {
        NativeImage img = ScreenshotRecorder.takeScreenshot(mc.getFramebuffer());
        try {
            if (((img.getWidth() / stride) & ~1) != w || ((img.getHeight() / stride) & ~1) != h) return null;
            byte[] rgb = new byte[w * h * 3];
            int i = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = img.getColorArgb(x * stride, y * stride);
                    rgb[i++] = (byte) (c >> 16); rgb[i++] = (byte) (c >> 8); rgb[i++] = (byte) c;
                }
            }
            return rgb;
        } finally {
            img.close();
        }
    }

    public static void setTimeOfDay(MinecraftClient mc, long t) {
        if (mc.world != null) ((ClientWorld.Properties) mc.world.getLevelProperties()).setTimeOfDay(t);
    }
    public static void hitboxes(MinecraftClient mc, boolean on) { mc.getEntityRenderDispatcher().setRenderHitboxes(on); }
}
