package dev.adda.levixop;

public abstract class Setting {
    public final String name;
    public String def;
    protected Setting(String n) { name = n; }
    public boolean persist() { return true; }
    public abstract String save();
    public abstract void load(String s);
    public void reset() { if (def != null) load(def); }

    public static class Bool extends Setting {
        private boolean v;
        public Bool(String n, boolean d) { super(n); v = d; }
        public boolean get() { return v; }
        public void set(boolean b) { v = b; }
        public String save() { return String.valueOf(get()); }
        public void load(String s) { set(Boolean.parseBoolean(s)); }
    }

    public static class Slider extends Setting {
        public final double min, max, step;
        public final String unit;
        private double v;
        public Slider(String n, double min, double max, double step, double d, String unit) {
            super(n); this.min = min; this.max = max; this.step = step; this.unit = unit; v = d;
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
        public Mode(String n, int d, String... opts) { super(n); this.opts = opts; i = d; }
        public int get() { return i; }
        public void set(int v) { i = Math.floorMod(v, opts.length); }
        public int count() { return opts.length; }
        public String save() { return String.valueOf(get()); }
        public void load(String s) { try { set(Integer.parseInt(s)); } catch (NumberFormatException ignored) {} }
    }

    public static class Number extends Setting {
        public final int min, max, step;
        private int v;
        public Number(String n, int min, int max, int step, int d) { super(n); this.min = min; this.max = max; this.step = step; v = d; }
        public int get() { return v; }
        public void set(int x) { v = Math.max(min, Math.min(max, x)); }
        public String save() { return String.valueOf(get()); }
        public void load(String s) { try { set(Integer.parseInt(s)); } catch (NumberFormatException ignored) {} }
    }

    public static class Multi extends Setting {
        public final String[] opts;
        private final boolean[] v;
        public Multi(String n, String... opts) { super(n); this.opts = opts; v = new boolean[opts.length]; java.util.Arrays.fill(v, true); }
        public boolean get(int i) { return v[i]; }
        public void set(int i, boolean b) { v[i] = b; }
        public String save() {
            StringBuilder sb = new StringBuilder();
            for (boolean b : v) sb.append(b ? '1' : '0');
            return sb.toString();
        }
        public void load(String s) { for (int i = 0; i < v.length && i < s.length(); i++) v[i] = s.charAt(i) == '1'; }
    }

    public static class Key extends Setting {
        public int code = -1;
        public Key(String n) { super(n); }
        public String save() { return String.valueOf(code); }
        public void load(String s) { try { code = Integer.parseInt(s); } catch (NumberFormatException ignored) {} }
    }
}
