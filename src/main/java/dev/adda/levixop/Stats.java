package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

/** Shared runtime state used by the HUD modules. */
public final class Stats {
    public static final long START = System.currentTimeMillis();
    public static final ArrayDeque<Long> L = new ArrayDeque<>(), R = new ArrayDeque<>();
    private static boolean pl, pr, pf, pb, wasDead;
    private static int tickN, fRel, bPress, prevHurt;

    public static int combo, deaths;
    public static double reach;
    public static float lastDamage;
    public static LivingEntity target;
    public static long targetTime, lastHit, wtap, stap;
    private static float targetHp;

    public static int cps(ArrayDeque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    public static void pollMouse(MinecraftClient mc) {
        long w = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (mc.currentScreen == null) {
            if (l && !pl) L.add(now);
            if (r && !pr) R.add(now);
        }
        pl = l; pr = r;
    }

    public static void onAttack(PlayerEntity p, Entity e) {
        Vec3d eye = p.getEyePos();
        Box b = e.getBoundingBox();
        double cx = MathHelper.clamp(eye.x, b.minX, b.maxX);
        double cy = MathHelper.clamp(eye.y, b.minY, b.maxY);
        double cz = MathHelper.clamp(eye.z, b.minZ, b.maxZ);
        reach = eye.distanceTo(new Vec3d(cx, cy, cz));
        if (e instanceof LivingEntity le) {
            target = le;
            targetHp = le.getHealth();
            targetTime = System.currentTimeMillis();
            lastHit = targetTime;
            combo++;
        }
    }

    public static void tick(MinecraftClient mc) {
        tickN++;
        long now = System.currentTimeMillis();

        boolean dead = mc.currentScreen instanceof DeathScreen;
        if (dead && !wasDead) deaths++;
        wasDead = dead;

        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        if (p.hurtTime > 0 && prevHurt == 0) combo = 0;
        prevHurt = p.hurtTime;
        if (combo > 0 && now - lastHit > 4000) combo = 0;

        if (target != null) {
            if (target.isRemoved() || now - targetTime > 8000) {
                target = null;
            } else {
                float hp = target.getHealth();
                if (hp < targetHp) lastDamage = targetHp - hp;
                targetHp = hp;
            }
        }

        boolean f = mc.options.forwardKey.isPressed();
        boolean b = mc.options.backKey.isPressed();
        if (pf && !f) fRel = tickN;
        if (!pf && f && tickN - fRel <= 6) wtap = now;
        pf = f;
        if (!pb && b) bPress = tickN;
        if (pb && !b && tickN - bPress <= 6) stap = now;
        pb = b;
    }

    public static int count(MinecraftClient mc, Item item) {
        int n = 0;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) n += s.getCount();
        }
        return n;
    }
}
