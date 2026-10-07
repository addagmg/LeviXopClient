package dev.adda.levixop;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Fully custom-drawn ClickGUI: sidebar, search, module cards, per-module settings, profiles. */
public class ClickGui extends Screen {
    enum Page {
        MODULES("Modules", null), PERFORMANCE("Performance", Category.PERFORMANCE), COMBAT("Combat", Category.COMBAT),
        MOVEMENT("Movement", Category.MOVEMENT), RENDER("Render", Category.RENDER), HUD("HUD", Category.HUD),
        MISC("Misc", Category.MISC), PROFILES("Profiles", null), SETTINGS("Settings", null);
        final String label; final Category cat;
        Page(String l, Category c) { label = l; cat = c; }
        boolean grid() { return this != PROFILES && this != SETTINGS; }
    }

    private static final int SIDE = 92, HEAD = 30, SB_H = 19, GAP = 6, CARD_H = 56;
    private static final String FABRIC = FabricLoader.getInstance().getModContainer("fabricloader")
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");

    private int px, py, pw, ph;
    private Page page = Page.MODULES;
    private String query = "";
    private boolean searchFocus, sortEnabled, potential;
    private int catFilter = -1;
    private double scroll, pressX, pressY, moved;
    private int pressBtn;

    private Module modal;
    private final List<Setting> modalRows = new ArrayList<>();
    private List<Setting> settingsRows = new ArrayList<>();
    private Setting listening;
    private Setting.Slider dragging;
    private int dragX, dragW;

