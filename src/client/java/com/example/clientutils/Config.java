package com.example.clientutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Настройки мода. Хранятся в config/clientutils.json */
public final class Config {
    public record Def(String id, String title, boolean def) {}
    public record KeyDef(String id, String title, int def) {}

    public static final int MAX_WP = 50;

    public static final class Waypoint {
        public String name, dim;
        public int x, y, z;
        public Waypoint() {}
        public Waypoint(String name, String dim, int x, int y, int z) {
            this.name = name; this.dim = dim; this.x = x; this.y = y; this.z = z;
        }
    }

    /** Все переключаемые модули (порядок = порядок в меню). Только информационные, без влияния на геймплей. */
    public static final List<Def> DEFS = List.of(
            new Def("hud", "HUD целиком", true),
            new Def("fps", "FPS", true),
            new Def("ping", "Пинг", true),
            new Def("coords", "Координаты + сторона света", true),
            new Def("nether", "Пересчёт координат Нижний/Обычный", true),
            new Def("biome", "Биом", false),
            new Def("clock", "Время: реальное, игровое, день", true),
            new Def("speed", "Скорость (блоки/сек)", false),
            new Def("light", "Уровень света (блок / небо)", false),
            new Def("session", "Время сессии", false),
            new Def("memory", "Память Java", false),
            new Def("slots", "Свободные слоты инвентаря", false),
            new Def("counts", "Тотемы / стрелы / предмет в руке", true),
            new Def("armor", "Броня и прочность", true),
            new Def("effects", "Эффекты и таймеры", true),
            new Def("target", "Цель под прицелом", true),
            new Def("players", "Игроки рядом", true),
            new Def("keys", "Клавиши WASD / мышь", false),
            new Def("cps", "CPS (клики/сек)", true),
            new Def("waypoints", "Точки + стрелка + точка смерти", true),
            new Def("compass", "Компас-лента с метками точек", true),
            new Def("warnings", "Предупреждения (HP, голод, прочность)", true),
            new Def("zoom", "Зум (удерживать клавишу)", true),
            new Def("minimap", "Миникарта", true),
            new Def("chunk", "Чанк (номер и позиция в нём)", false)
    );

    /** Клавиши (GLFW key codes). */
    public static final List<KeyDef> KEYS = List.of(
            new KeyDef("menu", "Открыть это меню", 344),      // Right Shift
            new KeyDef("zoom", "Зум (удерживать)", 90),       // Z
            new KeyDef("hud", "HUD вкл/выкл", 72),            // H
            new KeyDef("wp", "Добавить точку здесь", 78),     // N
            new KeyDef("copy", "Копировать координаты", 74),  // J
            new KeyDef("quick", "Быстрая панель", 71),        // G
            new KeyDef("map", "Большая карта", 77),           // M
            new KeyDef("craft", "Справочник рецептов", 75)    // K
    );

    public static final Map<String, String[]> PRESETS = new LinkedHashMap<>();
    static {
        PRESETS.put("Минимум", new String[]{"hud", "fps", "coords"});
        PRESETS.put("Шахта", new String[]{"hud", "coords", "light", "armor", "counts", "slots", "waypoints", "warnings", "minimap"});
        PRESETS.put("Путешествие", new String[]{"hud", "coords", "nether", "biome", "clock", "speed", "compass", "waypoints", "minimap", "zoom"});
        PRESETS.put("Всё", null);
    }

    public void applyPreset(String name) {
        String[] on = PRESETS.get(name);
        java.util.Set<String> s = on == null ? null : new java.util.HashSet<>(java.util.Arrays.asList(on));
        for (Def d : DEFS) modules.put(d.id(), s == null || s.contains(d.id()));
        save();
    }

    public Map<String, Boolean> modules = new LinkedHashMap<>();
    public Map<String, Integer> keys = new LinkedHashMap<>();
    public int zoomFov = 30;
    public int mapSize = 96, mapZoom = 1, mapPos = 0;
    public int hudX = 0, hudY = 0, hudAlpha = 40;
    public List<Waypoint> waypoints = new ArrayList<>();
    public Waypoint death;

    private static Config inst;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config get() {
        if (inst == null) load();
        return inst;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("clientutils.json");
    }

    private static void load() {
        Path p = path();
        try {
            if (Files.exists(p)) inst = GSON.fromJson(Files.readString(p), Config.class);
        } catch (Exception e) {
            // битый файл: сохраняем копию, чтобы не потерять точки
            try { Files.copy(p, p.resolveSibling("clientutils.json.bak"), StandardCopyOption.REPLACE_EXISTING); }
            catch (Exception ignored) {}
        }
        if (inst == null) inst = new Config();
        inst.fill();
    }

    private void fill() {
        if (modules == null) modules = new LinkedHashMap<>();
        if (keys == null) keys = new LinkedHashMap<>();
        if (waypoints == null) waypoints = new ArrayList<>();
        modules.remove("sprint"); modules.remove("bright");
        keys.remove("sprint"); keys.remove("bright");
        for (Def d : DEFS) modules.putIfAbsent(d.id(), d.def());
        for (KeyDef k : KEYS) keys.putIfAbsent(k.id(), k.def());
        if (zoomFov < 30 || zoomFov > 70) zoomFov = 30;
        if (mapSize < 64 || mapSize > 160) mapSize = 96;
        if (mapZoom != 1 && mapZoom != 2 && mapZoom != 4) mapZoom = 1;
        if (mapPos < 0 || mapPos > 2) mapPos = 0;
        hudX = Math.max(0, Math.min(60, hudX));
        hudY = Math.max(0, Math.min(60, hudY));
        hudAlpha = Math.max(0, Math.min(100, hudAlpha));
    }

    public void save() {
        try { Files.writeString(path(), GSON.toJson(this)); } catch (Exception ignored) {}
    }

    public static void reset() {
        inst = new Config();
        inst.fill();
        inst.save();
    }

    public String nextName() {
        for (int i = 1; ; i++) {
            String n = "Точка " + i;
            boolean used = false;
            for (Waypoint w : waypoints) if (n.equals(w.name)) { used = true; break; }
            if (!used) return n;
        }
    }

    public boolean on(String id) { return modules.getOrDefault(id, false); }
    public int key(String id) { return keys.getOrDefault(id, -1); }
}
