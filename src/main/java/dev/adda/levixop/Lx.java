package dev.adda.levixop;

import net.minecraft.client.option.SimpleOption;

/** Small helpers that behave the same on every Minecraft version. */
public final class Lx {
    /** Enum constant of an option's value type by name (avoids importing enums that moved between versions). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T named(SimpleOption<T> o, String name) {
        Class c = ((Enum) o.getValue()).getDeclaringClass();
        return (T) Enum.valueOf(c, name);
    }

    public static String nameOf(SimpleOption<?> o) { return ((Enum<?>) o.getValue()).name(); }

    public static <T> void setNamed(SimpleOption<T> o, String name) { o.setValue(named(o, name)); }
}
