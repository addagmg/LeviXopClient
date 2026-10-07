package dev.adda.levixop;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;

/** Minimal MJPEG AVI writer (video only). */
final class AviWriter {
    private final RandomAccessFile f;
    private final ArrayList<int[]> idx = new ArrayList<>();
    private long pTotal, pLen, pMoviSize, moviPos;
    private int frames;

    AviWriter(Path p, int w, int h, int fps) throws IOException {
        f = new RandomAccessFile(p.toFile(), "rw");
        f.setLength(0);
        fcc("RIFF"); i32(0); fcc("AVI ");
        fcc("LIST"); i32(192); fcc("hdrl");
        fcc("avih"); i32(56);
        i32(1_000_000 / fps); i32(0); i32(0); i32(0x10);
        pTotal = f.getFilePointer(); i32(0);
        i32(0); i32(1); i32(w * h * 3); i32(w); i32(h);
        i32(0); i32(0); i32(0); i32(0);
        fcc("LIST"); i32(116); fcc("strl");
        fcc("strh"); i32(56); fcc("vids"); fcc("MJPG");
        i32(0); i32(0); i32(0); i32(1); i32(fps); i32(0);
        pLen = f.getFilePointer(); i32(0);
        i32(w * h * 3); i32(-1); i32(0);
        i16(0); i16(0); i16(w); i16(h);
        fcc("strf"); i32(40); i32(40); i32(w); i32(h); i16(1); i16(24); fcc("MJPG");
        i32(w * h * 3); i32(0); i32(0); i32(0); i32(0);
        fcc("LIST"); pMoviSize = f.getFilePointer(); i32(0);
        moviPos = f.getFilePointer(); fcc("movi");
    }

    synchronized void add(byte[] jpg) throws IOException {
        long pos = f.getFilePointer();
        fcc("00dc"); i32(jpg.length);
        f.write(jpg);
        if ((jpg.length & 1) == 1) f.write(0);
        idx.add(new int[]{(int) (pos - moviPos), jpg.length});
        frames++;
    }

    synchronized void close() throws IOException {
        long end = f.getFilePointer();
        fcc("idx1"); i32(idx.size() * 16);
        for (int[] e : idx) { fcc("00dc"); i32(0x10); i32(e[0]); i32(e[1]); }
        long fileEnd = f.getFilePointer();
        f.seek(4); i32((int) (fileEnd - 8));
        f.seek(pMoviSize); i32((int) (end - moviPos));
        f.seek(pTotal); i32(frames);
        f.seek(pLen); i32(frames);
        f.close();
    }

    private void fcc(String s) throws IOException { f.writeBytes(s); }
    private void i32(int v) throws IOException {
        f.write(new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24)});
    }
    private void i16(int v) throws IOException { f.write(new byte[]{(byte) v, (byte) (v >> 8)}); }
}
