package dev.adda.levixop;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.text.Text;

import java.util.List;

/** Fully custom-drawn ClickGUI: no vanilla widgets or textures. */
public class ClickGui extends Screen {
    private static final int PW = 320, HEAD = 28, TABH = 16, ROW = 26, GAP = 3, FOOT = 28, SW = 28, SH = 12;

    private int px, py, ph, listTop, listBot, profile = 1;
    private Category tab = Category.PVP;
    private double scroll, pressX, pressY, moved;
    private int pressBtn = -1;

    public ClickGui() { super(Text.literal("LeviXopclient")); }

    @Override
    protected void init() {
        ph = Math.min(height - 12, 270);
        px = (width - PW) / 2;
        py = (height - ph) / 2;
        listTop = py + HEAD + TABH + 8;
        listBot = py + ph - FOOT;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {
        ctx.fill(0, 0, width, height, 0x70000000);
    }

    private int tabW() { return (PW - 12) / Category.values().length; }
    private int rowY(int i) { return listTop + i * (ROW + GAP) - (int) scroll; }
    private int btnW() { return (PW - 12 - 4 * 4) / 5; }
    private int btnX(int i) { return px + 6 + i * (btnW() + 4); }
    private int btnY() { return py + ph - FOOT + 6; }
    private double maxScroll(int n) { return Math.max(0, n * (ROW + GAP) - (listBot - listTop)); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        int acc = Theme.accent();

        ctx.fill(px - 1, py - 1, px + PW + 1, py + ph + 1, acc);
        ctx.fill(px, py, px + PW, py + ph, 0xF0141418);
        ctx.fill(px, py, px + PW, py + HEAD, 0xFF1C1C24);
        ctx.fill(px, py + HEAD - 2, px + PW, py + HEAD, acc);
        ctx.drawTextWithShadow(textRenderer, "LeviXopclient", px + 10, py + 10, acc);
        String v = "v1.1";
        ctx.drawTextWithShadow(textRenderer, v, px + PW - 10 - textRenderer.getWidth(v), py + 10, 0xFF8888A0);

        // tabs
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            int x = px + 6 + i * tabW(), y = py + HEAD + 4;
            boolean sel = cats[i] == tab;
            ctx.fill(x, y, x + tabW() - 2, y + TABH, sel ? 0xFF2A2A38 : 0xFF1C1C24);
            if (sel) ctx.fill(x, y + TABH - 2, x + tabW() - 2, y + TABH, acc);
            String l = cats[i].label;
            ctx.drawTextWithShadow(textRenderer, l, x + (tabW() - 2 - textRenderer.getWidth(l)) / 2, y + 4, sel ? 0xFFFFFFFF : 0xFF9999AA);
        }

        // module list
        List<Module> list = Modules.in(tab);
        scroll = Math.max(0, Math.min(scroll, maxScroll(list.size())));
        ctx.enableScissor(px + 6, listTop, px + PW - 6, listBot);
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int y = rowY(i);
            if (y + ROW < listTop || y > listBot) continue;
            boolean hover = mx >= px + 6 && mx < px + PW - 6 && my >= Math.max(y, listTop) && my < Math.min(y + ROW, listBot);
            ctx.fill(px + 6, y, px + PW - 6, y + ROW, hover ? 0xFF2A2A38 : 0xFF1F1F29);
            ctx.drawTextWithShadow(textRenderer, m.name, px + 14, y + 4, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(m.desc, 170), px + 14, y + 15, 0xFF8888A0);

            int sx = px + PW - 14 - SW;
            if (!m.pinned) {
                int sy = y + (ROW - SH) / 2;
                ctx.fill(sx, sy, sx + SW, sy + SH, m.enabled ? 0xFF3DDC84 : 0xFF44444F);
                int k = m.enabled ? sx + SW - SH + 1 : sx + 1;
                ctx.fill(k, sy + 1, k + SH - 2, sy + SH - 1, 0xFFFFFFFF);
            }
            if (m.opts != null) {
                String s = "< " + m.option() + " >";
                int ox = (m.pinned ? px + PW - 14 : sx - 10) - textRenderer.getWidth(s);
                ctx.drawTextWithShadow(textRenderer, s, ox, y + 9, acc);
            }
        }
        ctx.disableScissor();

        // footer buttons
        String[] labels = {"HUD Edit", "Keys", "Prof " + profile, "Save", "Load"};
        for (int i = 0; i < 5; i++) {
            int x = btnX(i), y = btnY();
            boolean hover = mx >= x && mx < x + btnW() && my >= y && my < y + 18;
            ctx.fill(x, y, x + btnW(), y + 18, hover ? acc : 0xFF2A2A38);
            ctx.drawTextWithShadow(textRenderer, labels[i], x + (btnW() - textRenderer.getWidth(labels[i])) / 2, y + 5, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        pressX = mx; pressY = my; pressBtn = button; moved = 0;
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        moved += Math.abs(dx) + Math.abs(dy);
        if (my >= listTop && my < listBot) scroll -= dy;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        scroll -= vAmount * 20;
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (pressBtn == button && moved < 6) click(mx, my, button);
        pressBtn = -1;
        return true;
    }

    private void click(double mx, double my, int button) {
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            int x = px + 6 + i * tabW(), y = py + HEAD + 4;
            if (mx >= x && mx < x + tabW() - 2 && my >= y && my < y + TABH) { tab = cats[i]; scroll = 0; return; }
        }
        for (int i = 0; i < 5; i++) {
            int x = btnX(i), y = btnY();
            if (mx >= x && mx < x + btnW() && my >= y && my < y + 18) { button(i); return; }
        }
        if (my < listTop || my >= listBot) return;
        List<Module> list = Modules.in(tab);
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int y = rowY(i);
            if (mx >= px + 6 && mx < px + PW - 6 && my >= y && my < y + ROW) {
                int sx = px + PW - 14 - SW;
                boolean optZone = m.opts != null && (m.pinned || mx < sx - 4)
                        && mx > (m.pinned ? px + PW - 14 : sx - 10) - textRenderer.getWidth("< " + m.option() + " >") - 4;
                if (optZone || (button == 1 && m.opts != null)) m.cycleOption(client);
                else if (button == 0) { m.setEnabled(!m.enabled, client); }
                Config.save();
                return;
            }
        }
    }

    private void button(int i) {
        switch (i) {
            case 0 -> { if (client.world != null) client.setScreen(new HudEditorScreen(this)); }
            case 1 -> client.setScreen(new ControlsOptionsScreen(this, client.options));
            case 2 -> profile = profile % 3 + 1;
            case 3 -> { Config.save(); Config.write(Config.profile(profile)); }
            case 4 -> { Config.read(Config.profile(profile), client); Config.save(); }
            default -> {}
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (LeviXopClient.menuKey.matchesKey(keyCode, scanCode)) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() { Config.save(); super.close(); }
}
