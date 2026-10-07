package dev.adda.levixop;

public enum Category {
    PERFORMANCE("Performance"), COMBAT("Combat"), MOVEMENT("Movement"), RENDER("Render"), HUD("HUD"), MISC("Misc");
    public final String label;
    Category(String l) { label = l; }
}
