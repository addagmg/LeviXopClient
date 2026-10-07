package dev.adda.levixop;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class Commands {
    private static void msg(FabricClientCommandSource s, String t) {
        s.sendFeedback(Text.literal("[LeviXop] " + t));
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((d, access) -> {
            d.register(literal("friend")
                    .then(literal("add").then(argument("name", StringArgumentType.word()).executes(c -> {
                        String n = StringArgumentType.getString(c, "name");
                        Social.FRIENDS.add(n.toLowerCase());
                        Social.save();
                        msg(c.getSource(), "Added friend " + n);
                        return 1;
                    })))
                    .then(literal("remove").then(argument("name", StringArgumentType.word()).executes(c -> {
                        String n = StringArgumentType.getString(c, "name");
                        Social.FRIENDS.remove(n.toLowerCase());
                        Social.save();
                        msg(c.getSource(), "Removed friend " + n);
                        return 1;
                    })))
                    .then(literal("list").executes(c -> {
                        msg(c.getSource(), "Friends: " + (Social.FRIENDS.isEmpty() ? "none" : String.join(", ", Social.FRIENDS)));
                        return 1;
                    })));

            d.register(literal("wp")
                    .then(literal("add").then(argument("name", StringArgumentType.word()).executes(c -> {
                        String n = StringArgumentType.getString(c, "name");
                        var p = c.getSource().getPlayer();
                        String dim = c.getSource().getWorld().getRegistryKey().getValue().toString();
                        Social.WAYPOINTS.removeIf(w -> w.name().equalsIgnoreCase(n) && w.dim().equals(dim));
                        Social.WAYPOINTS.add(new Social.Waypoint(n, dim, p.getX(), p.getY(), p.getZ()));
                        Social.save();
                        msg(c.getSource(), "Waypoint " + n + " saved");
                        return 1;
                    })))
                    .then(literal("del").then(argument("name", StringArgumentType.word()).executes(c -> {
                        String n = StringArgumentType.getString(c, "name");
                        Social.WAYPOINTS.removeIf(w -> w.name().equalsIgnoreCase(n));
                        Social.save();
                        msg(c.getSource(), "Waypoint " + n + " removed");
                        return 1;
                    })))
                    .then(literal("list").executes(c -> {
                        for (Social.Waypoint w : Social.WAYPOINTS) {
                            msg(c.getSource(), String.format("%s: %.0f %.0f %.0f (%s)", w.name(), w.x(), w.y(), w.z(), w.dim()));
                        }
                        if (Social.WAYPOINTS.isEmpty()) msg(c.getSource(), "No waypoints");
                        return 1;
                    })));

            d.register(literal("lx").executes(c -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.send(() -> mc.setScreen(new ClickGui()));
                return 1;
            }));
        });
    }
}
