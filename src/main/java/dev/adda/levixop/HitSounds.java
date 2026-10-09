package dev.adda.levixop;

public class HitSounds extends Module {
    public final Setting.Mode sound = add(new Setting.Mode("Sound", 0, "Orb", "Crit", "Ding"));
    public final Setting.Slider volume = add(new Setting.Slider("Volume", 0.1, 1.0, 0.1, 0.8, ""));

    public HitSounds() { super("Hit Sounds", Category.COMBAT, "Plays a sound when you hit something"); }
}
