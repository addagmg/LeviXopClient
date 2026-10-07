package dev.adda.levixop;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class Social {
    public record Waypoint(String name, String dim, double x, double y, double z) {}

    public static final Set<String> FRIENDS = new TreeSet<>();
    public static final List<Waypoint> WAYPOINTS = new ArrayList<>();

    private static Path friendsFile() { return FabricLoader.getInstance().getConfigDir().resolve("levixopclient-friends.txt"); }
    private static Path wpFile() { return FabricLoader.getInstance().getConfigDir().resolve("levixopclient-waypoints.txt"); }

    public static boolean isFriend(String name) { return FRIENDS.contains(name.toLowerCase()); }

    public static void load() {
        try {
            if (Files.exists(friendsFile())) FRIENDS.addAll(Files.readAllLines(friendsFile()));
            if (Files.exists(wpFile())) {
                for (String line : Files.readAllLines(wpFile())) {
                    String[] a = line.split("\t");
                    if (a.length == 5) WAYPOINTS.add(new Waypoint(a[0], a[1],
                            Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4])));
                }
            }
        } catch (IOException | NumberFormatException ignored) {}
    }

    public static void save() {
        try {
            Files.write(friendsFile(), new ArrayList<>(FRIENDS));
            List<String> out = new ArrayList<>();
            for (Waypoint w : WAYPOINTS) out.add(w.name() + "\t" + w.dim() + "\t" + w.x() + "\t" + w.y() + "\t" + w.z());
            Files.write(wpFile(), out);
        } catch (IOException ignored) {}
    }
}
