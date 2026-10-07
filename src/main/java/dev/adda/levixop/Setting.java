package dev.adda.levixop;

public abstract class Setting {
    public final String name;
    protected Setting(String n) { name = n; }
    public boolean persist() { return true; }
    public abstract String save();
    public abstract void load(String s);

    public static class Bool extends Setting {
        private boolean v;
        public Bool(String n, boolean def) { super(n); v = def; }
        public boolean get() { return v; }
        public void set(boolean b) { v = b; }
        public String save() { return String.valueOf(get()); }
        public void load(String s) { set(Boolean.parseBoolean(s)); }
    }

    public static class Slider extends Setting {
        public final double min, max, step;
        public final String unit;
        private double v;
        public Slider(String n, double min, double max, double step, double def, String unit) {
            super(n); this.min = min; this.max = max; this.step = step; this.unit = unit; v = def;
        }
        public double get() { return v; }
        public void set(double d) {
            d = Math.max(min, Math.min(max, d));
            if (step > 0) d = Math.round((d - min) / step) * step + min;
            v = Math.max(min, Math.min(max, d));
        }
        public String save() { return String.valueOf(get()); }
        public void load(String s) { try { set(Double.parseDouble(s)); } catch (NumberFormatException ignored) {} }
    }

    public static class Mode extends Setting {
        public final String[] opts;
        private int i;
        public Mode(String n, int def, String... opts) { super(n); this.opts = opts; i = def; }
        public int get() { return i; }
        public void set(int v) { i = Math.floorMod(v, opts.length); }
        public int count() { return opts.length; }
        public String save() { return String.valueOf(get()); }
        public void load(String s) { try { set(Integer.parseInt(s)); } catch (NumberFormatException ignored) {} }
    }

    public static class Key extends Setting {
        public int code = -1;
        public Key(String n) { super(n); }
        public String save() { return String.valueOf(code); }
        public void load(String s) { try { code = Integer.parseInt(s); } catch (NumberFormatException ignored) {} }
    }
}
