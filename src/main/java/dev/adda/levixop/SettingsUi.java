package dev.adda.levixop;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Setting rows drawn like the reference "Module Settings" panel. */
final class SettingsUi {
    static final int GAP = 4, NUMW = 74;
    static int hitTop;
    private static final Map<Setting, Float> ANIM = new IdentityHashMap<>();

    static int rowH(Setting s) {
        if (s instanceof Setting.Slider) return 38;
        if (s instanceof Setting.Multi m) return 24 + m.opts.length * 16 + 2;
        return 28;
    }

    static int totalH(List<Setting> l) {
        int h = 0;
        for (Setting s : l) h += rowH(s) + GAP;
        return h;
    }

    static String keyName(int code) { return Compat.keyName(code); }

    static String fmt(Setting.Slider s) {
        String v = s.step >= 1 ? String.valueOf((int) Math.round(s.get())) : String.format("%.1f", s.get());
        return v + s.unit;
    }

    static int hit(List<Setting> rows, int x, int y, int w, double mx, double my) {
        int top = y;
        for (int i = 0; i < rows.size(); i++) {
            int h = rowH(rows.get(i));
            if (mx >= x && mx < x + w && my >= top && my < top + h) { hitTop = top; return i; }
            top += h + GAP;
        }
        return -1;
    }

    /** click for everything except sliders (drag) and keys (listening) */
    static void click(Setting s, int x, int w, int rowTop, double mx, double my) {
        if (s instanceof Setting.Bool b) b.set(!b.get());
        else if (s instanceof Setting.Mode m) m.set(m.get() + 1);
        else if (s instanceof Setting.Number n) {
            int bx = x + w - 8 - NUMW;
            if (mx >= bx && mx < bx + 24) n.set(n.get() - n.step);
            else if (mx >= bx + NUMW - 24 && mx < bx + NUMW) n.set(n.get() + n.step);
        } else if (s instanceof Setting.Multi mu) {
            int i = (int) ((my - (rowTop + 22)) / 16);
            if (i >= 0 && i < mu.opts.length) mu.set(i, !mu.get(i));
        }
    }

    static void sliderTo(Setting.Slider sl, int trackX, int trackW, double mx) {
        double t = Math.max(0, Math.min(1, (mx - trackX) / (double) Math.max(1, trackW)));
        sl.set(sl.min + t * (sl.max - sl.min));
    }

    private static float anim(Setting s, float target, float dt) {
        float cur = ANIM.getOrDefault(s, target);
        cur = Anim.approach(cur, target, dt);
        ANIM.put(s, cur);
        return cur;
    }

    static void draw(DrawContext ctx, TextRenderer tr, List<Setting> rows, int x, int y, int w, int acc, Setting listening, float dt) {
        int top = y;
        for (Setting s : rows) {
            int h = rowH(s);
            ctx.fill(x, top, x + w, top + h, Ui.FIELD);
            ctx.drawBorder(x, top, w, h, Ui.SOFT);
            int ty = top + (Math.min(h, 28) - 8) / 2;
            if (s instanceof Setting.Bool b) {
                Ui.t(ctx, tr, s.name, x + 8, ty, Ui.TXT);
                Ui.sw(ctx, x + w - 8 - 26, top + (h - 12) / 2, 26, 12, anim(s, b.get() ? 1f : 0f, dt), acc);
            } else if (s instanceof Setting.Slider sl) {
                Ui.t(ctx, tr, s.name, x + 8, top + 6, Ui.TXT);
                String v = fmt(sl);
                Ui.t(ctx, tr, v, x + w - 8 - tr.getWidth(v), top + 6, Ui.DIM);
                int tx = x + 8, tw = w - 16, tk = top + h - 12;
                ctx.fill(tx, tk, tx + tw, tk + 4, 0xFFE5E7EB);
                double t = (sl.get() - sl.min) / Math.max(0.0001, sl.max - sl.min);
                int fx = tx + (int) (tw * Math.max(0, Math.min(1, t)));
                ctx.fill(tx, tk, fx, tk + 4, acc);
                ctx.fill(fx - 4, tk - 3, fx + 4, tk + 7, acc);
                ctx.fill(fx - 2, tk - 1, fx + 2, tk + 5, 0xFFFFFFFF);
            } else if (s instanceof Setting.Number n) {
                Ui.t(ctx, tr, s.name, x + 8, ty, Ui.TXT);
                int bx = x + w - 8 - NUMW, by = top + 4, bh = h - 8;
                ctx.fill(bx, by, bx + NUMW, by + bh, 0xFFFFFFFF);
                ctx.drawBorder(bx, by, NUMW, bh, Ui.BORDER);
                Ui.tc(ctx, tr, "-", bx + 12, ty, acc);
                Ui.tc(ctx, tr, String.valueOf(n.get()), bx + NUMW / 2, ty, Ui.TXT);
                Ui.tc(ctx, tr, "+", bx + NUMW - 12, ty, acc);
            } else if (s instanceof Setting.Multi mu) {
                Ui.t(ctx, tr, s.name, x + 8, top + 6, Ui.TXT);
                for (int i = 0; i < mu.opts.length; i++) {
                    int cy = top + 22 + i * 16;
                    ctx.fill(x + 10, cy + 2, x + 20, cy + 12, 0xFFFFFFFF);
                    ctx.drawBorder(x + 10, cy + 2, 10, 10, Ui.BORDER);
                    if (mu.get(i)) ctx.fill(x + 12, cy + 4, x + 18, cy + 10, acc);
                    Ui.t(ctx, tr, mu.opts[i], x + 26, cy + 3, Ui.TXT);
                }
            } else {
                Ui.t(ctx, tr, s.name, x + 8, ty, Ui.TXT);
                String label = "";
                if (s instanceof Setting.Mode m) label = m.opts[m.get()];
                else if (s instanceof Setting.Key k) label = (listening == s) ? "press a key..." : keyName(k.code);
                int bw = Math.min(140, w / 2), bx = x + w - 8 - bw, by = top + 4, bh = h - 8;
                ctx.fill(bx, by, bx + bw, by + bh, 0xFFFFFFFF);
                ctx.drawBorder(bx, by, bw, bh, listening == s ? acc : Ui.BORDER);
                Ui.t(ctx, tr, tr.trimToWidth(label, bw - 18), bx + 5, ty, Ui.TXT);
                if (s instanceof Setting.Mode) {
                    int ax = bx + bw - 11, ay = by + bh / 2 - 1;
                    ctx.fill(ax, ay, ax + 7, ay + 1, acc);
                    ctx.fill(ax + 1, ay + 1, ax + 6, ay + 2, acc);
                    ctx.fill(ax + 2, ay + 2, ax + 5, ay + 3, acc);
                    ctx.fill(ax + 3, ay + 3, ax + 4, ay + 4, acc);
                }
            }
            top += h + GAP;
        }
    }
}
