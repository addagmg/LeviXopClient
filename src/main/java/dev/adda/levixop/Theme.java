package dev.adda.levixop;

public final class Theme {
    public static final String[] NAMES = {"Purple", "Blue", "Green", "Red", "Pink", "Orange"};
    private static final int[] COL = {0xFF7C5CFF, 0xFF3B82F6, 0xFF22C55E, 0xFFEF4444, 0xFFEC4899, 0xFFF59E0B};

    public static int accent() {
        int i = Modules.THEME == null ? 1 : Modules.THEME.opt;
        return COL[Math.floorMod(i, COL.length)];
    }
}
