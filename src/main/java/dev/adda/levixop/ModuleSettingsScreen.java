package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Big separate window with all settings of one module (like the reference "Module Settings"). */
public class ModuleSettingsScreen extends LxScreen {
    private static final int HEAD = 38, FOOT = 34;

    private final Screen parent;
    private final Module m;
    private final List<Setting> rows = new ArrayList<>();
    private final Map<Setting, String> snap = new IdentityHashMap<>();
    private int px, py, pw, ph;
    private double scroll, moved;
    private boolean potential, closing;
    private long openAt = System.currentTimeMillis(), closeAt, last = openAt;
    private float dt, animT;
    private Setting listening;
    private Setting.Slider dragging;
    private int dragX, dragW;

    public ModuleSettingsScreen(Screen parent, Module m) {
        super(Text.literal(m.name));
        this.parent = parent;
        this.m = m;
        boolean normal = !m.pinned && !m.hidden;
        if (normal) {
            rows.add(new Setting.Bool("Enabled", m.enabled) {
                @Override public boolean get() { return m.enabled; }
                @Override public void set(boolean b) { m.setEnabled(b, MinecraftClient.getInstance()); }
                @Override public boolean persist() { return false; }
            });
        }
        if (m.opts != null) {
            rows.add(new Setting.Mode("Mode", m.opt, m.opts) {
                @Override public int get() { return m.opt; }
                @Override public void set(int i) { m.cycleOption(MinecraftClient.getInstance()); }
                @Override public boolean persist() { return false; }
            });
        }
        rows.addAll(m.settings);
        if (normal) rows.add(m.key);
        for (Setting s : m.settings) snap.put(s, s.save());
        Sfx.play(SoundEvents.UI_BUTTON_CLICK, 1.1f);
    }

    @Override
    protected void init() {
        pw = Math.min(width - 10, 400);
        ph = Math.min(height - 10, 300);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {
        ctx.fill(0, 0, width, height, ((int) (0x80 * animT)) << 24);
    }

    private int bodyTop() { return py + HEAD + 4; }
    private int bodyBot() { return py + ph - FOOT; }
    private int rx() { return px + 8; }
    private int rw() { return pw - 16; }
    private int rowsY() { return bodyTop() - (int) scroll; }
    private boolean busy() { return closing || System.currentTimeMillis() - openAt < 250; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        dt = Math.min(0.05f, (now - last) / 1000f);
        last = now;
        animT = closing ? 1f - Anim.ease((now - closeAt) / 150f) : Anim.ease((now - openAt) / 250f);
        super.render(ctx, mx, my, delta);
        if (closing && now - closeAt >= 150) { Config.save(); client.setScreen(parent); return; }

        int acc = Theme.accent();
        float s = 0.9f + 0.1f * animT;
        int cxp = px + pw / 2, cyp = py + ph / 2;
        Compat.pushScale(ctx, cxp, cyp, s);

        ctx.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, acc);
        ctx.fill(px, py, px + pw, py + ph, Ui.BG);
        ctx.fill(px, py, px + pw, py + HEAD, Ui.SIDE);
        ctx.fill(px, py + HEAD - 1, px + pw, py + HEAD, acc);
        ctx.fill(px + 8, py + 7, px + 30, py + 29, Ui.ICON);
        Ui.tc(ctx, textRenderer, m.name.substring(0, 1), px + 19, py + 14, acc);
        Ui.t(ctx, textRenderer, m.name, px + 38, py + 9, Ui.TXT);
        Ui.t(ctx, textRenderer, textRenderer.trimToWidth(m.desc, pw - 90), px + 38, py + 21, Ui.DIM);
        boolean xh = Ui.in(mx, my, px + pw - 24, py + 8, 16, 16);
        ctx.fill(px + pw - 24, py + 8, px + pw - 8, py + 24, xh ? acc : Ui.CARD_H);
        Ui.tc(ctx, textRenderer, "X", px + pw - 16, py + 12, xh ? 0xFFFFFFFF : Ui.TXT);

        double max = Math.max(0, SettingsUi.totalH(rows) - (bodyBot() - bodyTop()));
        scroll = Math.max(0, Math.min(scroll, max));
        ctx.enableScissor(px, bodyTop(), px + pw, bodyBot());
        SettingsUi.draw(ctx, textRenderer, rows, rx(), rowsY(), rw(), acc, listening, dt);
        ctx.disableScissor();

        // footer buttons
        boolean canDisable = !m.pinned && !m.hidden;
        String[] labels = canDisable ? new String[]{"Apply", "Reset", "Default", "Disable"} : new String[]{"Apply", "Reset", "Default"};
        int n = labels.length, bw = (pw - 16 - (n - 1) * 6) / n, by = py + ph - FOOT + 6;
        for (int i = 0; i < n; i++) {
            int bx = px + 8 + i * (bw + 6);
            boolean hov = Ui.in(mx, my, bx, by, bw, 22);
            boolean red = canDisable && i == 3;
            int bg = i == 0 ? (hov ? Anim.lerpColor(acc, 0xFF000000, 0.12f) : acc)
                    : red ? (hov ? 0xFFDC2626 : 0xFFEF4444) : (hov ? Ui.CARD_H : Ui.FIELD);
            ctx.fill(bx, by, bx + bw, by + 22, bg);
            if (i != 0 && !red) Ui.border(ctx, bx, by, bw, 22, Ui.BORDER);
            Ui.tc(ctx, textRenderer, labels[i], bx + bw / 2, by + 7, (i == 0 || red) ? 0xFFFFFFFF : Ui.TXT);
        }
        Compat.pop(ctx);
    }

