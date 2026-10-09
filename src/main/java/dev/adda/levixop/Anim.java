package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

final class Anim {
    static float clamp01(float t) { return Math.max(0f, Math.min(1f, t)); }
    static float ease(float t) { float u = 1f - clamp01(t); return 1f - u * u * u; }
    static float approach(float cur, float target, float dt) { return cur + (target - cur) * Math.min(1f, dt * 16f); }

    static int lerpColor(int a, int b, float t) {
        t = clamp01(t);
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}

final class Sfx {
    static void play(SoundEvent e, float pitch) {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(e, pitch));
    }
    static void play(RegistryEntry<SoundEvent> e, float pitch) { play(e.value(), pitch); }
}
