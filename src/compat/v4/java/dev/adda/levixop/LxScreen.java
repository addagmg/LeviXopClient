package dev.adda.levixop;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Input glue for Minecraft up to 1.21.8. */
public abstract class LxScreen extends Screen {
    protected LxScreen(Text t) { super(t); }

    protected boolean onClick(double x, double y, int b) { return false; }
    protected boolean onDrag(double x, double y, int b, double dx, double dy) { return false; }
    protected boolean onRelease(double x, double y, int b) { return false; }
    protected boolean onKey(int key, int scan, int mods) { return false; }
    protected boolean onChar(char c) { return false; }

    @Override public boolean mouseClicked(double x, double y, int b) { return onClick(x, y, b) || super.mouseClicked(x, y, b); }
    @Override public boolean mouseDragged(double x, double y, int b, double dx, double dy) { return onDrag(x, y, b, dx, dy) || super.mouseDragged(x, y, b, dx, dy); }
    @Override public boolean mouseReleased(double x, double y, int b) { return onRelease(x, y, b) || super.mouseReleased(x, y, b); }
    @Override public boolean keyPressed(int k, int s, int m) { return onKey(k, s, m) || super.keyPressed(k, s, m); }
    @Override public boolean charTyped(char c, int m) { return onChar(c) || super.charTyped(c, m); }
}
