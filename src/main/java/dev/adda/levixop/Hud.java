package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class Hud {
    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        Stats.pollMouse(mc);
        if (mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;
        if (mc.currentScreen instanceof HudEditorScreen) return;
        for (Module m : Modules.ALL) {
            if (m.enabled && m instanceof HudModule h) h.render(ctx, mc, false);
        }
    }
}
