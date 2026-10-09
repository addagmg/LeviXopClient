package dev.adda.levixop;

public final class Theme {
    public static final String[] NAMES = {"Orange", "Blue", "Purple", "Green", "Red", "Pink"};
    private static final int[] COL = {0xFFFF8A00, 0xFF3B82F6, 0xFF7C5CFF, 0xFF22C55E, 0xFFEF4444, 0xFFEC4899};

    public static int accent() {
        int i = Modules.THEME == null ? 0 : Modules.THEME.opt;
        return COL[Math.floorMod(i, COL.length)];
    }
}
