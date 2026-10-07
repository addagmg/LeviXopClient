package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.math.MathHelper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();

    static <T extends Module> T reg(T m) { ALL.add(m); return m; }

    static HudModule hud(String n, Category c, String d, Function<MinecraftClient, List<String>> f) {
        return reg(new HudModule(n, c, d, f));
    }

    public static List<Module> in(Category c) {
        List<Module> l = new ArrayList<>();
        for (Module m : ALL) if (m.cat == c) l.add(m);
        return l;
    }

    public static final Module THEME, FRIENDS, NO_BG, RECORDER;
    public static final HitSounds HIT_SOUNDS;

    static {
        // ---- default-on HUD (created first so they get the first screen slots)
        hud("FPS Display", Category.HUD, "Frames per second", mc -> List.of("FPS: " + mc.getCurrentFps())).on();
        hud("CPS Counter", Category.COMBAT, "Left | right clicks per second",
                mc -> List.of("CPS: " + Stats.cps(Stats.L) + " | " + Stats.cps(Stats.R))).on();
        hud("Ping Display", Category.HUD, "Your latency", mc -> {
            PlayerListEntry e = mc.getNetworkHandler() == null ? null
                    : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            return List.of("Ping: " + (e == null ? "-" : e.getLatency() + " ms"));
        }).on();
        hud("Coordinates", Category.HUD, "XYZ position", mc -> List.of(String.format("XYZ: %.1f %.1f %.1f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ()))).on();
        hud("Server IP", Category.HUD, "Address of the server you are on", mc -> {
            ServerInfo si = mc.getCurrentServerEntry();
            return List.of("IP: " + (si == null ? "Singleplayer" : si.address));
        }).on();

        // ---- HUD tab controls
        NO_BG = reg(new Module("Remove HUD Background", Category.HUD, "Hides the background of every HUD element"));
        reg(new Module("HUD Editor", Category.HUD, "Drag HUD elements to move them")
                .action(mc -> { if (mc.world != null) mc.setScreen(new HudEditorScreen(mc.currentScreen)); }));

        // ---- HUD
        hud("Direction", Category.HUD, "Facing direction and yaw", mc -> List.of(String.format("Facing: %s (%.0f)",
                mc.player.getHorizontalFacing().asString(), MathHelper.wrapDegrees(mc.player.getYaw()))));
        hud("Biome", Category.HUD, "Current biome", mc -> List.of("Biome: " +
                mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("?")));
        hud("Clock", Category.HUD, "Real time clock",
                mc -> List.of(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        hud("Session Time", Category.HUD, "Time since you launched the game", mc -> {
            long s = (System.currentTimeMillis() - Stats.START) / 1000;
            return List.of(String.format("Session: %d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60));
        });
        hud("Death Counter", Category.HUD, "Deaths this session", mc -> List.of("Deaths: " + Stats.deaths));
        hud("Memory Usage", Category.HUD, "Used / max game memory", mc -> {
            Runtime rt = Runtime.getRuntime();
            return List.of("Mem: " + (rt.totalMemory() - rt.freeMemory()) / 1048576L + " / " + rt.maxMemory() / 1048576L + " MB");
        });
        hud("Entity Counter", Category.HUD, "Loaded entities around you", mc -> {
            int n = 0;
            for (Entity e : mc.world.getEntities()) n++;
            return List.of("Entities: " + n);
        });
        hud("Waypoints", Category.HUD, "Nearest waypoints (/wp add <name>)", mc -> {
            String dim = mc.world.getRegistryKey().getValue().toString();
            List<Social.Waypoint> l = new ArrayList<>();
            for (Social.Waypoint w : Social.WAYPOINTS) if (w.dim().equals(dim)) l.add(w);
            l.sort((a, b) -> Double.compare(mc.player.squaredDistanceTo(a.x(), a.y(), a.z()),
                    mc.player.squaredDistanceTo(b.x(), b.y(), b.z())));
            List<String> out = new ArrayList<>();
            for (int i = 0; i < Math.min(4, l.size()); i++) {
                Social.Waypoint w = l.get(i);
                out.add(w.name() + ": " + (int) Math.sqrt(mc.player.squaredDistanceTo(w.x(), w.y(), w.z())) + "m");
            }
            return out;
        });

        // ---- Combat (legit displays only)
        hud("Reach Display", Category.COMBAT, "Distance of your last hit",
                mc -> List.of(Stats.reach <= 0 ? "Reach: -" : String.format("Reach: %.2f", Stats.reach)));
        hud("Combo Counter", Category.COMBAT, "Hits in a row without being hit", mc -> List.of("Combo: " + Stats.combo));
        hud("Damage Indicator", Category.COMBAT, "Damage of your last hit",
                mc -> List.of(String.format("Damage: %.1f", Stats.lastDamage)));
        hud("Sprint Status", Category.COMBAT, "Walking / sprinting / sneaking", mc -> {
            String s = mc.player.getAbilities().flying ? "Flying"
                    : mc.player.isSneaking() ? "Sneaking" : mc.player.isSprinting() ? "Sprinting" : "Walking";
            return List.of(s);
        });
        hud("W-Tap Indicator", Category.COMBAT, "Shows when you W-tap",
                mc -> List.of(System.currentTimeMillis() - Stats.wtap < 800 ? "W-Tap: YES" : "W-Tap: -"));
        hud("S-Tap Indicator", Category.COMBAT, "Shows when you S-tap",
                mc -> List.of(System.currentTimeMillis() - Stats.stap < 800 ? "S-Tap: YES" : "S-Tap: -"));
        hud("Potion Status", Category.COMBAT, "Active effects with time left", mc -> {
            List<String> l = new ArrayList<>();
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                int s = e.getDuration() / 20;
                String t = e.isInfinite() ? "inf" : String.format("%d:%02d", s / 60, s % 60);
                l.add(e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1) + " - " + t);
            }
            return l;
        });
        hud("Pearl Tracker", Category.COMBAT, "Ender pearls in your inventory",
                mc -> List.of("Pearls: " + Stats.count(mc, Items.ENDER_PEARL)));
        hud("Mace Tracker", Category.COMBAT, "Mace in inventory and fall distance", mc -> List.of(
                "Mace: " + (Stats.count(mc, Items.MACE) > 0 ? "yes" : "no"),
                String.format("Fall: %.1f", (double) mc.player.fallDistance)));
        hud("Crystal Counter", Category.COMBAT, "End crystals in your inventory",
                mc -> List.of("Crystals: " + Stats.count(mc, Items.END_CRYSTAL)));
        hud("Totem Counter", Category.COMBAT, "Totems of undying in your inventory",
                mc -> List.of("Totems: " + Stats.count(mc, Items.TOTEM_OF_UNDYING)));
        hud("Item Counter", Category.COMBAT, "Total of the item you hold", mc -> {
            ItemStack s = mc.player.getMainHandStack();
            return List.of(s.isEmpty() ? "Item: -" : s.getName().getString() + ": " + Stats.count(mc, s.getItem()));
        });
        hud("Durability HUD", Category.COMBAT, "Durability of the item you hold", mc -> {
            ItemStack s = mc.player.getMainHandStack();
            return List.of(s.isDamageable() ? "Durability: " + (s.getMaxDamage() - s.getDamage()) + "/" + s.getMaxDamage() : "Durability: -");
        });
        reg(new Widgets.Keystrokes());
        reg(new Widgets.ArmorStatus());
        reg(new Widgets.TargetHud());
        reg(new Widgets.InventoryHud());
        HIT_SOUNDS = reg(new HitSounds());

        // ---- Movement
        reg(new Module("Auto Sprint", Category.MOVEMENT, "Sprint automatically while moving forward") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null) return;
                mc.options.sprintKey.setPressed(mc.options.forwardKey.isPressed() && !mc.player.isSneaking());
            }
        });

        // ---- Performance (option based; real engine optimisation needs Sodium-style mods)
        reg(new Tweak("FPS Boost", "Fast graphics, no clouds, no entity shadows") {
            @Override public void onEnable(MinecraftClient mc) {
                set(mc.options.getGraphicsMode(), GraphicsMode.FAST);
                set(mc.options.getCloudRenderMode(), CloudRenderMode.OFF);
                set(mc.options.getEntityShadows(), false);
            }
        });
        reg(new Tweak("Entity Culling", "Render fewer far entities (Normal / Aggressive)") {
            @Override public void onEnable(MinecraftClient mc) {
                set(mc.options.getEntityDistanceScaling(), opt == 0 ? 0.75 : 0.5);
            }
        }.options(0, "Normal", "Aggressive"));
        reg(new Tweak("Fast Render", "No biome blending, no view bobbing") {
            @Override public void onEnable(MinecraftClient mc) {
                set(mc.options.getBiomeBlendRadius(), 0);
                set(mc.options.getBobView(), false);
            }
        });
        reg(new Tweak("Fast World", "Lower simulation distance to 5") {
            @Override public void onEnable(MinecraftClient mc) { set(mc.options.getSimulationDistance(), 5); }
        });
        reg(new Tweak("Particle Optimizer", "Minimal particles") {
            @Override public void onEnable(MinecraftClient mc) { set(mc.options.getParticles(), ParticlesMode.MINIMAL); }
        });
        reg(new Tweak("Dynamic FPS", "Lower FPS cap while the game window is unfocused") {
            private final Setting.Slider bgFps = add(new Setting.Slider("Background FPS", 10, 60, 5, 30, ""));
            private boolean active;
            @Override public void onTick(MinecraftClient mc) {
                boolean bg = !mc.isWindowFocused();
                if (bg && !active) { set(mc.options.getMaxFps(), (int) bgFps.get()); active = true; }
                else if (!bg && active) { onDisable(mc); active = false; }
            }
            @Override public void onDisable(MinecraftClient mc) { super.onDisable(mc); active = false; }
        });
        reg(new Tweak("Smart Render", "Adjusts view distance automatically from your FPS") {
            private final Setting.Slider minVd = add(new Setting.Slider("Min Distance", 4, 12, 1, 6, ""));
            private final Setting.Slider lowFps = add(new Setting.Slider("Low FPS", 20, 60, 5, 35, ""));
            private final Setting.Slider highFps = add(new Setting.Slider("High FPS", 60, 200, 10, 120, ""));
            private int t;
            @Override public void onEnable(MinecraftClient mc) { set(mc.options.getViewDistance(), mc.options.getViewDistance().getValue()); }
            @Override public void onTick(MinecraftClient mc) {
                if (++t < 100) return;
                t = 0;
                SimpleOption<Integer> vd = mc.options.getViewDistance();
                int cur = vd.getValue(), orig = original(vd), fps = mc.getCurrentFps();
                if (fps < lowFps.get() && cur > minVd.get()) vd.setValue(cur - 1);
                else if (fps > highFps.get() && cur < orig) vd.setValue(cur + 1);
            }
        });
        reg(new Tweak("Memory Optimizer", "Runs garbage collection when memory is almost full") {
            private final Setting.Slider limit = add(new Setting.Slider("Threshold", 60, 95, 5, 80, "%"));
            private int t;
            @Override public void onTick(MinecraftClient mc) {
                if (++t < 1200) return;
                t = 0;
                Runtime r = Runtime.getRuntime();
                if ((r.totalMemory() - r.freeMemory()) > (limit.get() / 100.0) * r.maxMemory()) System.gc();
            }
        });

        // ---- Render
        reg(new Module("Fullbright", Category.RENDER, "See in the dark (local night vision)") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player != null && !mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 100000, 0, false, false, false));
                }
            }
            @Override public void onDisable(MinecraftClient mc) {
                if (mc.player != null) {
                    StatusEffectInstance e = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
                    if (e != null && e.getDuration() > 30000) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                }
            }
        });
        reg(new Module("Time Changer", Category.RENDER, "Fixed time of day") {
            private final long[] T = {1000L, 6000L, 12500L, 14000L, 18000L};
            @Override public void onTick(MinecraftClient mc) {
                if (mc.world != null) ((ClientWorld.Properties) mc.world.getLevelProperties()).setTimeOfDay(T[opt]);
            }
        }.options(1, "Morning", "Noon", "Sunset", "Night", "Midnight"));
        reg(new Module("No Weather", Category.RENDER, "Hide rain and thunder") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.world != null) { mc.world.setRainGradient(0f); mc.world.setThunderGradient(0f); }
            }
        });
        reg(new Module("HitBox", Category.RENDER, "Show entity hitboxes (same as F3+B)") {
            @Override public void onEnable(MinecraftClient mc) { mc.getEntityRenderDispatcher().setRenderHitboxes(true); }
            @Override public void onDisable(MinecraftClient mc) { mc.getEntityRenderDispatcher().setRenderHitboxes(false); }
        });
        reg(new ZoomModule());

        // ---- Misc
        RECORDER = reg(new RecorderModule());
        THEME = reg(new Module("Theme Manager", Category.MISC, "Menu and HUD accent color").options(1, Theme.NAMES).pin());
        FRIENDS = reg(new Module("Friend System", Category.MISC, "/friend add|remove|list <name>, shown in Target HUD").on());
    }
}
