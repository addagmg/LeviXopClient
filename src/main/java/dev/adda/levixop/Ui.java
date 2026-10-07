package dev.adda.levixop;

import net.minecraft.client.gui.DrawContext;

final class Ui {
    static final int BG = 0xF00B1220, SIDE = 0xFF0D1526, CARD = 0xFF131D33, CARD_H = 0xFF1A2640,
            BORDER = 0xFF24334F, TXT = 0xFFE6EDF7, DIM = 0xFF8A97AD, FIELD = 0xFF0F1830, OFFSW = 0xFF3A4660;

    static void sw(DrawContext ctx, int x, int y, int w, int h, boolean on, int acc) {
        ctx.fill(x, y, x + w, y + h, on ? acc : OFFSW);
        int k = on ? x + w - h + 1 : x + 1;
        ctx.fill(k, y + 1, k + h - 2, y + h - 1, 0xFFFFFFFF);
    }

    static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
