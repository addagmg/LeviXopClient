package dev.adda.levixop;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Official LeviXopclient menu: white + orange, animated. */
public class ClickGui extends LxScreen {
    enum Page {
        MODULES("Modules", null), PERFORMANCE("Performance", Category.PERFORMANCE), COMBAT("Combat", Category.COMBAT),
        MOVEMENT("Movement", Category.MOVEMENT), RENDER("Render", Category.RENDER), HUD("HUD", Category.HUD),
        MISC("Misc", Category.MISC), RECORDER("Recorder", null), PROFILES("Profiles", null), SETTINGS("Settings", null);
        final String label; final Category cat;
        Page(String l, Category c) { label = l; cat = c; }
        boolean grid() { return this != PROFILES && this != SETTINGS && this != RECORDER; }
    }

    private static final int SIDE = 92, HEAD = 30, SB_H = 19, GAP = 6, CARD_H = 64;
    private static final String FABRIC = FabricLoader.getInstance().getModContainer("fabricloader")
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");

    private int px, py, pw, ph;
    private Page page = Page.MODULES;
    private String query = "";
    private boolean searchFocus, sortEnabled, potential, closing;
    private int catFilter = -1;
    private double scroll, moved;
    private int pressBtn;
    private long openAt = System.currentTimeMillis(), closeAt, pageAt = openAt, last = openAt;
    private float dt, animT, selY = -1f;

    private List<Setting> settingsRows = new ArrayList<>();
    private Setting listening;
    private Setting.Slider dragging;
    private int dragX, dragW;

    public ClickGui() {
        super(Text.literal("LeviXopclient"));
        Sfx.play(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f);
    }

