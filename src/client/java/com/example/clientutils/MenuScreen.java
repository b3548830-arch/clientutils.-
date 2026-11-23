package com.example.clientutils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Меню мода: вкладки «Модули», «Клавиши», «Прочее». Клавиши переназначаются прямо здесь. */
public class MenuScreen extends Screen {
    private static final int PAGE_SIZE = 16;
    private static final String[] TABS = {"Модули", "Клавиши", "Прочее"};

    private int tab = 0, page = 0;
    private String query = "";
    private String listening = null;
    private boolean armed = false, closeArmed = false;

    public MenuScreen() {
        super(Text.literal("Client Utils"));
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearChildren();
        Config c = Config.get();
        int cx = width / 2;

        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal((tab == i ? "» " : "") + TABS[i]),
                    b -> { tab = idx; page = 0; listening = null; rebuild(); })
                    .dimensions(cx - 140 + i * 95, 22, 90, 18).build());
        }

        int y0 = 48;
        if (tab == 0) {
            List<Config.Def> defs = new java.util.ArrayList<>();
            for (Config.Def d : Config.DEFS)
                if (query.isEmpty() || d.title().toLowerCase().contains(query.toLowerCase())) defs.add(d);
            net.minecraft.client.gui.widget.TextFieldWidget sf =
                    new net.minecraft.client.gui.widget.TextFieldWidget(textRenderer, cx - 205, height - 28, 150, 20, Text.literal("Поиск"));
            sf.setPlaceholder(Text.literal("Поиск..."));
            sf.setText(query);
            sf.setChangedListener(t -> { query = t; page = 0; rebuild(); });
            addDrawableChild(sf);
            if (!query.isEmpty()) setFocused(sf);
            int from = page * PAGE_SIZE, to = Math.min(defs.size(), from + PAGE_SIZE);
            for (int i = from; i < to; i++) {
                Config.Def d = defs.get(i);
                int n = i - from;
                int col = n / 8, row = n % 8;
                boolean on = c.on(d.id());
                Text label = Text.literal(d.title() + ": ")
                        .append(Text.literal(on ? "ВКЛ" : "ВЫКЛ").formatted(on ? Formatting.GREEN : Formatting.RED));
                addDrawableChild(ButtonWidget.builder(label, b -> {
                    c.modules.put(d.id(), !c.on(d.id()));
                    c.save();
                    rebuild();
                }).dimensions(cx - 205 + col * 205, y0 + row * 20, 200, 18).build());
            }
            int pages = (defs.size() + PAGE_SIZE - 1) / PAGE_SIZE;
            if (pages > 1) {
                addDrawableChild(ButtonWidget.builder(Text.literal("Стр. " + (page + 1) + "/" + pages),
                        b -> { page = (page + 1) % pages; rebuild(); })
                        .dimensions(cx + 55, height - 28, 70, 20).build());
            }
        } else if (tab == 1) {
            for (int i = 0; i < Config.KEYS.size(); i++) {
                Config.KeyDef k = Config.KEYS.get(i);
                boolean wait = k.id().equals(listening);
                Text label = Text.literal(k.title() + " — ")
                        .append(wait
                                ? Text.literal("> нажми клавишу <").formatted(Formatting.YELLOW)
                                : Text.literal("[" + Keys.name(c.key(k.id())) + "]").formatted(Formatting.AQUA));
                addDrawableChild(ButtonWidget.builder(label, b -> {
                    listening = k.id();
                    armed = false;
                    rebuild();
                }).dimensions(cx - 140, y0 + i * 22, 280, 20).build());
            }
        } else {
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Угол зума: " + c.zoomFov + "° (нажми — сменить)"),
                    b -> { c.zoomFov = c.zoomFov >= 60 ? 30 : c.zoomFov + 10; c.save(); rebuild(); })
                    .dimensions(cx - 205, y0, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Фон HUD: " + c.hudAlpha + "%"),
                    b -> { c.hudAlpha = c.hudAlpha >= 80 ? 0 : c.hudAlpha + 20; c.save(); rebuild(); })
                    .dimensions(cx - 205, y0 + 24, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Отступ HUD по X: " + c.hudX),
                    b -> { c.hudX = c.hudX >= 60 ? 0 : c.hudX + 20; c.save(); rebuild(); })
                    .dimensions(cx - 205, y0 + 48, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Отступ HUD по Y: " + c.hudY),
                    b -> { c.hudY = c.hudY >= 60 ? 0 : c.hudY + 20; c.save(); rebuild(); })
                    .dimensions(cx - 205, y0 + 72, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Очистить точки (" + c.waypoints.size() + ")"),
                    b -> { c.waypoints.clear(); c.save(); rebuild(); })
                    .dimensions(cx - 205, y0 + 96, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Удалить точку смерти"),
                    b -> { c.death = null; c.save(); })
                    .dimensions(cx - 205, y0 + 120, 200, 20).build());
            String[] posN = {"справа по центру", "снизу справа", "слева по центру"};
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Карта: размер " + c.mapSize),
                    b -> { c.mapSize = c.mapSize >= 160 ? 64 : c.mapSize + 32; c.save(); rebuild(); })
                    .dimensions(cx + 5, y0, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Карта: масштаб 1:" + c.mapZoom),
                    b -> { c.mapZoom = c.mapZoom >= 4 ? 1 : c.mapZoom * 2; c.save(); rebuild(); })
                    .dimensions(cx + 5, y0 + 24, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Карта: " + posN[c.mapPos]),
                    b -> { c.mapPos = (c.mapPos + 1) % 3; c.save(); rebuild(); })
                    .dimensions(cx + 5, y0 + 48, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Сбросить ВСЕ настройки").formatted(Formatting.RED),
                    b -> { Config.reset(); rebuild(); })
                    .dimensions(cx - 205, y0 + 144, 200, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(cx - 50 + (tab == 0 ? 0 : 0), height - 28, 100, 20).build());
    }

    @Override
    public void tick() {
        if (client == null) return;
        if (listening == null) {
            // повторное нажатие клавиши меню закрывает его
            boolean md = Keys.down(client, Config.get().key("menu"));
            if (!md) closeArmed = true;
            else if (closeArmed) close();
            return;
        }
        long h = client.getWindow().getHandle();
        int pressed = -1;
        for (int k = 32; k <= 348; k++) {
            if (k == GLFW.GLFW_KEY_ESCAPE) continue;
            if (GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS) { pressed = k; break; }
        }
        if (pressed == -1) { armed = true; return; }   // ждём, пока все клавиши отпущены
        if (!armed) return;
        Config c = Config.get();
        c.keys.put(listening, pressed == GLFW.GLFW_KEY_BACKSPACE ? -1 : pressed);
        c.save();
        listening = null;
        armed = false;
        rebuild();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        center(ctx, "Client Utils — меню (открыть: " + Keys.name(Config.get().key("menu")) + ")", 8, 0xFFFFFFFF);
        if (tab == 1) {
            center(ctx, "Нажми на кнопку, затем на нужную клавишу. Backspace — снять. Esc — закрыть меню.",
                    height - 42, 0xFFAAAAAA);
        } else if (tab == 2) {
            center(ctx, "Точки: /cu add <имя>, /cu list, /cu del <№>, /cu rename <№> <имя>", height - 42, 0xFFAAAAAA);
        }
    }

    private void center(DrawContext ctx, String s, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, s, width / 2 - textRenderer.getWidth(s) / 2, y, color);
    }

    @Override
    public void removed() {
        Config.get().save();
    }
}
