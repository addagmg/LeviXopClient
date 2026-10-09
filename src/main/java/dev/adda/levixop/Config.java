package dev.adda.levixop;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    public static Path dir() { return FabricLoader.getInstance().getConfigDir(); }
    public static Path main() { return dir().resolve("levixopclient.properties"); }

    private static String key(Module m) { return m.name.replace(' ', '_'); }
    private static String sk(Setting s) { return s.name.replace(' ', '_'); }

    public static void load() { read(main(), null); }
    public static void save() { write(main()); }

    public static void write(Path f) {
        Properties p = new Properties();
        for (Module m : Modules.ALL) {
            String k = key(m);
            if (!m.noSave) p.setProperty(k + ".on", String.valueOf(m.enabled));
            if (m.opts != null) p.setProperty(k + ".opt", String.valueOf(m.opt));
            if (m.key.code > 0) p.setProperty(k + ".key", String.valueOf(m.key.code));
            for (Setting s : m.settings) if (s.persist()) p.setProperty(k + ".s." + sk(s), s.save());
            if (m instanceof HudModule h) {
                p.setProperty(k + ".x", String.valueOf(h.x));
                p.setProperty(k + ".y", String.valueOf(h.y));
            }
        }
        p.setProperty("profile", Profiles.NAMES[Profiles.selected]);
        try (OutputStream out = Files.newOutputStream(f)) {
            p.store(out, "LeviXopclient");
        } catch (IOException ignored) {}
    }

    /** mc == null: startup load (hooks run on first tick). Otherwise apply live. */
    public static void read(Path f, MinecraftClient mc) {
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(f)) { p.load(in); } catch (IOException e) { return; }
        for (Module m : Modules.ALL) {
            String k = key(m);
            try {
                String on = p.getProperty(k + ".on");
                if (on != null && !m.pinned && !m.noSave) {
                    boolean b = Boolean.parseBoolean(on);
                    if (mc == null) m.enabled = b; else m.setEnabled(b, mc);
                }
                for (Setting s : m.settings) {
                    String v = p.getProperty(k + ".s." + sk(s));
                    if (v != null && s.persist()) s.load(v);
                }
                String kc = p.getProperty(k + ".key");
                m.key.code = kc == null ? -1 : Integer.parseInt(kc);
                String o = p.getProperty(k + ".opt");
                if (o != null && m.opts != null) {
                    int v = Integer.parseInt(o);
                    if (v >= 0 && v < m.opts.length) {
                        m.opt = v;
                        if (mc != null && m.enabled && !m.pinned) { m.onDisable(mc); m.onEnable(mc); }
                    }
                }
                if (m instanceof HudModule h) {
                    String x = p.getProperty(k + ".x"), y = p.getProperty(k + ".y");
                    if (x != null) h.x = Integer.parseInt(x);
                    if (y != null) h.y = Integer.parseInt(y);
                }
            } catch (NumberFormatException ignored) {}
        }
    }
}