    @Override
    protected void init() {
        pw = Math.min(width - 10, 480);
        ph = Math.min(height - 10, 300);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        settingsRows = buildSettingsRows();
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {
        ctx.fill(0, 0, width, height, ((int) (0x80 * animT)) << 24);
    }

    // ---------- layout
    private int cx() { return px + SIDE + 8; }
    private int cw() { return pw - SIDE - 16; }
    private int topY() { return py + HEAD + 6; }
    private int gridTop() { return topY() + 24; }
    private int gridBot() { return py + ph - 6; }
    private int cols() { return Math.max(1, Math.min(3, cw() / 150)); }
    private int cardW() { return (cw() - (cols() - 1) * GAP) / cols(); }
    private int searchW() { return cw() - 18 - 62 - 78 - 12; }
    private int catX() { return cx() + searchW() + 4; }
    private int sortX() { return catX() + 78 + 4; }
    private int gearX() { return sortX() + 62 + 4; }
    private boolean busy() { return closing || System.currentTimeMillis() - openAt < 250; }

    // rows shown on SETTINGS / RECORDER pages
    private List<Setting> pageRows() { return page == Page.RECORDER ? Modules.RECORDER.settings : settingsRows; }
    private int rowsTop() { return page == Page.RECORDER ? topY() + 66 : topY() + 14; }
    private int rowsY() { return rowsTop() - (int) scroll; }

    private List<Module> visible() {
        List<Module> l = new ArrayList<>();
        for (Module m : Modules.ALL) {
            if (m.hidden) continue;
            if (page.cat != null && m.cat != page.cat) continue;
            if (page == Page.MODULES && catFilter >= 0 && m.cat != Category.values()[catFilter]) continue;
            if (!query.isEmpty() && !m.name.toLowerCase().contains(query.toLowerCase())) continue;
            l.add(m);
        }
        if (sortEnabled) l.sort((a, b) -> {
            int c = Boolean.compare(b.enabled, a.enabled);
            return c != 0 ? c : a.name.compareTo(b.name);
        });
        else l.sort((a, b) -> a.name.compareTo(b.name));
        return l;
    }

    private void toast(String s) {
        if (client != null && client.player != null) client.player.sendMessage(Text.literal("[LeviXop] " + s), true);
    }

    private void setPage(Page p) {
        if (p == page) return;
        page = p; scroll = 0; searchFocus = false; pageAt = System.currentTimeMillis();
        Sfx.play(SoundEvents.UI_BUTTON_CLICK, 1.0f);
    }

    // ---------- render
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        dt = Math.min(0.05f, (now - last) / 1000f);
        last = now;
        animT = closing ? 1f - Anim.ease((now - closeAt) / 150f) : Anim.ease((now - openAt) / 250f);
        super.render(ctx, mx, my, delta);
        if (closing && now - closeAt >= 150) { Config.save(); super.close(); return; }

        int acc = Theme.accent();
        float s = 0.9f + 0.1f * animT;
        int cxp = px + pw / 2, cyp = py + ph / 2;
        Compat.pushScale(ctx, cxp, cyp, s);

        ctx.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, acc);
        ctx.fill(px, py, px + pw, py + ph, Ui.BG);
        renderHeader(ctx, acc);
        renderSidebar(ctx, mx, my, acc);
        switch (page) {
            case PROFILES -> renderProfiles(ctx, mx, my, acc);
            case SETTINGS -> renderRows(ctx, "Graphics Settings", acc, topY() + 12);
            case RECORDER -> renderRecorder(ctx, mx, my, acc, now);
            default -> renderGrid(ctx, mx, my, acc, now);
        }
        Compat.pop(ctx);
    }

    private void renderHeader(DrawContext ctx, int acc) {
        ctx.fill(px, py, px + pw, py + HEAD, Ui.SIDE);
        ctx.fill(px, py + HEAD - 1, px + pw, py + HEAD, acc);
        ctx.fill(px + 8, py + 5, px + 28, py + 25, acc);
        Ui.tc(ctx, textRenderer, "LX", px + 18, py + 11, 0xFFFFFFFF);
        int tx = px + 34;
        Ui.t(ctx, textRenderer, "LeviXop", tx, py + 6, Ui.TXT);
        Ui.t(ctx, textRenderer, "Client", tx + textRenderer.getWidth("LeviXop"), py + 6, acc);
        Ui.t(ctx, textRenderer, "Performance | Smooth | Better FPS", tx, py + 17, Ui.DIM);
        if (pw >= 420) {
            String info = "MC 1.21.4 | Fabric " + FABRIC + "  FPS: " + client.getCurrentFps();
            int iw = textRenderer.getWidth(info) + 10, ix = px + pw - 28 - iw;
            ctx.fill(ix, py + 6, ix + iw, py + 24, Ui.BG);
            Ui.border(ctx, ix, py + 6, iw, 18, Ui.BORDER);
            Ui.t(ctx, textRenderer, info, ix + 5, py + 11, Ui.TXT);
        }
        Ui.t(ctx, textRenderer, "X", px + pw - 18, py + 11, Ui.TXT);
    }

    private void renderSidebar(DrawContext ctx, int mx, int my, int acc) {
        ctx.fill(px, py + HEAD, px + SIDE, py + ph, Ui.SIDE);
        Page[] ps = Page.values();
        float target = py + HEAD + 8 + page.ordinal() * SB_H;
        if (selY < 0) selY = target;
        selY = Anim.approach(selY, target, dt);
        ctx.fill(px + 6, (int) selY, px + SIDE - 6, (int) selY + SB_H - 2, (acc & 0x00FFFFFF) | 0x33000000);
        ctx.fill(px + 6, (int) selY, px + 8, (int) selY + SB_H - 2, acc);
        for (int i = 0; i < ps.length; i++) {
            int y = py + HEAD + 8 + i * SB_H;
            boolean sel = ps[i] == page, hov = Ui.in(mx, my, px + 6, y, SIDE - 12, SB_H - 2);
            if (hov && !sel) ctx.fill(px + 6, y, px + SIDE - 6, y + SB_H - 2, 0x14FF8A00);
            ctx.fill(px + 12, y + 4, px + 20, y + 12, sel ? acc : Ui.SOFT);
            Ui.t(ctx, textRenderer, ps[i].label, px + 26, y + 5, sel ? Ui.TXT : Ui.DIM);
        }
        int fy = py + ph - 24;
        if (fy > py + HEAD + 8 + ps.length * SB_H) {
            Ui.t(ctx, textRenderer, "Made by", px + 10, fy, Ui.DIM);
            Ui.t(ctx, textRenderer, "LeviXopClient", px + 10, fy + 10, Ui.TXT);
        }
    }

    private void smallBtn(DrawContext ctx, int x, int y, int w, String label, int col) {
        ctx.fill(x, y, x + w, y + 18, Ui.BG);
        Ui.border(ctx, x, y, w, 18, Ui.BORDER);
        Ui.tc(ctx, textRenderer, textRenderer.trimToWidth(label, w - 6), x + w / 2, y + 5, col);
    }

    private void renderGrid(DrawContext ctx, int mx, int my, int acc, long now) {
        int tx = cx(), ty = topY();
        ctx.fill(tx, ty, tx + searchW(), ty + 18, Ui.BG);
        Ui.border(ctx, tx, ty, searchW(), 18, searchFocus ? acc : Ui.BORDER);
        boolean empty = query.isEmpty() && !searchFocus;
        String s = empty ? "Search modules..." : query + ((searchFocus && now / 500 % 2 == 0) ? "_" : "");
        Ui.t(ctx, textRenderer, textRenderer.trimToWidth(s, searchW() - 10), tx + 5, ty + 5, empty ? Ui.DIM : Ui.TXT);
        String cl = page != Page.MODULES ? page.label : (catFilter < 0 ? "All Categories" : Category.values()[catFilter].label);
        smallBtn(ctx, catX(), ty, 78, cl, page == Page.MODULES ? Ui.TXT : Ui.DIM);
        smallBtn(ctx, sortX(), ty, 62, "Sort: " + (sortEnabled ? "On" : "Name"), Ui.TXT);
        smallBtn(ctx, gearX(), ty, 18, "*", acc);

        List<Module> l = visible();
        int rows = (l.size() + cols() - 1) / cols();
        double max = Math.max(0, rows * (CARD_H + GAP) - (gridBot() - gridTop()));
        scroll = Math.max(0, Math.min(scroll, max));
        ctx.enableScissor(cx(), gridTop(), cx() + cw(), gridBot());
        for (int i = 0; i < l.size(); i++) {
            float t = Anim.ease((now - pageAt - i * 25L) / 220f);
            if (t <= 0f) continue;
            int x = cx() + (i % cols()) * (cardW() + GAP);
            int y = gridTop() + (i / cols()) * (CARD_H + GAP) - (int) scroll + Math.round((1f - t) * 12f);
            if (y + CARD_H < gridTop() || y > gridBot()) continue;
            drawCard(ctx, l.get(i), x, y, cardW(), mx, my, acc);
        }
        if (l.isEmpty()) Ui.t(ctx, textRenderer, "No modules found", cx() + 8, gridTop() + 8, Ui.DIM);
        ctx.disableScissor();
    }

    private void drawCard(DrawContext ctx, Module m, int x, int y, int w, int mx, int my, int acc) {
        boolean hov = Ui.in(mx, my, x, y, w, CARD_H) && my >= gridTop() && my < gridBot();
        m.hoverAnim = Anim.approach(m.hoverAnim, hov ? 1f : 0f, dt);
        m.swAnim = Anim.approach(m.swAnim, m.enabled ? 1f : 0f, dt);
        ctx.fill(x, y, x + w, y + CARD_H, Anim.lerpColor(Ui.CARD, Ui.CARD_H, m.hoverAnim));
        boolean on = m.enabled && !m.pinned;
        Ui.border(ctx, x, y, w, CARD_H, Anim.lerpColor(Ui.SOFT, acc, m.swAnim * (on ? 1f : 0f) + m.hoverAnim * 0.4f));
        ctx.fill(x + 6, y + 6, x + 28, y + 28, Ui.ICON);
        Ui.tc(ctx, textRenderer, m.name.substring(0, 1), x + 17, y + 13, acc);
        Ui.t(ctx, textRenderer, textRenderer.trimToWidth(m.name, w - 42), x + 34, y + 7, Ui.TXT);
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(m.desc), w - 40);
        for (int i = 0; i < Math.min(2, lines.size()); i++) Ui.t(ctx, textRenderer, lines.get(i), x + 34, y + 19 + i * 10, Ui.DIM);
        // bottom row: tag | switch | gear
        int by = y + CARD_H - 17;
        String tag = m.cat.label;
        int tw = textRenderer.getWidth(tag) + 8;
        ctx.fill(x + 6, by + 1, x + 6 + tw, by + 12, Ui.TAG);
        Ui.t(ctx, textRenderer, tag, x + 10, by + 3, Ui.TAGTXT);
        int gx = x + w - 20;
        ctx.fill(gx, by - 1, gx + 14, by + 13, Ui.TAG);
        ctx.fill(gx + 4, by + 3, gx + 10, by + 9, hov ? acc : Ui.TAGTXT);
        ctx.fill(gx + 6, by + 5, gx + 8, by + 7, Ui.TAG);
        int sx = gx - 6 - 26;
        if (m.action != null) Ui.t(ctx, textRenderer, "Open >", gx - 6 - textRenderer.getWidth("Open >"), by + 3, acc);
        else if (m.pinned) {
            String o = m.option();
            Ui.t(ctx, textRenderer, o, gx - 6 - textRenderer.getWidth(o), by + 3, acc);
        } else Ui.sw(ctx, sx, by, 26, 12, m.swAnim, acc);
    }

    private void renderRows(DrawContext ctx, String title, int acc, int viewTop) {
        Ui.t(ctx, textRenderer, title, cx(), topY(), Ui.TXT);
        double max = Math.max(0, SettingsUi.totalH(pageRows()) - (gridBot() - rowsTop()));
        scroll = Math.max(0, Math.min(scroll, max));
        ctx.enableScissor(cx(), viewTop, cx() + cw(), gridBot());
        SettingsUi.draw(ctx, textRenderer, pageRows(), cx(), rowsY(), cw(), acc, listening, dt);
        ctx.disableScissor();
    }

    private void renderRecorder(DrawContext ctx, int mx, int my, int acc, long now) {
        int x = cx(), y = topY(), w = cw();
        ctx.fill(x, y, x + w, y + 34, Ui.FIELD);
        Ui.border(ctx, x, y, w, 34, Ui.SOFT);
        boolean rec = Recorder.active();
        String state = !rec ? "Idle" : Recorder.paused() ? "PAUSED" : "REC";
        long sec = Recorder.elapsedMs() / 1000;
        String time = String.format("%02d:%02d", sec / 60, sec % 60);
        float pulse = rec && !Recorder.paused() ? (float) (0.5 + 0.5 * Math.sin(now / 250.0)) : 0f;
        ctx.fill(x + 8, y + 12, x + 18, y + 22, rec ? Anim.lerpColor(0xFFFCA5A5, 0xFFDC2626, pulse) : Ui.OFFSW);
        Ui.t(ctx, textRenderer, "Screen Recorder - " + state, x + 26, y + 7, Ui.TXT);
        Ui.t(ctx, textRenderer, rec ? time : "Records gameplay (no menus) to .minecraft/recordings", x + 26, y + 19, Ui.DIM);
        String[] labels = {"Start", Recorder.paused() ? "Resume" : "Pause", "Stop & Save"};
        int bw = (w - 12) / 3;
        for (int i = 0; i < 3; i++) {
            int bx = x + i * (bw + 6), by = y + 40;
            boolean hov = Ui.in(mx, my, bx, by, bw, 20);
            boolean red = i == 2;
            int bg = i == 0 ? (hov ? Anim.lerpColor(acc, 0xFF000000, 0.12f) : acc) : red ? (hov ? 0xFFDC2626 : 0xFFEF4444) : (hov ? Ui.CARD_H : Ui.FIELD);
            ctx.fill(bx, by, bx + bw, by + 20, bg);
            if (i == 1) Ui.border(ctx, bx, by, bw, 20, Ui.BORDER);
            Ui.tc(ctx, textRenderer, labels[i], bx + bw / 2, by + 6, i == 1 ? Ui.TXT : 0xFFFFFFFF);
        }
        double max = Math.max(0, SettingsUi.totalH(pageRows()) - (gridBot() - rowsTop()));
        scroll = Math.max(0, Math.min(scroll, max));
        ctx.enableScissor(cx(), rowsTop(), cx() + cw(), gridBot());
        SettingsUi.draw(ctx, textRenderer, pageRows(), cx(), rowsY(), cw(), acc, listening, dt);
        ctx.disableScissor();
    }

    private void renderProfiles(DrawContext ctx, int mx, int my, int acc) {
        int lw = cw() * 45 / 100, x1 = cx(), y0 = topY();
        Ui.t(ctx, textRenderer, "Profiles", x1, y0, Ui.TXT);
        for (int i = 0; i < Profiles.NAMES.length; i++) {
            int y = y0 + 14 + i * 22;
            boolean sel = i == Profiles.selected;
            ctx.fill(x1, y, x1 + lw, y + 20, sel ? (acc & 0x00FFFFFF) | 0x33000000 : Ui.CARD);
            Ui.border(ctx, x1, y, lw, 20, sel ? acc : Ui.SOFT);
            Ui.t(ctx, textRenderer, Profiles.NAMES[i], x1 + 8, y + 6, Ui.TXT);
        }
        int x2 = x1 + lw + 10, w2 = cw() - lw - 10;
        Ui.t(ctx, textRenderer, "More Buttons", x2, y0, Ui.TXT);
        String[] labels = {"Save Profile", "Load Profile", "Reset Settings", "Disable All", "Quit Game"};
        for (int i = 0; i < labels.length; i++) {
            int y = y0 + 14 + i * 26;
            boolean red = i == 3, hov = Ui.in(mx, my, x2, y, w2, 22);
            ctx.fill(x2, y, x2 + w2, y + 22, red ? (hov ? 0xFFDC2626 : 0xFFEF4444) : (hov ? Ui.CARD_H : Ui.FIELD));
            if (!red) Ui.border(ctx, x2, y, w2, 22, Ui.BORDER);
            Ui.tc(ctx, textRenderer, labels[i], x2 + w2 / 2, y + 7, red ? 0xFFFFFFFF : Ui.TXT);
        }
    }

    // ---------- graphics settings rows (wrap vanilla options)
    private List<Setting> buildSettingsRows() {
        GameOptions o = client.options;
        List<Setting> l = new ArrayList<>();
        if (Compat.graphicsOpt(client) != null) l.add(new Setting.Mode("Graphics Quality", 0, "Fast", "Fancy") {
            @Override public int get() { return Lx.nameOf(Compat.graphicsOpt(client)).equals("FAST") ? 0 : 1; }
            @Override public void set(int i) { Lx.setNamed(Compat.graphicsOpt(client), i == 0 ? "FAST" : "FANCY"); }
            @Override public boolean persist() { return false; }
        });
        l.add(new Setting.Slider("Render Distance", 2, 32, 1, 12, "") {
            @Override public double get() { return o.getViewDistance().getValue(); }
            @Override public void set(double d) { o.getViewDistance().setValue((int) Math.max(2, Math.min(32, Math.round(d)))); }
            @Override public boolean persist() { return false; }
        });
        l.add(new Setting.Slider("Brightness", 0, 100, 1, 50, "%") {
            @Override public double get() { return o.getGamma().getValue() * 100.0; }
            @Override public void set(double d) { o.getGamma().setValue(Math.max(0, Math.min(100, d)) / 100.0); }
            @Override public boolean persist() { return false; }
        });
        final int[] fpsVals = {30, 60, 120, 144, 240, 260};
        l.add(new Setting.Mode("FPS Limit", 0, "30", "60", "120", "144", "240", "Unlimited") {
            @Override public int get() {
                int v = o.getMaxFps().getValue();
                for (int i = 0; i < fpsVals.length; i++) if (v <= fpsVals[i]) return i;
                return fpsVals.length - 1;
            }
            @Override public void set(int i) { o.getMaxFps().setValue(fpsVals[Math.floorMod(i, fpsVals.length)]); }
            @Override public boolean persist() { return false; }
        });
        l.add(new Setting.Bool("VSync", false) {
            @Override public boolean get() { return o.getEnableVsync().getValue(); }
            @Override public void set(boolean b) { o.getEnableVsync().setValue(b); }
            @Override public boolean persist() { return false; }
        });
        l.add(new Setting.Bool("Dynamic FOV", true) {
            @Override public boolean get() { return o.getFovEffectScale().getValue() > 0.0; }
            @Override public void set(boolean b) { o.getFovEffectScale().setValue(b ? 1.0 : 0.0); }
            @Override public boolean persist() { return false; }
        });
        l.add(new Setting.Mode("Particles", 0, "Minimal", "Decreased", "All") {
            @Override public int get() {
                String m = Lx.nameOf(o.getParticles());
                return m.equals("MINIMAL") ? 0 : m.equals("DECREASED") ? 1 : 2;
            }
            @Override public void set(int i) {
                int v = Math.floorMod(i, 3);
                o.getParticles().setValue(Lx.named(o.getParticles(), v == 0 ? "MINIMAL" : v == 1 ? "DECREASED" : "ALL"));
            }
            @Override public boolean persist() { return false; }
        });
        return l;
    }

    // ---------- input
    @Override
    protected boolean onClick(double mx, double my, int button) {
        if (busy()) return true;
        pressBtn = button; moved = 0;
        boolean rowsPage = page == Page.SETTINGS || page == Page.RECORDER;
        if (rowsPage && my >= rowsTop() && my < gridBot() && mx >= cx()) {
            int i = SettingsUi.hit(pageRows(), cx(), rowsY(), cw(), mx, my);
            if (i >= 0 && pageRows().get(i) instanceof Setting.Slider sl) {
                dragging = sl; dragX = cx() + 8; dragW = cw() - 16;
                SettingsUi.sliderTo(sl, dragX, dragW, mx);
                return true;
            }
        }
        boolean area = (page.grid() && Ui.in(mx, my, cx(), gridTop(), cw(), gridBot() - gridTop()))
                || (rowsPage && Ui.in(mx, my, cx(), rowsTop(), cw(), gridBot() - rowsTop()));
        if (area) { potential = true; return true; }
        globalClick(mx, my);
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
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) { scroll -= vAmount * 20; return true; }

    @Override
    protected boolean onRelease(double mx, double my, int button) {
        if (dragging != null) { dragging = null; Config.save(); }
        else if (potential && moved < 6) {
            if (page == Page.SETTINGS || page == Page.RECORDER) rowsClick(mx, my);
            else cardClick(mx, my, pressBtn);
        }
        potential = false;
        return true;
    }

    private void rowsClick(double mx, double my) {
        int i = SettingsUi.hit(pageRows(), cx(), rowsY(), cw(), mx, my);
        if (i < 0) return;
        Setting s = pageRows().get(i);
        if (s instanceof Setting.Key) listening = s;
        else {
            SettingsUi.click(s, cx(), cw(), SettingsUi.hitTop, mx, my);
            Sfx.play(SoundEvents.UI_BUTTON_CLICK, 1.2f);
            Config.save();
        }
    }

    private void cardClick(double mx, double my, int button) {
        List<Module> l = visible();
        for (int i = 0; i < l.size(); i++) {
            int x = cx() + (i % cols()) * (cardW() + GAP);
            int y = gridTop() + (i / cols()) * (CARD_H + GAP) - (int) scroll;
            if (!Ui.in(mx, my, x, y, cardW(), CARD_H)) continue;
            Module m = l.get(i);
            boolean gear = Ui.in(mx, my, x + cardW() - 22, y + CARD_H - 19, 18, 17);
            if (gear || button == 1 || m.pinned && m.action == null) { client.setScreen(new ModuleSettingsScreen(this, m)); return; }
            if (m.action != null) { Sfx.play(SoundEvents.UI_BUTTON_CLICK, 1.0f); m.action.accept(client); return; }
            m.setEnabled(!m.enabled, client);
            Sfx.play(SoundEvents.UI_BUTTON_CLICK, m.enabled ? 1.3f : 0.9f);
            Config.save();
            return;
        }
    }

    private void globalClick(double mx, double my) {
        if (Ui.in(mx, my, px + pw - 22, py + 6, 18, 18)) { startClose(); return; }
        Page[] ps = Page.values();
        for (int i = 0; i < ps.length; i++) {
            if (Ui.in(mx, my, px + 6, py + HEAD + 8 + i * SB_H, SIDE - 12, SB_H - 2)) { setPage(ps[i]); return; }
        }
        if (page.grid()) {
            int ty = topY();
            searchFocus = Ui.in(mx, my, cx(), ty, searchW(), 18);
            if (Ui.in(mx, my, catX(), ty, 78, 18) && page == Page.MODULES) {
                catFilter++;
                if (catFilter >= Category.values().length) catFilter = -1;
                scroll = 0; pageAt = System.currentTimeMillis();
                Sfx.play(SoundEvents.UI_BUTTON_CLICK, 1.0f);
            } else if (Ui.in(mx, my, sortX(), ty, 62, 18)) { sortEnabled = !sortEnabled; pageAt = System.currentTimeMillis(); }
            else if (Ui.in(mx, my, gearX(), ty, 18, 18)) setPage(Page.SETTINGS);
        } else if (page == Page.RECORDER) {
            int x = cx(), y = topY() + 40, bw = (cw() - 12) / 3;
            for (int i = 0; i < 3; i++) {
                if (!Ui.in(mx, my, x + i * (bw + 6), y, bw, 20)) continue;
                Sfx.play(SoundEvents.UI_BUTTON_CLICK, 1.0f);
                RecorderModule r = Modules.RECORDER;
                if (i == 0) r.setEnabled(true, client);
                else if (i == 1) Recorder.togglePause(client);
                else r.stopAll(client);
                return;
            }
        } else if (page == Page.PROFILES) {
            int lw = cw() * 45 / 100, y0 = topY();
            for (int i = 0; i < Profiles.NAMES.length; i++) {
                if (Ui.in(mx, my, cx(), y0 + 14 + i * 22, lw, 20)) {
                    Profiles.selected = i;
                    Profiles.load(client, Profiles.NAMES[i]);
                    Config.save();
                    Sfx.play(SoundEvents.UI_BUTTON_CLICK, 1.0f);
                    toast("Profile: " + Profiles.NAMES[i]);
                    return;
                }
            }
            int x2 = cx() + lw + 10, w2 = cw() - lw - 10;
            for (int i = 0; i < 5; i++) {
                if (!Ui.in(mx, my, x2, y0 + 14 + i * 26, w2, 22)) continue;
                String n = Profiles.NAMES[Profiles.selected];
                Sfx.play(SoundEvents.UI_BUTTON_CLICK, 1.0f);
                switch (i) {
                    case 0 -> { Profiles.save(n); Config.save(); toast("Saved profile " + n); }
                    case 1 -> { Profiles.load(client, n); toast("Loaded profile " + n); }
                    case 2 -> { Profiles.reset(client); Config.save(); toast("Settings reset"); }
                    case 3 -> { Profiles.disableAll(client); Config.save(); toast("All modules disabled"); }
                    default -> client.scheduleStop();
                }
                return;
            }
        }
    }

    private void startClose() {
        if (closing) return;
        closing = true;
        closeAt = System.currentTimeMillis();
        Sfx.play(SoundEvents.UI_BUTTON_CLICK, 0.8f);
    }

    @Override public void close() { startClose(); }

    @Override
    protected boolean onChar(char chr) {
        if (searchFocus && chr >= 32 && query.length() < 20) {
            query += chr;
            scroll = 0; pageAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    @Override
    protected boolean onKey(int keyCode, int scanCode, int modifiers) {
        if (listening instanceof Setting.Key k) {
            k.code = keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode;
            listening = null;
            Config.save();
            return true;
        }
        if (searchFocus) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { if (!query.isEmpty()) query = query.substring(0, query.length() - 1); return true; }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) { searchFocus = false; return true; }
        }
        if (!searchFocus && Compat.matches(LeviXopClient.menuKey, keyCode, scanCode)) { startClose(); return true; }
        return false;
    }
}
