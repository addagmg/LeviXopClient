package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/** HUD elements with custom drawing. */
public final class Widgets {

    public static class Keystrokes extends HudModule {
        public Keystrokes() {
            super("Keystrokes", Category.COMBAT, "WASD, mouse buttons and space with CPS");
            x = 4; y = 120;
        }

        @Override
        public void render(DrawContext ctx, MinecraftClient mc, boolean editor) {
            w = 77; h = 90;
            clamp(ctx);
            GameOptions o = mc.options;
            key(ctx, mc, "W", x + 26, y, 24, 24, o.forwardKey.isPressed());
            key(ctx, mc, "A", x, y + 26, 24, 24, o.leftKey.isPressed());
            key(ctx, mc, "S", x + 26, y + 26, 24, 24, o.backKey.isPressed());
            key(ctx, mc, "D", x + 52, y + 26, 24, 24, o.rightKey.isPressed());
            key(ctx, mc, "L " + Stats.cps(Stats.L), x, y + 52, 38, 24, o.attackKey.isPressed());
            key(ctx, mc, "R " + Stats.cps(Stats.R), x + 39, y + 52, 38, 24, o.useKey.isPressed());
            key(ctx, mc, "SPACE", x, y + 78, 77, 12, o.jumpKey.isPressed());
        }

        private void key(DrawContext ctx, MinecraftClient mc, String label, int kx, int ky, int kw, int kh, boolean down) {
            if (down) ctx.fill(kx, ky, kx + kw, ky + kh, (Theme.accent() & 0x00FFFFFF) | 0xCC000000);
            else if (!noBg()) ctx.fill(kx, ky, kx + kw, ky + kh, 0x90000000);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, label, kx + kw / 2, ky + (kh - 8) / 2, 0xFFFFFFFF);
        }
    }

    public static class ArmorStatus extends HudModule {
        private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        public ArmorStatus() {
            super("Armor Status", Category.COMBAT, "Armor pieces with remaining durability");
            x = 4; y = 220;
        }

        @Override
        public void render(DrawContext ctx, MinecraftClient mc, boolean editor) {
            boolean any = false;
            for (EquipmentSlot s : SLOTS) if (!mc.player.getEquippedStack(s).isEmpty()) any = true;
            if (!any && !editor) { w = 0; h = 0; return; }
            w = 66; h = 4 * 18 + 2;
            clamp(ctx);
            if (!noBg()) {
                ctx.fill(x, y, x + w, y + h, 0x70000000);
                ctx.fill(x, y, x + 2, y + h, Theme.accent());
            }
            int yy = y + 2;
            for (EquipmentSlot s : SLOTS) {
                ItemStack st = mc.player.getEquippedStack(s);
                if (!st.isEmpty()) {
                    ctx.drawItem(st, x + 5, yy);
                    if (st.isDamageable()) {
                        ctx.drawTextWithShadow(mc.textRenderer, String.valueOf(st.getMaxDamage() - st.getDamage()), x + 25, yy + 5, 0xFFFFFFFF);
                    }
                }
                yy += 18;
            }
        }
    }

    public static class TargetHud extends HudModule {
        public TargetHud() {
            super("Target HUD", Category.COMBAT, "Name and health of your last target");
            x = 130; y = 4;
        }

        @Override
        public void render(DrawContext ctx, MinecraftClient mc, boolean editor) {
            LivingEntity t = Stats.target;
            boolean show = t != null && !t.isRemoved();
            if (!show && !editor) { w = 0; h = 0; return; }
            w = 112; h = 26;
            clamp(ctx);
            if (!noBg()) {
                ctx.fill(x, y, x + w, y + h, 0x90000000);
                ctx.fill(x, y, x + 2, y + h, Theme.accent());
            }
            String name = show ? t.getName().getString() : "Target";
            if (show && Modules.FRIENDS.enabled && Social.isFriend(name)) name += " [Friend]";
            ctx.drawTextWithShadow(mc.textRenderer, name, x + 6, y + 3, 0xFFFFFFFF);
            float hp = show ? t.getHealth() : 20f;
            float max = show ? Math.max(1f, t.getMaxHealth()) : 20f;
            float ratio = MathHelper.clamp(hp / max, 0f, 1f);
            int bw = w - 12;
            ctx.fill(x + 6, y + 15, x + 6 + bw, y + 21, 0xFF333340);
            int col = ratio > 0.5f ? 0xFF3DDC84 : (ratio > 0.25f ? 0xFFF59E0B : 0xFFEF4444);
            ctx.fill(x + 6, y + 15, x + 6 + (int) (bw * ratio), y + 21, col);
        }
    }

    public static class InventoryHud extends HudModule {
        public InventoryHud() {
            super("Inventory HUD", Category.COMBAT, "Shows your main inventory on screen");
            x = 4; y = 300;
        }

        @Override
        public void render(DrawContext ctx, MinecraftClient mc, boolean editor) {
            w = 9 * 18 + 6; h = 3 * 18 + 6;
            clamp(ctx);
            if (!noBg()) {
                ctx.fill(x, y, x + w, y + h, 0x70000000);
                ctx.fill(x, y, x + w, y + 2, Theme.accent());
            }
            for (int i = 0; i < 27; i++) {
                ItemStack s = mc.player.getInventory().getStack(9 + i);
                if (s.isEmpty()) continue;
                int ix = x + 4 + (i % 9) * 18, iy = y + 4 + (i / 9) * 18;
                ctx.drawItem(s, ix, iy);
                ctx.drawStackOverlay(mc.textRenderer, s, ix, iy);
            }
        }
    }
}
