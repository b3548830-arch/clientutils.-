package com.example.clientutils;

import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Миникарта: вид сверху по цветам карты Minecraft. Берёт только загруженные клиентом чанки. */
public final class Minimap {
    private Minimap() {}

    private static int[] grid = new int[0];
    private static int gStep;
    private static long last;

    /** Сетка n×n цветов (ARGB, 0 = чанк не загружен), центр — (cx, cz), шаг — step блоков на клетку. */
    public static int[] sample(ClientWorld w, int cx, int cz, int cy, int n, int step) {
        int[] out = new int[n * n];
        int[] hs = new int[n * n];
        boolean ceil = w.getDimension().hasCeiling();
        int bottom = w.getBottomY();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int x = cx + (i - n / 2) * step, z = cz + (j - n / 2) * step;
                if (!w.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) continue;
                int y = w.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
                if (ceil) y = Math.min(y, cy + 4);
                int rgb = 0x101010, found = bottom;
                for (int k = 0; k < 64 && y > bottom; k++, y--) {
                    m.set(x, y, z);
                    BlockState s = w.getBlockState(m);
                    MapColor mc = s.getMapColor(w, m);
                    if (mc != MapColor.CLEAR) { rgb = mc.color; found = y; break; }
                }
                int idx = j * n + i;
                hs[idx] = found;
                int north = j > 0 && out[idx - n] != 0 ? hs[idx - n] : found;
                double f = found > north ? 1.0 : found < north ? 0.72 : 0.86;
                int r = (int) (((rgb >> 16) & 255) * f), g = (int) (((rgb >> 8) & 255) * f), b = (int) ((rgb & 255) * f);
                out[idx] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
        return out;
    }

    /** Рисует сетку горизонтальными полосами одного цвета (быстрее, чем по пикселю). */
    public static void draw(DrawContext ctx, int[] g, int n, int x0, int y0) {
        for (int j = 0; j < n; j++) {
            int i = 0;
            while (i < n) {
                int c = g[j * n + i], k = i + 1;
                while (k < n && g[j * n + k] == c) k++;
                if (c != 0) ctx.fill(x0 + i, y0 + j, x0 + k, y0 + j + 1, c);
                i = k;
            }
        }
    }

    public static List<Config.Waypoint> points(Config c, String dim) {
        List<Config.Waypoint> all = new ArrayList<>();
        for (Config.Waypoint w : c.waypoints) if (dim.equals(w.dim)) all.add(w);
        if (c.death != null && dim.equals(c.death.dim)) all.add(c.death);
        return all;
    }

    public static void marker(DrawContext ctx, ClientPlayerEntity p, int cx, int cy) {
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        double a = Math.toRadians(p.getYaw());
        int dx = (int) Math.round(-Math.sin(a) * 4), dz = (int) Math.round(Math.cos(a) * 4);
        ctx.fill(cx + dx, cy + dz, cx + dx + 2, cy + dz + 2, 0xFFFFFF55);
    }

    public static void render(DrawContext ctx, TextRenderer tr, MinecraftClient mc, Config c, String dim, int sw, int sh) {
        ClientPlayerEntity p = mc.player;
        int size = c.mapSize, step = c.mapZoom;
        long now = System.currentTimeMillis();
        if (grid.length != size * size || gStep != step || now - last > 250) {
            grid = sample(mc.world, p.getBlockX(), p.getBlockZ(), p.getBlockY(), size, step);
            gStep = step;
            last = now;
        }
        int x0 = c.mapPos == 2 ? 6 : sw - size - 6;
        int y0 = c.mapPos == 1 ? sh - size - 24 : sh / 2 - size / 2;
        ctx.fill(x0 - 2, y0 - 2, x0 + size + 2, y0 + size + 2, 0xFF000000);
        ctx.fill(x0, y0, x0 + size, y0 + size, 0xFF1A1A1A);
        draw(ctx, grid, size, x0, y0);

        for (Config.Waypoint w : points(c, dim)) {
            int px = x0 + size / 2 + (int) Math.floor((w.x - p.getBlockX()) / (double) step);
            int py = y0 + size / 2 + (int) Math.floor((w.z - p.getBlockZ()) / (double) step);
            px = Math.max(x0, Math.min(x0 + size - 3, px));
            py = Math.max(y0, Math.min(y0 + size - 3, py));
            ctx.fill(px, py, px + 3, py + 3, w == c.death ? 0xFFFF5555 : 0xFF55FFFF);
        }
        marker(ctx, p, x0 + size / 2, y0 + size / 2);
        ctx.drawTextWithShadow(tr, "С", x0 + size / 2 - 2, y0 + 1, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, "1:" + step, x0 + 2, y0 + size - 9, 0xFFDDDDDD);
    }
}
