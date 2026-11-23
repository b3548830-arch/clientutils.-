package com.example.clientutils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Быстрая панель: крупные кнопки под палец, чтобы не тратить клавиши. */
public class QuickScreen extends Screen {
    private boolean closeArmed;

    public QuickScreen() { super(Text.literal("Быстрая панель")); }

    private void btn(int col, int row, String t, Runnable r) {
        addDrawableChild(ButtonWidget.builder(Text.literal(t), b -> r.run())
                .dimensions(width / 2 - 155 + col * 160, 24 + row * 24, 150, 20).build());
    }

    @Override
    protected void init() {
        Config c = Config.get();
        btn(0, 0, "Большая карта", () -> client.setScreen(new MapScreen()));
        btn(1, 0, "Рецепты", () -> client.setScreen(new CraftScreen()));
        btn(0, 1, "Точка здесь", () -> {
            if (c.waypoints.size() >= Config.MAX_WP) return;
            BlockPos p = client.player.getBlockPos();
            c.waypoints.add(new Config.Waypoint(c.nextName(), ClientUtilsMod.dim(client), p.getX(), p.getY(), p.getZ()));
            c.save();
            client.player.sendMessage(Text.literal("Точка добавлена"), true);
        });
        btn(1, 1, "Копировать координаты", () -> {
            BlockPos p = client.player.getBlockPos();
            client.keyboard.setClipboard(p.getX() + " " + p.getY() + " " + p.getZ());
            client.player.sendMessage(Text.literal("Координаты скопированы"), true);
        });
        btn(0, 2, "HUD вкл/выкл", () -> { c.modules.put("hud", !c.on("hud")); c.save(); });
        btn(1, 2, "Миникарта вкл/выкл", () -> { c.modules.put("minimap", !c.on("minimap")); c.save(); });
        btn(0, 3, "Размер карты", () -> { c.mapSize = c.mapSize >= 160 ? 64 : c.mapSize + 32; c.save(); });
        btn(1, 3, "Масштаб карты", () -> { c.mapZoom = c.mapZoom >= 4 ? 1 : c.mapZoom * 2; c.save(); });
        int i = 0;
        for (String name : Config.PRESETS.keySet()) {
            final String n = name;
            btn(i % 2, 4 + i / 2, "Пресет: " + n, () -> { c.applyPreset(n); client.player.sendMessage(Text.literal("Пресет: " + n), true); });
            i++;
        }
        btn(0, 6, "Меню", () -> client.setScreen(new MenuScreen()));
        btn(1, 6, "Закрыть", this::close);
    }

    @Override
    public void tick() {
        if (client == null) return;
        boolean down = Keys.down(client, Config.get().key("quick"));
        if (!down) closeArmed = true;
        else if (closeArmed) close();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        ctx.drawTextWithShadow(textRenderer, "Быстрая панель", width / 2 - textRenderer.getWidth("Быстрая панель") / 2, 8, 0xFFFFFFFF);
    }
}
