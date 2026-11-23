package com.example.clientutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Клиентские команды /cu. Не отправляются на сервер. */
public final class Cmds {
    private Cmds() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(ClientCommandManager.literal("cu")
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> add(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(ClientCommandManager.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(ClientCommandManager.literal("del")
                        .then(ClientCommandManager.argument("n", IntegerArgumentType.integer(1, Config.MAX_WP))
                                .executes(ctx -> del(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "n")))))
                .then(ClientCommandManager.literal("rename")
                        .then(ClientCommandManager.argument("n", IntegerArgumentType.integer(1, Config.MAX_WP))
                                .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> rename(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "n"),
                                                StringArgumentType.getString(ctx, "name")))))));
    }

    private static String cut(String s) {
        s = s.trim();
        return s.length() > 24 ? s.substring(0, 24) : s;
    }

    private static int add(FabricClientCommandSource s, String name) {
        Config c = Config.get();
        if (c.waypoints.size() >= Config.MAX_WP) {
            s.sendError(Text.literal("Максимум " + Config.MAX_WP + " точек"));
            return 0;
        }
        BlockPos p = s.getPlayer().getBlockPos();
        c.waypoints.add(new Config.Waypoint(cut(name), ClientUtilsMod.dim(s.getClient()), p.getX(), p.getY(), p.getZ()));
        c.save();
        s.sendFeedback(Text.literal("Точка добавлена: " + cut(name) + " (" + p.getX() + " " + p.getY() + " " + p.getZ() + ")"));
        return 1;
    }

    private static int list(FabricClientCommandSource s) {
        Config c = Config.get();
        if (c.waypoints.isEmpty()) {
            s.sendFeedback(Text.literal("Точек нет. /cu add <имя>"));
            return 1;
        }
        for (int i = 0; i < c.waypoints.size(); i++) {
            Config.Waypoint w = c.waypoints.get(i);
            s.sendFeedback(Text.literal((i + 1) + ". " + w.name + "  " + w.x + " " + w.y + " " + w.z + "  [" + w.dim + "]"));
        }
        return 1;
    }

    private static int del(FabricClientCommandSource s, int n) {
        Config c = Config.get();
        if (n > c.waypoints.size()) { s.sendError(Text.literal("Нет точки №" + n)); return 0; }
        Config.Waypoint w = c.waypoints.remove(n - 1);
        c.save();
        s.sendFeedback(Text.literal("Удалена: " + w.name));
        return 1;
    }

    private static int rename(FabricClientCommandSource s, int n, String name) {
        Config c = Config.get();
        if (n > c.waypoints.size()) { s.sendError(Text.literal("Нет точки №" + n)); return 0; }
        c.waypoints.get(n - 1).name = cut(name);
        c.save();
        s.sendFeedback(Text.literal("Переименована в: " + cut(name)));
        return 1;
    }
}
