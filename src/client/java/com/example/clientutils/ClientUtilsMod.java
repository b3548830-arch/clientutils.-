package com.example.clientutils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Client Utils — клиентский мод для Fabric, Minecraft 1.21.11.
 * Только информационные функции (HUD, точки, компас, зум): ничего не меняет в геймплее и не шлёт лишнего на сервер.
 */
public class ClientUtilsMod implements ClientModInitializer {
    public static volatile double speed = 0;
    public static volatile long sessionStart = System.currentTimeMillis();

    private final Map<String, Boolean> prev = new HashMap<>();
    private Object lastWorld;
    private double lastX, lastZ;
    private boolean zooming, deathSaved;
    private int savedFov = 70;
    private BlockPos lastAlivePos;
    private String lastAliveDim = "";

    @Override
    public void onInitializeClient() {
        Config.get();
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> restoreZoom(mc));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> Cmds.register(dispatcher));
        HudElementRegistry.addLast(Identifier.of("clientutils", "hud"), (ctx, tick) -> Hud.render(ctx, tick));
    }

    public static String dim(MinecraftClient mc) {
        return mc.world == null ? "" : mc.world.getRegistryKey().getValue().toString();
    }

    private boolean edge(MinecraftClient mc, String id) {
        boolean now = Keys.down(mc, Config.get().key(id));
        boolean was = prev.getOrDefault(id, false);
        prev.put(id, now);
        return now && !was;
    }

    private void toggle(MinecraftClient mc, String id, String title) {
        Config c = Config.get();
        boolean v = !c.on(id);
        c.modules.put(id, v);
        c.save();
        mc.player.sendMessage(Text.literal(title + ": " + (v ? "ВКЛ" : "ВЫКЛ")), true);
    }

    private void onTick(MinecraftClient mc) {
        Config c = Config.get();
        if (mc.player == null || mc.world == null) {
            lastWorld = null;
            restoreZoom(mc);   // выход из мира во время зума не оставляет FOV 30
            return;
        }
        if (mc.world != lastWorld) { lastWorld = mc.world; sessionStart = System.currentTimeMillis(); lastX = mc.player.getX(); lastZ = mc.player.getZ(); }

        boolean free = mc.currentScreen == null;
        boolean eMenu = edge(mc, "menu"), eHud = edge(mc, "hud"),
                eWp = edge(mc, "wp"), eCopy = edge(mc, "copy"),
                eMap = edge(mc, "map"), eQuick = edge(mc, "quick"), eCraft = edge(mc, "craft");

        if (free) {
            if (eMenu) { mc.setScreen(new MenuScreen()); return; }
            if (eQuick) { mc.setScreen(new QuickScreen()); return; }
            if (eMap) { mc.setScreen(new MapScreen()); return; }
            if (eCraft) { mc.setScreen(new CraftScreen()); return; }
            if (eHud) toggle(mc, "hud", "HUD");
            if (eWp) {
                if (c.waypoints.size() >= Config.MAX_WP) {
                    mc.player.sendMessage(Text.literal("Максимум " + Config.MAX_WP + " точек (/cu del <номер> или меню → Прочее)"), true);
                } else {
                    BlockPos p = mc.player.getBlockPos();
                    c.waypoints.add(new Config.Waypoint(c.nextName(), dim(mc), p.getX(), p.getY(), p.getZ()));
                    c.save();
                    mc.player.sendMessage(Text.literal("Точка добавлена: " + p.getX() + " " + p.getY() + " " + p.getZ()), true);
                }
            }
            if (eCopy) {
                BlockPos p = mc.player.getBlockPos();
                mc.keyboard.setClipboard(p.getX() + " " + p.getY() + " " + p.getZ());
                mc.player.sendMessage(Text.literal("Координаты скопированы"), true);
            }
        }

        // скорость (сглаженная)
        double dx = mc.player.getX() - lastX, dz = mc.player.getZ() - lastZ;
        speed = speed * 0.8 + Math.sqrt(dx * dx + dz * dz) * 20 * 0.2;
        lastX = mc.player.getX();
        lastZ = mc.player.getZ();

        // точка смерти
        if (mc.currentScreen instanceof DeathScreen) {
            if (!deathSaved && lastAlivePos != null) {
                c.death = new Config.Waypoint("Смерть", lastAliveDim, lastAlivePos.getX(), lastAlivePos.getY(), lastAlivePos.getZ());
                c.save();
                deathSaved = true;
                mc.player.sendMessage(Text.literal("Точка смерти: " + lastAlivePos.getX() + " " + lastAlivePos.getY() + " " + lastAlivePos.getZ()), false);
            }
        } else if (mc.player.getHealth() > 0) {
            deathSaved = false;
            lastAlivePos = mc.player.getBlockPos();
            lastAliveDim = dim(mc);
        }

        // зум
        boolean wantZoom = c.on("zoom") && free && Keys.down(mc, c.key("zoom"));
        if (wantZoom) {
            if (!zooming) { savedFov = mc.options.getFov().getValue(); zooming = true; }
            mc.options.getFov().setValue(c.zoomFov);
        } else if (zooming) {
            restoreZoom(mc);
        }
    }

    private void restoreZoom(MinecraftClient mc) {
        if (zooming) {
            mc.options.getFov().setValue(savedFov);
            zooming = false;
        }
    }
}
