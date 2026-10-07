package dev.adda.levixop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** Gameplay recorder: saves MJPEG .avi (no audio) into .minecraft/recordings. */
public final class Recorder {
    private static volatile boolean running;
    private static boolean paused;
    private static BlockingQueue<byte[]> queue;
    private static Thread worker;
    private static int outW, outH, stride, fps;
    private static long lastCap;
    private static File file;

    public static boolean active() { return running; }
    public static boolean paused() { return paused; }

    public static void msg(MinecraftClient mc, String t) {
        mc.execute(() -> { if (mc.player != null) mc.player.sendMessage(Text.literal("[LeviXop] " + t), true); });
    }

    public static void start(MinecraftClient mc, int strideIn, int fpsIn) {
        if (running) return;
        Framebuffer fb = mc.getFramebuffer();
        stride = Math.max(1, strideIn);
        fps = Math.max(5, fpsIn);
        outW = (fb.textureWidth / stride) & ~1;
        outH = (fb.textureHeight / stride) & ~1;
        File dir = new File(mc.runDirectory, "recordings");
        dir.mkdirs();
        file = new File(dir, "LeviXop-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".avi");
        final AviWriter avi;
        try { avi = new AviWriter(file.toPath(), outW, outH, fps); }
        catch (Exception e) { msg(mc, "Recorder failed: " + e.getMessage()); return; }

        queue = new ArrayBlockingQueue<>(8);
        paused = false;
        running = true;
        lastCap = 0;
        worker = new Thread(() -> {
            try {
                while (running || !queue.isEmpty()) {
                    byte[] rgb = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (rgb == null) continue;
                    avi.add(encode(rgb));
                }
                avi.close();
            } catch (Exception e) {
                try { avi.close(); } catch (Exception ignored) {}
            }
            msg(mc, "Saved: recordings/" + file.getName());
        }, "LeviXop-Recorder");
        worker.start();
        msg(mc, "Recording started (F7 pause, F8 stop)");
    }

    private static byte[] encode(byte[] rgb) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(rgb.length / 8);
        ByteBuffer buf = MemoryUtil.memAlloc(rgb.length);
        buf.put(rgb).flip();
        STBIWriteCallback cb = STBIWriteCallback.create((ctx, data, size) -> {
            byte[] b = new byte[size];
            MemoryUtil.memByteBuffer(data, size).get(b);
            out.write(b, 0, size);
        });
        try {
            STBImageWrite.stbi_write_jpg_to_func(cb, 0L, outW, outH, 3, buf, 80);
        } finally {
            cb.free();
            MemoryUtil.memFree(buf);
        }
        return out.toByteArray();
    }

    public static void togglePause(MinecraftClient mc) {
        if (!running) return;
        paused = !paused;
        msg(mc, paused ? "Recording paused" : "Recording resumed");
    }

    public static void stop(MinecraftClient mc) {
        if (!running) return;
        running = false;
        msg(mc, "Stopping recording...");
    }

    /** Called once per rendered frame, after the HUD is drawn. */
    public static void capture(MinecraftClient mc) {
        if (!running || paused) return;
        long now = System.currentTimeMillis();
        if (now - lastCap < 1000L / fps) return;
        if (queue.remainingCapacity() == 0) return;
        lastCap = now;
        NativeImage img = ScreenshotRecorder.takeScreenshot(mc.getFramebuffer());
        try {
            int w = (img.getWidth() / stride) & ~1, h = (img.getHeight() / stride) & ~1;
            if (w != outW || h != outH) return;
            byte[] rgb = new byte[w * h * 3];
            int i = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = img.getColorArgb(x * stride, y * stride);
                    rgb[i++] = (byte) (c >> 16);
                    rgb[i++] = (byte) (c >> 8);
                    rgb[i++] = (byte) c;
                }
            }
            queue.offer(rgb);
        } finally {
            img.close();
        }
    }
}