    private void startClose() {
        if (closing) return;
        closing = true;
        closeAt = System.currentTimeMillis();
        Sfx.play(SoundEvents.UI_BUTTON_CLICK, 0.8f);
    }

    @Override public void close() { startClose(); }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        if (busy()) return true;
        moved = 0;
        if (Ui.in(mx, my, px + pw - 24, py + 8, 16, 16) || !Ui.in(mx, my, px, py, pw, ph)) { startClose(); return true; }
        if (my >= bodyBot()) { footerClick(mx, my); return true; }
        if (my >= bodyTop()) {
            int i = SettingsUi.hit(rows, rx(), rowsY(), rw(), mx, my);
            if (i >= 0 && rows.get(i) instanceof Setting.Slider sl) {
                dragging = sl; dragX = rx() + 8; dragW = rw() - 16;
                SettingsUi.sliderTo(sl, dragX, dragW, mx);
                return true;
            }
            potential = true;
        }
        return true;
    }

    @Override
    protected boolean onDrag(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { SettingsUi.sliderTo(dragging, dragX, dragW, mx); return true; }
        moved += Math.abs(dx) + Math.abs(dy);
        if (potential) scroll -= dy;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) { scroll -= v * 20; return true; }

    @Override
    protected boolean onRelease(double mx, double my, int button) {
        if (dragging != null) { dragging = null; m.settingChanged(client); Config.save(); }
        else if (potential && moved < 6) {
            int i = SettingsUi.hit(rows, rx(), rowsY(), rw(), mx, my);
            if (i >= 0) {
                Setting s = rows.get(i);
                if (s instanceof Setting.Key) listening = s;
                else {
                    SettingsUi.click(s, rx(), rw(), SettingsUi.hitTop, mx, my);
                    Sfx.play(SoundEvents.UI_BUTTON_CLICK, 1.2f);
                    if (m.settings.contains(s)) m.settingChanged(client);
                    Config.save();
                }
            }
        }
        potential = false;
        return true;
    }

    private void footerClick(double mx, double my) {
        boolean canDisable = !m.pinned && !m.hidden;
        int n = canDisable ? 4 : 3, bw = (pw - 16 - (n - 1) * 6) / n, by = py + ph - FOOT + 6;
        for (int i = 0; i < n; i++) {
            if (!Ui.in(mx, my, px + 8 + i * (bw + 6), by, bw, 22)) continue;
            Sfx.play(SoundEvents.UI_BUTTON_CLICK, 1.0f);
            switch (i) {
                case 0 -> { Config.save(); startClose(); }
                case 1 -> { for (Setting s : m.settings) if (snap.containsKey(s)) s.load(snap.get(s)); m.settingChanged(client); }
                case 2 -> { for (Setting s : m.settings) s.reset(); m.settingChanged(client); }
                default -> { m.setEnabled(false, client); }
            }
            return;
        }
    }

    @Override
    protected boolean onKey(int keyCode, int scanCode, int modifiers) {
        if (listening instanceof Setting.Key k) {
            k.code = keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode;
            listening = null;
            Config.save();
            return true;
        }
        return false;
    }
}
