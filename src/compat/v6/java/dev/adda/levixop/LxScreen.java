package dev.adda.levixop;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/** Input glue for Minecraft 1.21.9+ (Click / KeyInput / CharInput records). */
public abstract class LxScreen extends Screen {
    protected LxScreen(Text t) { super(t); }

    protected boolean onClick(double x, double y, int b) { return false; }
    protected boolean onDrag(double x, double y, int b, double dx, double dy) { return false; }
    protected boolean onRelease(double x, double y, int b) { return false; }
    protected boolean onKey(int key, int scan, int mods) { return false; }
    protected boolean onChar(char c) { return false; }

    @Override public boolean mouseClicked(Click c, boolean doubled) { return onClick(c.x(), c.y(), c.button()) || super.mouseClicked(c, doubled); }
    @Override public boolean mouseDragged(Click c, double dx, double dy) { return onDrag(c.x(), c.y(), c.button(), dx, dy) || super.mouseDragged(c, dx, dy); }
    @Override public boolean mouseReleased(Click c) { return onRelease(c.x(), c.y(), c.button()) || super.mouseReleased(c); }
    @Override public boolean keyPressed(KeyInput k) { return onKey(k.key(), k.scancode(), k.modifiers()) || super.keyPressed(k); }
    @Override public boolean charTyped(CharInput c) { return onChar((char) c.codepoint()) || super.charTyped(c); }
}
