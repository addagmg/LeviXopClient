package dev.adda.levixop;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;

/** Official LeviXopclient look: white background, orange lines. */
final class Ui {
    static final int BG = 0xFFFFFFFF, SIDE = 0xFFFFF4E5, CARD = 0xFFFFFFFF, CARD_H = 0xFFFFF1DC,
            BORDER = 0xFFFFA94D, SOFT = 0xFFFFD9A8, TXT = 0xFF1F2937, DIM = 0xFF6B7280, FIELD = 0xFFFFFAF0,
            OFFSW = 0xFFD1D5DB, ICON = 0xFFFFE8CC, TAG = 0xFFFFE8CC, TAGTXT = 0xFFB45309;

    static void t(DrawContext c, TextRenderer tr, String s, int x, int y, int col) { c.drawText(tr, s, x, y, col, false); }
    static void t(DrawContext c, TextRenderer tr, OrderedText s, int x, int y, int col) { c.drawText(tr, s, x, y, col, false); }
    static void tc(DrawContext c, TextRenderer tr, String s, int cx, int y, int col) { c.drawText(tr, s, cx - tr.getWidth(s) / 2, y, col, false); }

    /** p = 0..1 animation progress */
    static void sw(DrawContext ctx, int x, int y, int w, int h, float p, int acc) {
        ctx.fill(x, y, x + w, y + h, Anim.lerpColor(OFFSW, acc, p));
        int k = x + 1 + Math.round((w - h) * p);
        ctx.fill(k, y + 1, k + h - 2, y + h - 1, 0xFFFFFFFF);
    }

    static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
