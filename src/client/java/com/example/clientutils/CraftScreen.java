package com.example.clientutils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Справочник рецептов самых нужных предметов (статический список). */
public class CraftScreen extends Screen {
    private static final String[] R = {
            "Верстак — 4 доски (2×2)",
            "Палки — 2 доски друг под другом → 4 шт",
            "Факел — уголь + палка → 4 шт",
            "Печь — 8 булыжников по кругу",
            "Сундук — 8 досок по кругу",
            "Бочка — 6 досок + 2 плиты по бокам",
            "Кровать — 3 шерсти + 3 доски в ряд",
            "Дверь — 6 досок (2×3) → 3 шт",
            "Лестница — 6 блоков «ступенькой» → 4 шт",
            "Плита — 3 блока в ряд → 6 шт",
            "Стремянка — 7 палок буквой H → 3 шт",
            "Лодка — 5 досок буквой U",
            "Кирка — 3 материала сверху + 2 палки по центру",
            "Топор — 3 материала углом + 2 палки",
            "Лопата — 1 материал + 2 палки",
            "Мотыга — 2 материала сверху + 2 палки",
            "Меч — 2 материала + палка",
            "Лук — 3 палки + 3 нити",
            "Стрела — кремень + палка + перо → 4 шт",
            "Щит — 6 досок + 1 слиток железа",
            "Ведро — 3 слитка железа буквой V",
            "Ножницы — 2 слитка железа по диагонали",
            "Кремень и сталь — слиток железа + кремень",
            "Бумага — 3 тростника в ряд → 3 шт",
            "Книга — 3 бумаги + кожа",
            "Книжная полка — 6 досок + 3 книги",
            "Стол зачарования — книга + 2 алмаза + 4 обсидиана",
            "Наковальня — 3 блока железа сверху + 1 слиток + 2 слитка снизу",
            "Воронка — 5 слитков железа + сундук",
            "Рельсы — 6 слитков железа + палка → 16 шт",
            "Вагонетка — 5 слитков железа буквой U",
            "Поршень — 3 доски + 4 булыжника + железо + редстоун",
            "Повторитель — 3 камня + 2 красных факела + редстоун",
            "Раздатчик — 7 булыжников + лук; Выбрасыватель — 7 булыжников + редстоун",
            "Компас — 4 слитка железа + редстоун",
            "Часы — 4 слитка золота + редстоун",
            "Карта — 8 бумаги + компас",
            "Золотое яблоко — 8 слитков золота + яблоко",
            "Маяк — 5 стекла + звезда Нижнего мира + 3 обсидиана",
            "Блок из слитков/самоцветов — 9 штук (3×3)"
    };
    private static final int PER = 12;
    private int page;
    private boolean closeArmed;

    public CraftScreen() {
        super(Text.literal("Рецепты"));
    }

    @Override
    protected void init() {
        int pages = (R.length + PER - 1) / PER;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> page = (page + pages - 1) % pages)
                .dimensions(width / 2 - 75, height - 28, 40, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(width / 2 - 30, height - 28, 60, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> page = (page + 1) % pages)
                .dimensions(width / 2 + 35, height - 28, 40, 20).build());
    }

    @Override
    public void tick() {
        if (client == null) return;
        boolean down = Keys.down(client, Config.get().key("craft"));
        if (!down) closeArmed = true;
        else if (closeArmed) close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int pages = (R.length + PER - 1) / PER;
        String t = "Справочник рецептов — стр. " + (page + 1) + "/" + pages;
        ctx.drawTextWithShadow(textRenderer, t, width / 2 - textRenderer.getWidth(t) / 2, 8, 0xFFFFFFFF);
        for (int i = 0; i < PER; i++) {
            int idx = page * PER + i;
            if (idx >= R.length) break;
            ctx.drawTextWithShadow(textRenderer, R[idx], 12, 28 + i * 14, 0xFFEEEEEE);
        }
    }
}