    public ClickGui() { super(Text.literal("LeviXopclient")); }

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
        ctx.fill(0, 0, width, height, 0x70000000);
    }

    // ---------- layout helpers
    private int cx() { return px + SIDE + 8; }
    private int cw() { return pw - SIDE - 16; }
    private int topY() { return py + HEAD + 6; }
    private int gridTop() { return topY() + 24; }
    private int gridBot() { return py + ph - 6; }
    private int cols() { return cw() >= 330 ? 3 : 2; }
    private int cardW() { return (cw() - (cols() - 1) * GAP) / cols(); }
    private int searchW() { return cw() - 18 - 62 - 78 - 12; }
    private int catX() { return cx() + searchW() + 4; }
    private int sortX() { return catX() + 78 + 4; }
    private int gearX() { return sortX() + 62 + 4; }
    private int settingsY() { return topY() + 14 - (int) scroll; }

    private List<Module> visible() {
        List<Module> l = new ArrayList<>();
        for (Module m : Modules.ALL) {
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

    // ---------- render
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        int acc = Theme.accent();
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, Ui.BORDER);
        ctx.fill(px, py, px + pw, py + ph, Ui.BG);
        renderHeader(ctx, acc);
        renderSidebar(ctx, mx, my, acc);
        switch (page) {
            case PROFILES -> renderProfiles(ctx, mx, my, acc);
            case SETTINGS -> renderSettings(ctx, acc);
            default -> renderGrid(ctx, mx, my, acc);
        }
        if (modal != null) renderModal(ctx, acc);
    }

    private void renderHeader(DrawContext ctx, int acc) {
        ctx.fill(px, py, px + pw, py + HEAD, Ui.SIDE);
        ctx.fill(px, py + HEAD - 1, px + pw, py + HEAD, Ui.BORDER);
        ctx.fill(px + 8, py + 5, px + 28, py + 25, acc);
        ctx.drawCenteredTextWithShadow(textRenderer, "LX", px + 18, py + 11, 0xFFFFFFFF);
        int tx = px + 34;
        ctx.drawTextWithShadow(textRenderer, "LeviXop", tx, py + 6, Ui.TXT);
        ctx.drawTextWithShadow(textRenderer, "Client", tx + textRenderer.getWidth("LeviXop"), py + 6, acc);
        ctx.drawTextWithShadow(textRenderer, "Performance | Smooth | Better FPS", tx, py + 17, Ui.DIM);
        if (pw >= 400) {
            String info = "MC 1.21.4 | Fabric " + FABRIC + "  FPS: " + client.getCurrentFps();
            int iw = textRenderer.getWidth(info) + 10, ix = px + pw - 28 - iw;
            ctx.fill(ix, py + 6, ix + iw, py + 24, Ui.FIELD);
            ctx.drawBorder(ix, py + 6, iw, 18, Ui.BORDER);
            ctx.drawTextWithShadow(textRenderer, info, ix + 5, py + 11, Ui.TXT);
        }
        ctx.drawTextWithShadow(textRenderer, "X", px + pw - 18, py + 11, Ui.DIM);
    }

    private void renderSidebar(DrawContext ctx, int mx, int my, int acc) {
        ctx.fill(px, py + HEAD, px + SIDE, py + ph, Ui.SIDE);
        Page[] ps = Page.values();
        for (int i = 0; i < ps.length; i++) {
            int y = py + HEAD + 8 + i * SB_H;
            boolean sel = ps[i] == page, hov = Ui.in(mx, my, px + 6, y, SIDE - 12, SB_H - 2);
            if (sel) {
                ctx.fill(px + 6, y, px + SIDE - 6, y + SB_H - 2, (acc & 0x00FFFFFF) | 0x40000000);
                ctx.fill(px + 6, y, px + 8, y + SB_H - 2, acc);
            } else if (hov) ctx.fill(px + 6, y, px + SIDE - 6, y + SB_H - 2, 0x22FFFFFF);
            ctx.fill(px + 12, y + 4, px + 20, y + 12, sel ? acc : Ui.DIM);
            ctx.drawTextWithShadow(textRenderer, ps[i].label, px + 26, y + 5, sel ? 0xFFFFFFFF : Ui.DIM);
        }
        int fy = py + ph - 24;
        if (fy > py + HEAD + 8 + ps.length * SB_H) {
            ctx.drawTextWithShadow(textRenderer, "Made by", px + 10, fy, Ui.DIM);
            ctx.drawTextWithShadow(textRenderer, "LeviXopClient", px + 10, fy + 10, Ui.TXT);
        }
    }

    private void renderGrid(DrawContext ctx, int mx, int my, int acc) {
        int tx = cx(), ty = topY();
        // search
        ctx.fill(tx, ty, tx + searchW(), ty + 18, Ui.FIELD);
        ctx.drawBorder(tx, ty, searchW(), 18, searchFocus ? acc : Ui.BORDER);
        boolean ph0 = query.isEmpty() && !searchFocus;
        String s = ph0 ? "Search modules..." : query + ((searchFocus && System.currentTimeMillis() / 500 % 2 == 0) ? "_" : "");
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(s, searchW() - 10), tx + 5, ty + 5, ph0 ? Ui.DIM : Ui.TXT);
        // category / sort / gear buttons
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
            int x = cx() + (i % cols()) * (cardW() + GAP);
            int y = gridTop() + (i / cols()) * (CARD_H + GAP) - (int) scroll;
            if (y + CARD_H < gridTop() || y > gridBot()) continue;
            drawCard(ctx, l.get(i), x, y, cardW(), mx, my, acc);
        }
        if (l.isEmpty()) ctx.drawTextWithShadow(textRenderer, "No modules found", cx() + 8, gridTop() + 8, Ui.DIM);
        ctx.disableScissor();
    }

    private void smallBtn(DrawContext ctx, int x, int y, int w, String label, int col) {
        ctx.fill(x, y, x + w, y + 18, Ui.FIELD);
        ctx.drawBorder(x, y, w, 18, Ui.BORDER);
        String t = textRenderer.trimToWidth(label, w - 6);
        ctx.drawTextWithShadow(textRenderer, t, x + (w - textRenderer.getWidth(t)) / 2, y + 5, col);
    }

    private void drawCard(DrawContext ctx, Module m, int x, int y, int w, int mx, int my, int acc) {
        boolean hov = Ui.in(mx, my, x, y, w, CARD_H) && my >= gridTop() && my < gridBot();
        ctx.fill(x, y, x + w, y + CARD_H, hov ? Ui.CARD_H : Ui.CARD);
        ctx.drawBorder(x, y, w, CARD_H, m.enabled && !m.pinned ? (acc & 0x00FFFFFF) | 0x80000000 : Ui.BORDER);
        // icon
        ctx.fill(x + 6, y + 6, x + 28, y + 28, 0xFF1E2B49);
        ctx.drawCenteredTextWithShadow(textRenderer, m.name.substring(0, 1), x + 17, y + 13, acc);
        // name + description
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(m.name, w - 34 - 36), x + 34, y + 7, Ui.TXT);
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(m.desc), w - 40);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            ctx.drawTextWithShadow(textRenderer, lines.get(i), x + 34, y + 18 + i * 10, Ui.DIM);
        }
        // toggle / action / option
        int sx = x + w - 6 - 24;
        if (m.action != null) ctx.drawTextWithShadow(textRenderer, ">", sx + 14, y + 7, acc);
        else if (m.pinned) {
            String o = m.option();
            ctx.drawTextWithShadow(textRenderer, o, x + w - 6 - textRenderer.getWidth(o), y + 7, acc);
        } else Ui.sw(ctx, sx, y + 7, 24, 11, m.enabled, acc);
        // tag + gear
        String tag = m.cat.label;
        int tw = textRenderer.getWidth(tag) + 8;
        ctx.fill(x + 6, y + CARD_H - 15, x + 6 + tw, y + CARD_H - 4, 0xFF1B2A4A);
        ctx.drawTextWithShadow(textRenderer, tag, x + 10, y + CARD_H - 13, 0xFF8FB4FF);
        int gx = x + w - 18, gy = y + CARD_H - 15;
        ctx.fill(gx, gy, gx + 12, gy + 11, 0xFF1B2A4A);
        ctx.fill(gx + 4, gy + 3, gx + 8, gy + 8, hov ? acc : Ui.DIM);
    }

    private void renderProfiles(DrawContext ctx, int mx, int my, int acc) {
        int lw = cw() * 45 / 100, x1 = cx(), y0 = topY();
        ctx.drawTextWithShadow(textRenderer, "Profiles", x1, y0, Ui.TXT);
        for (int i = 0; i < Profiles.NAMES.length; i++) {
            int y = y0 + 14 + i * 22;
            boolean sel = i == Profiles.selected;
            ctx.fill(x1, y, x1 + lw, y + 20, sel ? (acc & 0x00FFFFFF) | 0x50000000 : Ui.CARD);
            ctx.drawBorder(x1, y, lw, 20, sel ? acc : Ui.BORDER);
            ctx.drawTextWithShadow(textRenderer, Profiles.NAMES[i], x1 + 8, y + 6, Ui.TXT);
        }
        int x2 = x1 + lw + 10, w2 = cw() - lw - 10;
        ctx.drawTextWithShadow(textRenderer, "More Buttons", x2, y0, Ui.TXT);
        String[] labels = {"Save Profile", "Load Profile", "Reset Settings", "Disable All", "Quit Game"};
        for (int i = 0; i < labels.length; i++) {
            int y = y0 + 14 + i * 26;
            boolean red = i == 3;
            boolean hov = Ui.in(mx, my, x2, y, w2, 22);
            ctx.fill(x2, y, x2 + w2, y + 22, red ? (hov ? 0xFFDC2626 : 0xFFB91C1C) : (hov ? Ui.CARD_H : Ui.CARD));
            ctx.drawBorder(x2, y, w2, 22, red ? 0xFFEF4444 : Ui.BORDER);
            ctx.drawCenteredTextWithShadow(textRenderer, labels[i], x2 + w2 / 2, y + 7, 0xFFFFFFFF);
        }
    }

    private void renderSettings(DrawContext ctx, int acc) {
        ctx.drawTextWithShadow(textRenderer, "Graphics Settings", cx(), topY(), Ui.TXT);
        int viewTop = topY() + 12;
        double max = Math.max(0, settingsRows.size() * SettingsUi.ROWH - (gridBot() - viewTop));
        scroll = Math.max(0, Math.min(scroll, max));
        ctx.enableScissor(cx(), viewTop, cx() + cw(), gridBot());
        SettingsUi.draw(ctx, textRenderer, settingsRows, cx(), settingsY(), cw(), acc, null);
        ctx.disableScissor();
    }

    // ---------- module settings modal
    private int[] modalRect() {
        int w = Math.min(260, pw - 20), h = 32 + modalRows.size() * SettingsUi.ROWH + 6;
        return new int[]{px + (pw - w) / 2, py + (ph - h) / 2, w, h};
    }

    private void openModal(Module m) {
        modal = m;
        listening = null;
        modalRows.clear();
        if (!m.pinned) {
            modalRows.add(new Setting.Bool("Enabled", m.enabled) {
                @Override public boolean get() { return m.enabled; }
                @Override public void set(boolean b) { m.setEnabled(b, client); }
                @Override public boolean persist() { return false; }
            });
        }
        if (m.opts != null) {
            modalRows.add(new Setting.Mode("Mode", m.opt, m.opts) {
                @Override public int get() { return m.opt; }
                @Override public void set(int i) { m.cycleOption(client); }
                @Override public boolean persist() { return false; }
            });
        }
        modalRows.addAll(m.settings);
        if (!m.pinned) modalRows.add(m.key);
    }

    private void renderModal(DrawContext ctx, int acc) {
        ctx.fill(px, py, px + pw, py + ph, 0xB0000000);
        int[] r = modalRect();
        ctx.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, acc);
        ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], Ui.BG);
        ctx.fill(r[0] + 6, r[1] + 6, r[0] + 24, r[1] + 24, 0xFF1E2B49);
        ctx.drawCenteredTextWithShadow(textRenderer, modal.name.substring(0, 1), r[0] + 15, r[1] + 11, acc);
        ctx.drawTextWithShadow(textRenderer, modal.name, r[0] + 30, r[1] + 11, Ui.TXT);
        ctx.drawTextWithShadow(textRenderer, "X", r[0] + r[2] - 14, r[1] + 11, Ui.DIM);
        SettingsUi.draw(ctx, textRenderer, modalRows, r[0] + 6, r[1] + 28, r[2] - 12, acc, listening);
    }

    // ---------- settings page rows (wrap vanilla options)
    private List<Setting> buildSettingsRows() {
        GameOptions o = client.options;
        List<Setting> l = new ArrayList<>();
        l.add(new Setting.Mode("Graphics Quality", 0, "Fast", "Fancy") {
            @Override public int get() { return o.getGraphicsMode().getValue() == GraphicsMode.FAST ? 0 : 1; }
            @Override public void set(int i) { o.getGraphicsMode().setValue(i == 0 ? GraphicsMode.FAST : GraphicsMode.FANCY); }
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
                ParticlesMode m = o.getParticles().getValue();
                return m == ParticlesMode.MINIMAL ? 0 : m == ParticlesMode.DECREASED ? 1 : 2;
            }
            @Override public void set(int i) {
                int v = Math.floorMod(i, 3);
                o.getParticles().setValue(v == 0 ? ParticlesMode.MINIMAL : v == 1 ? ParticlesMode.DECREASED : ParticlesMode.ALL);
            }
            @Override public boolean persist() { return false; }
        });
        return l;
    }

    // ---------- input
    private void sliderTo(double mx) {
        if (dragging == null) return;
        double t = Math.max(0, Math.min(1, (mx - dragX) / (double) dragW));
        dragging.set(dragging.min + t * (dragging.max - dragging.min));
    }

    private void rowClick(Setting s, int x, int w, double mx) {
        if (s instanceof Setting.Bool b) b.set(!b.get());
        else if (s instanceof Setting.Mode m) m.set(m.get() + 1);
        else if (s instanceof Setting.Slider sl) {
            dragging = sl; dragX = SettingsUi.trackX(x, w); dragW = SettingsUi.trackW(x, w);
            sliderTo(mx);
        } else if (s instanceof Setting.Key) listening = s;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (modal != null) {
            int[] r = modalRect();
            if (Ui.in(mx, my, r[0] + r[2] - 20, r[1] + 6, 16, 16) || !Ui.in(mx, my, r[0], r[1], r[2], r[3])) {
                modal = null; listening = null; Config.save();
                return true;
            }
            int i = SettingsUi.hit(modalRows, r[0] + 6, r[1] + 28, r[2] - 12, mx, my);
            if (i >= 0) rowClick(modalRows.get(i), r[0] + 6, r[2] - 12, mx);
            return true;
        }
        pressX = mx; pressY = my; pressBtn = button; moved = 0;
        // sliders on the settings page start dragging immediately
        if (page == Page.SETTINGS && my >= topY() + 12 && my < gridBot()) {
            int i = SettingsUi.hit(settingsRows, cx(), settingsY(), cw(), mx, my);
            if (i >= 0 && settingsRows.get(i) instanceof Setting.Slider) {
                rowClick(settingsRows.get(i), cx(), cw(), mx);
                return true;
            }
        }
        boolean scrollArea = (page.grid() && Ui.in(mx, my, cx(), gridTop(), cw(), gridBot() - gridTop()))
                || (page == Page.SETTINGS && Ui.in(mx, my, cx(), topY() + 12, cw(), gridBot() - topY() - 12));
        if (scrollArea) { potential = true; return true; }
        globalClick(mx, my, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { sliderTo(mx); return true; }
        if (modal != null) return true;
        moved += Math.abs(dx) + Math.abs(dy);
        if (potential) scroll -= dy;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (modal == null) scroll -= vAmount * 20;
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) { dragging = null; Config.save(); }
        else if (potential && moved < 6) {
            if (page == Page.SETTINGS) settingsClick(mx, my);
            else cardClick(mx, my, pressBtn);
        }
        potential = false;
        return true;
    }

    private void settingsClick(double mx, double my) {
        int i = SettingsUi.hit(settingsRows, cx(), settingsY(), cw(), mx, my);
        if (i >= 0) rowClick(settingsRows.get(i), cx(), cw(), mx);
    }

    private void cardClick(double mx, double my, int button) {
        List<Module> l = visible();
        for (int i = 0; i < l.size(); i++) {
            int x = cx() + (i % cols()) * (cardW() + GAP);
            int y = gridTop() + (i / cols()) * (CARD_H + GAP) - (int) scroll;
            if (!Ui.in(mx, my, x, y, cardW(), CARD_H)) continue;
            Module m = l.get(i);
            boolean gear = Ui.in(mx, my, x + cardW() - 20, y + CARD_H - 17, 16, 15);
            if (gear || button == 1) { openModal(m); return; }
            if (m.action != null) { m.action.accept(client); return; }
            if (m.pinned) { openModal(m); return; }
            m.setEnabled(!m.enabled, client);
            Config.save();
            return;
        }
    }

    private void globalClick(double mx, double my, int button) {
        if (Ui.in(mx, my, px + pw - 22, py + 6, 18, 18)) { close(); return; }
        Page[] ps = Page.values();
        for (int i = 0; i < ps.length; i++) {
            if (Ui.in(mx, my, px + 6, py + HEAD + 8 + i * SB_H, SIDE - 12, SB_H - 2)) {
                page = ps[i]; scroll = 0; searchFocus = false;
                return;
            }
        }
        if (page.grid()) {
            int ty = topY();
            searchFocus = Ui.in(mx, my, cx(), ty, searchW(), 18);
            if (Ui.in(mx, my, catX(), ty, 78, 18) && page == Page.MODULES) {
                catFilter++;
                if (catFilter >= Category.values().length) catFilter = -1;
                scroll = 0;
            } else if (Ui.in(mx, my, sortX(), ty, 62, 18)) sortEnabled = !sortEnabled;
            else if (Ui.in(mx, my, gearX(), ty, 18, 18)) { page = Page.SETTINGS; scroll = 0; }
        } else if (page == Page.PROFILES) {
            int lw = cw() * 45 / 100, y0 = topY();
            for (int i = 0; i < Profiles.NAMES.length; i++) {
                if (Ui.in(mx, my, cx(), y0 + 14 + i * 22, lw, 20)) {
                    Profiles.selected = i;
                    Profiles.load(client, Profiles.NAMES[i]);
                    Config.save();
                    toast("Profile: " + Profiles.NAMES[i]);
                    return;
                }
            }
            int x2 = cx() + lw + 10, w2 = cw() - lw - 10;
            for (int i = 0; i < 5; i++) {
                if (!Ui.in(mx, my, x2, y0 + 14 + i * 26, w2, 22)) continue;
                String n = Profiles.NAMES[Profiles.selected];
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

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocus && modal == null && chr >= 32 && query.length() < 20) {
            query += chr;
            scroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            if (listening instanceof Setting.Key k) k.code = keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode;
            listening = null;
            Config.save();
            return true;
        }
        if (searchFocus) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { if (!query.isEmpty()) query = query.substring(0, query.length() - 1); return true; }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) { searchFocus = false; return true; }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && modal != null) { modal = null; Config.save(); return true; }
        if (!searchFocus && LeviXopClient.menuKey.matchesKey(keyCode, scanCode)) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() { Config.save(); super.close(); }
}
