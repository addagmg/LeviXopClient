package dev.adda.levixop;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/** Version specific glue (v5). */
public final class Compat {
    public static void pushScale(DrawContext ctx, float cx, float cy, float s) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx, cy);
        ctx.getMatrices().scale(s, s);
        ctx.getMatrices().translate(-cx, -cy);
    }
    public static void pop(DrawContext ctx) { ctx.getMatrices().popMatrix(); }

    public static KeyBinding bind(String id, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(id, InputUtil.Type.KEYSYM, key, KeyBinding.Category.MISC));
    }
    public static boolean matches(KeyBinding kb, int key, int scan) { return kb.matchesKey(new KeyInput(key, scan, 0)); }
    public static boolean keyDown(MinecraftClient mc, int code) { return InputUtil.isKeyPressed(mc.getWindow(), code); }
    public static String keyName(int code) {
        try { return InputUtil.fromKeyCode(new KeyInput(code, 0, 0)).getLocalizedText().getString(); }
        catch (Exception e) { return "Key " + code; }
    }

    public static boolean debugOpen(MinecraftClient mc) { return mc.getDebugHud().shouldShowDebugHud(); }

    // TODO: screen capture is not ported to this Minecraft version yet
    public static boolean captureSupported() { return false; }
    public static int[] fbSize(MinecraftClient mc) { return new int[]{0, 0}; }
    public static byte[] capture(MinecraftClient mc, int stride, int w, int h) { return null; }

    // TODO: not ported to this Minecraft version yet
    public static void setTimeOfDay(MinecraftClient mc, long t) {}
    // TODO: not ported to this Minecraft version yet
    public static void hitboxes(MinecraftClient mc, boolean on) {}

    public static void slotOverlay(DrawContext ctx, net.minecraft.client.font.TextRenderer tr, net.minecraft.item.ItemStack stack, int x, int y) {
        ctx.drawStackOverlay(tr, stack, x, y);
    }

    public static net.minecraft.client.option.SimpleOption<?> graphicsOpt(MinecraftClient mc) {
        return null;
    }
}
