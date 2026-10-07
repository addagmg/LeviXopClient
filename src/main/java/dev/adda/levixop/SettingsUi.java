package dev.adda.levixop;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

import java.util.List;

final class SettingsUi {
    static final int ROWH = 24;

    static int trackX(int x, int w) { return x + w * 45 / 100; }
    static int trackW(int x, int w) { return Math.max(20, (x + w - 8 - 34) - trackX(x, w)); }

    static String keyName(int code) {
        if (code <= 0) return "None";
        try { return InputUtil.fromKeyCode(code, 0).getLocalizedText().getString(); }
        catch (Exception e) { return "Key " + code; }
    }

    static String fmt(Setting.Slider s) {
        String v = s.step >= 1 ? String.valueOf((int) Math.round(s.get())) : String.format("%.1f", s.get());
        return v + s.unit;
    }

    static int hit(List<Setting> rows, int x, int y, int w, double mx, double my) {
        if (mx < x || mx >= x + w) return -1;
        for (int i = 0; i < rows.size(); i++) {
            int ry = y + i * ROWH;
            if (my >= ry && my < ry + ROWH - 2) return i;
        }
        return -1;
    }

    static void draw(DrawContext ctx, TextRenderer tr, List<Setting> rows, int x, int y, int w, int acc, Setting listening) {
        for (int i = 0; i < rows.size(); i++) {
            Setting s = rows.get(i);
            int ry = y + i * ROWH, rh = ROWH - 2, ty = ry + (rh - 8) / 2;
            ctx.fill(x, ry, x + w, ry + rh, Ui.CARD);
            ctx.drawTextWithShadow(tr, s.name, x + 8, ty, Ui.TXT);
            int cx = trackX(x, w);
            if (s instanceof Setting.Bool b) {
                Ui.sw(ctx, x + w - 8 - 24, ry + (rh - 11) / 2, 24, 11, b.get(), acc);
            } else if (s instanceof Setting.Slider sl) {
                int tw = trackW(x, w), by = ry + rh / 2 - 2;
                ctx.fill(cx, by, cx + tw, by + 4, 0xFF263250);
                double t = (sl.get() - sl.min) / Math.max(0.0001, sl.max - sl.min);
                int fx = cx + (int) (tw * Math.max(0, Math.min(1, t)));
                ctx.fill(cx, by, fx, by + 4, acc);
                ctx.fill(fx - 2, by - 2, fx + 2, by + 6, 0xFFFFFFFF);
                String v = fmt(sl);
                ctx.drawTextWithShadow(tr, v, x + w - 8 - tr.getWidth(v), ty, 0xFFB8C4D9);
            } else {
                String label;
                if (s instanceof Setting.Mode m) label = m.opts[m.get()];
                else if (s instanceof Setting.Key k) label = (listening == s) ? "press a key..." : keyName(k.code);
                else label = "";
                int bx = cx, bw = x + w - 8 - cx;
                ctx.fill(bx, ry + 3, bx + bw, ry + rh - 3, Ui.FIELD);
                ctx.drawBorder(bx, ry + 3, bw, rh - 6, listening == s ? acc : Ui.BORDER);
                ctx.drawTextWithShadow(tr, tr.trimToWidth(label, bw - 8), bx + 4, ty, Ui.TXT);
            }
        }
    }
}
