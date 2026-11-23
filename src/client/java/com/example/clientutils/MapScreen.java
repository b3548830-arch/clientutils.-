package com.example.clientutils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class MapScreen extends Screen {
    private int step = 2, n, x0, y0;
    private int[] grid;
    private long last;
    private boolean closeArmed;

    public MapScreen() {
        super(Text.literal("Карта"));
    }

    @Override
    protected void init() {
        n = Math.max(64, Math.min(320, Math.min(width - 20, height - 70)));
        x0 = (width - n) / 2;
        y0 = 28;
        grid = null;
        addDrawableChild(ButtonWidget.builder(Text.literal("Приблизить"), b -> { step = Math.max(1, step / 2); grid = null; })
                .dimensions(width / 2 - 125, 4, 80, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Отдалить"), b -> { step = Math.min(16, step * 2); grid = null; })
                .dimensions(width / 2 - 40, 4, 80, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(width / 2 + 45, 4, 80, 18).build());
    }

    @Override
    public void tick() {
        if (client == null) return;
        boolean down = Keys.down(client, Config.get().key("map"));
        if (!down) closeArmed = true;
        else if (closeArmed) close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        if (client == null || client.world == null || client.player == null) return;
        ClientPlayerEntity p = client.player;
        long now = System.currentTimeMillis();
        if (grid == null || now - last > 1000) {
            grid = Minimap.sample(client.world, p.getBlockX(), p.getBlockZ(), p.getBlockY(), n, step);
            last = now;
        }
        ctx.fill(x0 - 2, y0 - 2, x0 + n + 2, y0 + n + 2, 0xFF000000);
        ctx.fill(x0, y0, x0 + n, y0 + n, 0xFF1A1A1A);
        Minimap.draw(ctx, grid, n, x0, y0);

        String dim = ClientUtilsMod.dim(client);
        for (Config.Waypoint w : Minimap.points(Config.get(), dim)) {
            int px = x0 + n / 2 + (int) Math.floor((w.x - p.getBlockX()) / (double) step);
            int py = y0 + n / 2 + (int) Math.floor((w.z - p.getBlockZ()) / (double) step);
            if (px < x0 || py < y0 || px >= x0 + n - 3 || py >= y0 + n - 3) continue;
            ctx.fill(px, py, px + 3, py + 3, w == Config.get().death ? 0xFFFF5555 : 0xFF55FFFF);
            ctx.drawTextWithShadow(textRenderer, w.name, px + 5, py - 3, 0xFFFFFFFF);
        }
        Minimap.marker(ctx, p, x0 + n / 2, y0 + n / 2);
        ctx.drawTextWithShadow(textRenderer, "С", x0 + n / 2 - 2, y0 + 2, 0xFFFFFFFF);

        String info = "Масштаб 1:" + step + "  |  Вы: " + p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ();
        if (mouseX >= x0 && mouseX < x0 + n && mouseY >= y0 && mouseY < y0 + n) {
            int bx = p.getBlockX() + (mouseX - x0 - n / 2) * step, bz = p.getBlockZ() + (mouseY - y0 - n / 2) * step;
            info += "  |  Курсор: " + bx + " " + bz;
        }
        ctx.drawTextWithShadow(textRenderer, info, width / 2 - textRenderer.getWidth(info) / 2, y0 + n + 6, 0xFFFFFFFF);
    }
                     }
