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
    public static KeyBinding menuKey, zoomKey;
    private boolean started;

    @Override
    public void onInitializeClient() {
        Config.load();
        Social.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.levixopclient.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.levixopclient"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.levixopclient.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.levixopclient"));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register(Hud::render);
        Commands.register();

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient()) {
                Stats.onAttack(player, entity);
                if (Modules.HIT_SOUNDS.enabled) {
                    SoundEvent s = switch (Modules.HIT_SOUNDS.opt) {
                        case 1 -> SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
                        case 2 -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
                        default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
                    };
                    player.playSound(s, 0.8f, 1.2f);
                }
            }
            return ActionResult.PASS;
        });

        // restore any video options we changed before the game saves them
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
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
        Stats.tick(mc);
        for (Module m : Modules.ALL) if (m.enabled) m.onTick(mc);
    }
}
