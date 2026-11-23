package com.example.clientutils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class Hud {
    private Hud() {}

    record Line(String t, int c) {}

    static final int W = 0xFFFFFFFF, G = 0xFF55FF55, R = 0xFFFF5555, Y = 0xFFFFFF55, GR = 0xFFAAAAAA, C = 0xFF55FFFF;
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private static final ArrayDeque<Long> lc = new ArrayDeque<>(), rc = new ArrayDeque<>();
    private static boolean lPrev, rPrev;
    static int BG = 0x66000000;
    private static long cacheSlice = -1;
    private static Item cacheHand;
    private static int cTotems, cArrows, cFree, cHand;

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Config c = Config.get();
        if (mc.player == null || mc.world == null || mc.options.hudHidden || !c.on("hud")) return;

        BG = Math.round(c.hudAlpha * 2.55f) << 24;
        pollCps(mc);
        refresh(mc.player);
        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        ClientPlayerEntity p = mc.player;
        BlockPos pos = p.getBlockPos();
        String dim = ClientUtilsMod.dim(mc);

        // ───────── ЛЕВЫЙ БЛОК ─────────
        List<Line> L = new ArrayList<>();
        if (c.on("fps")) {
            int f = mc.getCurrentFps();
            L.add(new Line("FPS: " + f, f >= 60 ? G : f >= 30 ? Y : R));
        }
        if (c.on("ping") && !mc.isInSingleplayer() && mc.getNetworkHandler() != null) {
            var e = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
            if (e != null) {
                int ms = e.getLatency();
                L.add(new Line("Пинг: " + ms + " мс", ms < 80 ? G : ms < 200 ? Y : R));
            }
        }
        if (c.on("coords"))
            L.add(new Line("XYZ: " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                    + "  (" + dirRu(p.getHorizontalFacing()) + ")", W));
        if (c.on("chunk"))
            L.add(new Line("Чанк: " + (pos.getX() >> 4) + " " + (pos.getZ() >> 4)
                    + "  (в чанке " + (pos.getX() & 15) + " " + (pos.getZ() & 15) + ")", GR));
        if (c.on("nether")) {
            if (dim.equals("minecraft:overworld"))
                L.add(new Line("Нижний мир: " + Math.floorDiv(pos.getX(), 8) + " " + Math.floorDiv(pos.getZ(), 8), GR));
            else if (dim.equals("minecraft:the_nether"))
                L.add(new Line("Обычный мир: " + (pos.getX() * 8) + " " + (pos.getZ() * 8), GR));
        }
        if (c.on("biome")) {
            String b = mc.world.getBiome(pos).getKey().map(k -> k.getValue().getPath().replace('_', ' ')).orElse("?");
            L.add(new Line("Биом: " + b, W));
        }
        if (c.on("clock")) {
            long t = mc.world.getTimeOfDay();
            long dt = t % 24000;
            int hh = (int) ((dt / 1000 + 6) % 24), mm = (int) ((dt % 1000) * 60 / 1000);
            L.add(new Line(String.format(Locale.ROOT, "Время: %s | Игра: %02d:%02d | День %d",
                    LocalTime.now().format(HM), hh, mm, t / 24000 + 1), W));
        }
        if (c.on("speed"))
            L.add(new Line(String.format(Locale.ROOT, "Скорость: %.1f бл/с", ClientUtilsMod.speed), W));
        if (c.on("light")) {
            int bl = mc.world.getLightLevel(LightType.BLOCK, pos);
            int sl = mc.world.getLightLevel(LightType.SKY, pos);
            L.add(new Line("Свет: блок " + bl + " | небо " + sl + (bl == 0 ? " — возможен спавн" : ""), bl == 0 ? R : G));
        }
        if (c.on("session")) {
            long s = (System.currentTimeMillis() - ClientUtilsMod.sessionStart) / 1000;
            L.add(new Line(String.format(Locale.ROOT, "Сессия: %d:%02d:%02d", s / 3600, s / 60 % 60, s % 60), GR));
        }
        if (c.on("memory")) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1048576, max = rt.maxMemory() / 1048576;
            L.add(new Line("Память: " + used + "/" + max + " МБ", used * 10 > max * 9 ? R : GR));
        }
        if (c.on("slots"))
            L.add(new Line("Свободно слотов: " + cFree, cFree <= 3 ? R : W));
        if (c.on("cps"))
            L.add(new Line("CPS: " + lc.size() + " | " + rc.size(), W));
        if (c.on("counts")) {
            L.add(new Line("Тотемы: " + cTotems, cTotems == 0 ? R : C));
            if (cArrows > 0) L.add(new Line("Стрелы: " + cArrows, W));
            ItemStack hand = p.getMainHandStack();
            if (!hand.isEmpty() && hand.getMaxCount() > 1)
                L.add(new Line(hand.getName().getString() + ": " + cHand, W));
        }
        if (c.on("armor")) {
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack st = p.getEquippedStack(s);
                if (st.isEmpty() || !st.isDamageable()) continue;
                int max = st.getMaxDamage(), left = max - st.getDamage();
                L.add(new Line(st.getName().getString() + ": " + left + "/" + max, pctColor(left, max)));
            }
            ItemStack hand = p.getMainHandStack();
            if (!hand.isEmpty() && hand.isDamageable()) {
                int max = hand.getMaxDamage(), left = max - hand.getDamage();
                L.add(new Line("В руке: " + left + "/" + max, pctColor(left, max)));
            }
        }
        drawBlock(ctx, tr, L, 4 + c.hudX, 4 + c.hudY, false, sw);

        // ───────── ПРАВЫЙ БЛОК ─────────
        int ry = 4 + c.hudY;
        boolean anyEff = false, anyBad = false;
        for (StatusEffectInstance e : p.getStatusEffects()) {
            anyEff = true;
            if (!e.getEffectType().value().isBeneficial()) anyBad = true;
        }
        if (anyEff) ry += 28 + (anyBad ? 26 : 0);   // под ванильными иконками эффектов
        if (c.on("effects")) {
            List<Line> E = new ArrayList<>();
            for (StatusEffectInstance e : p.getStatusEffects()) {
                String n = e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1);
                String t = e.isInfinite() ? "∞" : String.format(Locale.ROOT, "%d:%02d", e.getDuration() / 1200, e.getDuration() / 20 % 60);
                E.add(new Line(n + "  " + t, !e.isInfinite() && e.getDuration() < 200 ? Y : W));
            }
            ry = drawBlock(ctx, tr, E, 0, ry, true, sw - c.hudX);
        }
        if (c.on("players")) {
            List<AbstractClientPlayerEntity> others = new ArrayList<>(mc.world.getPlayers());
            others.remove(p);
            others.sort(Comparator.comparingDouble(o -> p.distanceTo(o)));
            List<Line> P = new ArrayList<>();
            for (int i = 0; i < Math.min(5, others.size()); i++) {
                AbstractClientPlayerEntity o = others.get(i);
                P.add(new Line(o.getName().getString() + "  " + (int) p.distanceTo(o) + "м", Y));
            }
            if (!P.isEmpty()) P.add(0, new Line("Игроки рядом:", GR));
            ry = drawBlock(ctx, tr, P, 0, ry, true, sw - c.hudX);
        }
        if (c.on("waypoints")) {
            List<Config.Waypoint> all = new ArrayList<>(c.waypoints);
            if (c.death != null) all.add(c.death);
            List<Config.Waypoint> near = new ArrayList<>();
            for (Config.Waypoint w : all) if (dim.equals(w.dim)) near.add(w);
            near.sort(Comparator.comparingDouble(w -> dist(p, w)));
            List<Line> Wp = new ArrayList<>();
            for (int i = 0; i < Math.min(5, near.size()); i++) {
                Config.Waypoint w = near.get(i);
                Wp.add(new Line(arrow(p, w) + " " + w.name + "  " + (int) dist(p, w) + "м", w == c.death ? R : C));
            }
            ry = drawBlock(ctx, tr, Wp, 0, ry, true, sw - c.hudX);
        }

        if (c.on("compass")) compass(ctx, tr, p, c, dim, sw);
        if (c.on("minimap")) Minimap.render(ctx, tr, mc, c, dim, sw, sh);

        // ───────── КЛАВИШИ WASD + мышь (низ-лево) ─────────
        if (c.on("keys")) {
            int x = 4, y = sh - 62;
            var o = mc.options;
            box(ctx, tr, x + 20, y, 18, 18, "W", o.forwardKey.isPressed());
            box(ctx, tr, x, y + 20, 18, 18, "A", o.leftKey.isPressed());
            box(ctx, tr, x + 20, y + 20, 18, 18, "S", o.backKey.isPressed());
            box(ctx, tr, x + 40, y + 20, 18, 18, "D", o.rightKey.isPressed());
            box(ctx, tr, x, y + 40, 58, 12, "SPACE", o.jumpKey.isPressed());
            box(ctx, tr, x + 62, y + 20, 28, 18, "ЛКМ", lPrev);
            box(ctx, tr, x + 92, y + 20, 28, 18, "ПКМ", rPrev);
        }

        // ───────── ЦЕЛЬ ПОД ПРИЦЕЛОМ ─────────
        if (c.on("target") && mc.crosshairTarget != null) {
            HitResult h = mc.crosshairTarget;
            String s = null;
            if (h instanceof EntityHitResult eh) {
                Entity e = eh.getEntity();
                s = e.getName().getString();
                if (e instanceof LivingEntity le)
                    s += String.format(Locale.ROOT, "  HP %.1f/%.0f", le.getHealth(), le.getMaxHealth());
                s += "  " + (int) p.distanceTo(e) + "м";
            } else if (h instanceof BlockHitResult bh && h.getType() == HitResult.Type.BLOCK) {
                s = mc.world.getBlockState(bh.getBlockPos()).getBlock().getName().getString();
            }
            if (s != null) centerText(ctx, tr, s, sw / 2, sh / 2 + 14, W);
        }

        // ───────── ПРЕДУПРЕЖДЕНИЯ ─────────
        if (c.on("warnings") && (System.currentTimeMillis() / 500) % 2 == 0) {
            List<String> w = new ArrayList<>();
            float hp = p.getHealth();
            if (hp > 0 && hp <= 6) w.add("НИЗКОЕ ЗДОРОВЬЕ!");
            if (p.getHungerManager().getFoodLevel() <= 6) w.add("ГОЛОД!");
            double a = 1;
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack st = p.getEquippedStack(s);
                if (!st.isEmpty() && st.isDamageable()) a = Math.min(a, (st.getMaxDamage() - st.getDamage()) / (double) st.getMaxDamage());
            }
            if (a < 0.1) w.add("БРОНЯ СКОРО СЛОМАЕТСЯ!");
            ItemStack hand = p.getMainHandStack();
            if (!hand.isEmpty() && hand.isDamageable() && hand.getMaxDamage() >= 30
                    && (hand.getMaxDamage() - hand.getDamage()) < hand.getMaxDamage() * 0.1)
                w.add("ИНСТРУМЕНТ СКОРО СЛОМАЕТСЯ!");
            for (int i = 0; i < w.size(); i++) centerText(ctx, tr, w.get(i), sw / 2, sh / 2 - 60 + i * 11, R);
        }
    }

    // ───────── helpers ─────────

    private static void refresh(ClientPlayerEntity p) {
        long sl = System.currentTimeMillis() / 250;
        Item hand = p.getMainHandStack().getItem();
        if (sl == cacheSlice && hand == cacheHand) return;
        cacheSlice = sl;
        cacheHand = hand;
        cTotems = cArrows = cFree = cHand = 0;
        var inv = p.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack st = inv.getStack(i);
            if (st.isEmpty()) { cFree++; continue; }
            if (st.isOf(Items.TOTEM_OF_UNDYING)) cTotems += st.getCount();
            else if (st.isOf(Items.ARROW)) cArrows += st.getCount();
            if (st.isOf(hand)) cHand += st.getCount();
        }
        ItemStack off = p.getOffHandStack();
        if (off.isOf(Items.TOTEM_OF_UNDYING)) cTotems += off.getCount();
        if (off.isOf(Items.ARROW)) cArrows += off.getCount();
        if (off.isOf(hand)) cHand += off.getCount();
    }

    private static void compass(DrawContext ctx, TextRenderer tr, ClientPlayerEntity p, Config c, String dim, int sw) {
        int w = 200, cx = sw / 2, y = 4 + c.hudY;
        ctx.fill(cx - w / 2, y - 2, cx + w / 2, y + 11, BG);
        float yaw = p.getYaw();
        String[] names = {"Ю", "З", "С", "В"};
        float[] ang = {0, 90, 180, 270};
        for (int i = 0; i < 4; i++) {
            double rel = MathHelper.wrapDegrees(ang[i] - yaw);
            if (Math.abs(rel) > 90) continue;
            centerText(ctx, tr, names[i], cx + (int) (rel * w / 180.0), y, W);
        }
        List<Config.Waypoint> all = new ArrayList<>(c.waypoints);
        if (c.death != null) all.add(c.death);
        Config.Waypoint best = null;
        double bestRel = 999;
        for (Config.Waypoint wp : all) {
            if (!dim.equals(wp.dim)) continue;
            double dx = wp.x + 0.5 - p.getX(), dz = wp.z + 0.5 - p.getZ();
            double rel = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)) - yaw);
            if (Math.abs(rel) > 90) continue;
            int px = cx + (int) (rel * w / 180.0);
            ctx.fill(px, y - 1, px + 2, y + 9, wp == c.death ? R : C);
            if (Math.abs(rel) < Math.abs(bestRel)) { best = wp; bestRel = rel; }
        }
        ctx.fill(cx, y + 9, cx + 1, y + 12, Y);
        if (best != null && Math.abs(bestRel) < 15)
            centerText(ctx, tr, best.name + "  " + (int) dist(p, best) + "м", cx, y + 14, best == c.death ? R : C);
    }

    private static void pollCps(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        if (mc.currentScreen == null) {
            long h = mc.getWindow().getHandle();
            boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            if (l && !lPrev) lc.add(now);
            if (r && !rPrev) rc.add(now);
            lPrev = l;
            rPrev = r;
        } else { lPrev = false; rPrev = false; }
        while (!lc.isEmpty() && now - lc.peekFirst() > 1000) lc.pollFirst();
        while (!rc.isEmpty() && now - rc.peekFirst() > 1000) rc.pollFirst();
    }

    private static int drawBlock(DrawContext ctx, TextRenderer tr, List<Line> lines, int x, int y, boolean right, int sw) {
        if (lines.isEmpty()) return y;
        int maxW = 0;
        for (Line l : lines) maxW = Math.max(maxW, tr.getWidth(l.t()));
        int bx = right ? sw - 4 - maxW : x;
        ctx.fill(bx - 2, y - 2, bx + maxW + 2, y + lines.size() * 10, BG);
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            int tx = right ? sw - 4 - tr.getWidth(l.t()) : x;
            ctx.drawTextWithShadow(tr, l.t(), tx, y + i * 10, l.c());
        }
        return y + lines.size() * 10 + 4;
    }

    private static void box(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, boolean down) {
        ctx.fill(x, y, x + w, y + h, down ? 0xAAFFFFFF : 0x66000000);
        ctx.drawTextWithShadow(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2 + 1, down ? 0xFF000000 : 0xFFFFFFFF);
    }

    private static void centerText(DrawContext ctx, TextRenderer tr, String s, int cx, int y, int color) {
        ctx.drawTextWithShadow(tr, s, cx - tr.getWidth(s) / 2, y, color);
    }

    private static int pctColor(int left, int max) {
        return left * 5 < max ? R : left * 2 < max ? Y : G;
    }

    private static int count(ClientPlayerEntity p, Item item) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) n += s.getCount();
        }
        return n;
    }

    private static String dirRu(Direction d) {
        return switch (d) {
            case NORTH -> "С";
            case SOUTH -> "Ю";
            case WEST -> "З";
            case EAST -> "В";
            default -> "?";
        };
    }

    private static double dist(ClientPlayerEntity p, Config.Waypoint w) {
        double dx = w.x + 0.5 - p.getX(), dy = w.y - p.getY(), dz = w.z + 0.5 - p.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Стрелка направления на точку относительно взгляда игрока. */
    private static String arrow(ClientPlayerEntity p, Config.Waypoint w) {
        double dx = w.x + 0.5 - p.getX(), dz = w.z + 0.5 - p.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = MathHelper.wrapDegrees(targetYaw - p.getYaw());
        int idx = (int) Math.round(rel / 45.0);
        return ARROWS[((idx % 8) + 8) % 8];
    }
}
