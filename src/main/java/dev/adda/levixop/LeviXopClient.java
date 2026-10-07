package dev.adda.levixop;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class LeviXopClient implements ClientModInitializer {
    public static KeyBinding menuKey, zoomKey, recPauseKey, recStopKey;
    private boolean started;

    private static KeyBinding bind(String id, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(id, InputUtil.Type.KEYSYM, key, "category.levixopclient"));
    }

    @Override
    public void onInitializeClient() {
        Config.load();
        Social.load();

        menuKey = bind("key.levixopclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        zoomKey = bind("key.levixopclient.zoom", GLFW.GLFW_KEY_C);
        recPauseKey = bind("key.levixopclient.rec_pause", GLFW.GLFW_KEY_F7);
        recStopKey = bind("key.levixopclient.rec_stop", GLFW.GLFW_KEY_F8);

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register(Hud::render);
        Commands.register();

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient()) {
                Stats.onAttack(player, entity);
                HitSounds hs = Modules.HIT_SOUNDS;
                if (hs.enabled) {
                    SoundEvent s = switch (hs.sound.get()) {
                        case 1 -> SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
                        case 2 -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
                        default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
                    };
                    player.playSound(s, (float) hs.volume.get(), 1.2f);
                }
            }
            return ActionResult.PASS;
        });

        // restore any video options we changed before the game saves them
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            Recorder.stop(mc);
            for (Module m : Modules.ALL) if (m.enabled) m.onDisable(mc);
            Config.save();
        });
    }

    private void tick(MinecraftClient mc) {
        if (!started) {
            started = true;
            for (Module m : Modules.ALL) if (m.enabled && !m.pinned) m.onEnable(mc);
        }
        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new ClickGui());
        }
        while (recPauseKey.wasPressed()) Recorder.togglePause(mc);
        while (recStopKey.wasPressed()) {
            Recorder.stop(mc);
            Modules.RECORDER.enabled = false;
        }

        // per-module keybinds
        long h = mc.getWindow().getHandle();
        for (Module m : Modules.ALL) {
            int c = m.key.code;
            if (c > 0 && !m.pinned) {
                boolean d = InputUtil.isKeyPressed(h, c);
                if (d && !m.keyDown && mc.currentScreen == null) m.setEnabled(!m.enabled, mc);
                m.keyDown = d;
            }
        }

        Stats.tick(mc);
        for (Module m : Modules.ALL) if (m.enabled) m.onTick(mc);
    }
}
