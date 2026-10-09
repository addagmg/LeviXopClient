package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.function.Function;

public class HudModule extends Module {
    private static int counter = 0;
    public int x, y, w = 60, h = 12;
    private final Function<MinecraftClient, List<String>> src;

    public HudModule(String name, Category cat, String desc, Function<MinecraftClient, List<String>> src) {
        super(name, cat, desc);
        this.src = src;
        x = 4 + (counter / 12) * 130;
        y = 4 + (counter % 12) * 16;
        counter++;
    }

    public HudModule(String name, Category cat, String desc) { this(name, cat, desc, null); }

    protected List<String> lines(MinecraftClient mc) {
        return src == null ? List.of() : src.apply(mc);
    }

    protected void clamp(DrawContext ctx) {
        x = MathHelper.clamp(x, 0, Math.max(0, ctx.getScaledWindowWidth() - w));
        y = MathHelper.clamp(y, 0, Math.max(0, ctx.getScaledWindowHeight() - h));
    }

    public void render(DrawContext ctx, MinecraftClient mc, boolean editor) {
        List<String> ls = lines(mc);
        if (ls.isEmpty()) {
            if (!editor) { w = 0; h = 0; return; }
            ls = List.of(name);
        }
        int tw = 0;
        for (String s : ls) tw = Math.max(tw, mc.textRenderer.getWidth(s));
        w = tw + 9;
        h = ls.size() * 11 + 3;
        clamp(ctx);
        ctx.fill(x, y, x + w, y + h, 0x90000000);
        ctx.fill(x, y, x + 2, y + h, Theme.accent());
        int yy = y + 3;
        for (String s : ls) {
            ctx.drawTextWithShadow(mc.textRenderer, s, x + 5, yy, 0xFFFFFFFF);
            yy += 11;
        }
    }
}
