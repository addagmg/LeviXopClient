package dev.adda.levixop;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public class HudEditorScreen extends LxScreen {
    private final Screen parent;
    private HudModule sel;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {
        ctx.fill(0, 0, width, height, 0x50000000);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        for (Module m : Modules.ALL) {
            if (m.enabled && m instanceof HudModule h) {
                h.render(ctx, client, true);
                Ui.border(ctx, h.x - 1, h.y - 1, h.w + 2, h.h + 2, h == sel ? 0xFFFFFFFF : Theme.accent());
            }
        }
        String t = "HUD Editor - drag elements, ESC to save";
        ctx.drawTextWithShadow(textRenderer, t, (width - textRenderer.getWidth(t)) / 2, 6, 0xFFFFFFFF);
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        sel = null;
        List<Module> all = Modules.ALL;
        for (int i = all.size() - 1; i >= 0; i--) {
            Module m = all.get(i);
            if (m.enabled && m instanceof HudModule h && mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h) {
                sel = h;
                break;
            }
        }
        return true;
    }

    @Override
    protected boolean onDrag(double mx, double my, int button, double dx, double dy) {
        if (sel != null) { sel.x += dx; sel.y += dy; }
        return true;
    }

    @Override
    protected boolean onRelease(double mx, double my, int button) { sel = null; return true; }

    @Override
    public void close() {
        Config.save();
        client.setScreen(parent);
    }
}
