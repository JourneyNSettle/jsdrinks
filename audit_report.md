# Аудит аддона Journey Drinks (`jsdrinks`) v1.1

## ЭТАП 1: МАТРИЦА ТРЕБОВАНИЙ ТЗ

| ID | Требование | Ожидаемое поведение | Граничные условия |
|----|-----------|---------------------|-------------------|
| R01 | **Чайный куст** — tall bush 2 блока | Саженец → куст (LOWER+UPPER), съём листа ПКМ | Рост только в подходящем климате |
| R02 | **Куст → дерево** за 2 года без сбора | `seasonsWithoutHarvest >= 24 месяца` → трансформация, обратно нельзя | Порог в конфиге; dormant/нет климата — год не капает; любой съём сбрасывает |
| R03 | **Чайное дерево** — фруктовое TFC | sapling/trunk/branch/leaves, сезоны, ПКМ-съём, splicing, саженцы с локтя | Fruitwood, нет отдельной породы |
| R04 | **Кофейное дерево** — фруктовое TFC | Саженец → сразу дерево, плод = ягода кофе | Кустовой стадии нет |
| R05 | **Кофе: 2 года до плодов в мире** | Первые 2 TFC-года все стадии кроме плодоношения; с 3-го — ягоды | Счёт на стволе с посадки |
| R06 | **Бонсай (теплица FL)** — чай и кофе | Саженцы в bonsai planter; куст нельзя; чай сразу дерево; кофе без ожидания | Trellis/quad/hydroponic/hanging — нет |
| R07 | **Лом куста** → 1 саженец гарантированно | Слом нижней части — дроп `tea_sapling` | Не в креативе |
| R08 | **Съём рукой vs ножом** | Нож: +1 прочность, больше листа, без тегов. Рука: меньше, теги (select/large_leaf/quality) | |
| R09 | **Тег «отборный»** (`select`) | С куста рукой; живёт до зелёного чая; нужен для жёлтого | Не переносится в жидкость |
| R10 | **Тег «крупнолистовой»** (`large_leaf`) | С дерева рукой; нужен для пуэра | Без него пуэр портится |
| R11 | **Тег «качественный»** (`quality`) | Кофе рукой; влияет на decay/подсказку | Не переходит в жидкость |
| R12 | **Производство кофе** — полная цепочка | Ягода → сушка FL → нож → сырое зерно → сушка → обжарка → мельница → молотый | Горелое при перегреве; треск рядом |
| R13 | **Ручная мельница** (не жёрнов TFC) | Отдельный предмет с прочностью, крафт | Горелый остаётся на предмете |
| R14 | **Заварка кофе (котёл)** | 1×500, 2×500 крепкий, 2×1000, 4×1000 крепкий | |
| R15 | **Молоко в кофе** | 100 mB на 500 mB кофе; в котле или бочке | Недостаток → не идёт; избыток → съедается нужное |
| R16 | **Белый чай** | Свежий лист → коврик FL → белый | |
| R17 | **Зелёный чай** | Свежий → обжарка → влажный → коврик → зелёный | Отборный тянется до сюда |
| R18 | **Жёлтый чай** | Зелёный с тегом `select` → коврик 2 суток → жёлтый | Без тега — не даёт; дождь сбрасывает |
| R19 | **Красный чай** | Свежий → vat FL → мятый → куча 1 сутки → ферментированный → печь → красный | Перегрев → горелый |
| R20 | **Пуэр и крепкий пуэр** | Ферментированный + куча 2 месяца; крупнолистовой обязателен; на земле — шанс крепкого и порчи | Дождь стопит таймер |
| R21 | **Жидкости** — не ставятся источником | Только сосуды, 10 жидкостей | |
| R22 | **Эффекты напитков** | Кофе→Haste, белый→холод CS, красный→тепло CS, зелёный→Speed, жёлтый→Speed+Jump, пуэр→Haste, крепкий пуэр→Haste+Strength | Cold Sweat опционально |
| R23 | **Ворлдген** | 4 вида: куст (часто), дерево (реже), крупное дерево (очень редко, не вырастить), кофе (редко, ≥3 года) | Через TFC features + biome/climate tags |
| R24 | **Горелый** | Только короче хранится предмет. На жидкость/эффект не влияет | |
| R25 | **JEI / Field Guide / Patchouli** | Все цепочки видны | |
| R26 | **Локали** | `en_us` + `ru_ru` | |
| R27 | **Конфиг порога куст→дерево** | Порог НЕ зашит в код | |

---

## ЭТАП 2: ВЕРИФИКАЦИЯ ПО ИСХОДНИКАМ TFC / FIRMALIFE

### 2.1 Наследование и интерфейсы

#### `TeaBushBlock extends SeasonalPlantBlock`

**Проблемы:**

1. **[BLOCKER] `BerryBushBlockEntity.reset()` не вызывается при размещении.**
   В TFC [`SeasonalPlantBlock.setPlacedBy()`](file:///d:/GanjaCraft/git/mod%20sources/TerraFirmaCraft/src/main/java/net/dries007/tfc/common/blocks/plant/fruit/SeasonalPlantBlock.java#L258-L262) вызывает `BerryBushBlockEntity.reset(level, pos)`. Аддон вызывает `super.setPlacedBy()` (L99), но этот `super` вызовет `BerryBushBlockEntity.reset()` → а тот ищет блок-сущность через `TFCBlockEntities.BERRY_BUSH.get()`, а не через `JSDBlockEntities.TEA_BUSH.get()`. BE типа `TeaBushBlockEntity` зарегистрирован под `JSDBlockEntities.TEA_BUSH` — **НЕ НАЙДЁТСЯ** статической utility-методом TFC, счётчик не сбросится.

2. **[MAJOR] `getTicker()` возвращает `null`.**
   `TeaBushBlock` переопределяет `getTicker()` и возвращает `null` (L82). Но `SeasonalPlantBlock` реализует `EntityBlockExtension` → `ExtendedBlock` → ticker от `ExtendedProperties`. BE нужен ticker для обновления lifecycle (через `randomTick` → `onUpdate`). Из-за `null` tickera не вызывается `super.onUpdate()` через стандартный pipeline. Аддон полагается **только** на `randomTick` (L136), что вызывает `super.randomTick()`. В TFC `SeasonalPlantBlock.randomTick()` на L74-77 вызывает `plant.onUpdate()`. Это должно работать через цепочку `randomTick → onUpdate`, но **getTicker()=null убирает serverTick у BE**, что может сломать вещи при chunk reload (LastUpdateTick не обновляется при дозагрузке чанка).

3. **[MAJOR] Счётчик `seasonsWithoutHarvest` считает ПО ГОДАМ, а не по сезонам.**
   ТЗ R02 говорит: «Порог: 2 **года** = 24 **месяца TFC**». Но код в `TeaBushBlockEntity.checkYearlyGrowth()` (L44-53) увеличивает `seasonsWithoutHarvest` **на 1 за год**, а порог стоит `>= 2`. Это **2 года**, а ТЗ тоже говорит 2 года / 24 месяца. Название поля `seasonsWithoutHarvest` вводит в заблуждение (считает годы, не сезоны), но по факту логика совпадает с ТЗ.

4. **[MINOR] Нет проверки dormant/нет климата перед инкрементом годового счётчика.**
   ТЗ R02: «Dormant / нет плодового сезона / нет климата → год не капает». Код в `checkYearlyGrowth()` не проверяет, был ли плодовый сезон в этом году — просто считает смену `currentYear`. Куст в неподходящем климате (всегда dormant) всё равно будет набирать годы и превратится в дерево.

5. **[MAJOR] `TeaBushBlock` не передаёт тег `select` при всех путях.**
   При сборе рукой (L164-168) шанс 35% на `select` — ОК. Но ТЗ R08 говорит: нож → «нет отборного» — корректно (L160-162 нет тега). При сборе с **дерева** рукой — должен быть `large_leaf`, а не `select` — это другой блок (`JSDTeaLeavesBlock`), там корректно.

#### `JSDTeaLeavesBlock extends FruitTreeLeavesBlock`

6. **[MINOR] `getLifecycleForCurrentMonth` переопределён, но просто вызывает `super`.**
   Бесполезный override на L30-33. Не ошибка, но мусорный код.

#### `JSDCoffeeLeavesBlock extends FruitTreeLeavesBlock`

7. **[BLOCKER] `onUpdate()` обращается к `stemEntity.getTicksSinceUpdate()` через `TickCounterBlockEntity`.**
   На L48-50: `if (level.getBlockEntity(stemPos) instanceof TickCounterBlockEntity stemEntity)` — это сработает, если stem (ствол) имеет BE типа `TickCounterBlockEntity` или его наследник. Но ствол — это `FruitTreeBranchBlock` (не growing), который **НЕ** имеет `blockEntity()` в своих `ExtendedProperties` (в TFC `FruitTreeBranchBlock` — не EntityBlock!). `level.getBlockEntity(stemPos)` вернёт `null` для статического ствола. Только `GrowingFruitTreeBranchBlock` (с `EntityBlockExtension`) имеет BE.

   **Результат:** для зрелых деревьев (где ствол уже стал static `FruitTreeBranchBlock`) проверка возраста **никогда не сработает** — `totalAgeTicks` будет `plant.getTicksSinceUpdate()` из самого листа, а не от ствола. Это может быть и мало (если лист недавно размещён).

8. **[MAJOR] Возраст кофе считается от самого листа, а не от ствола.**
   ТЗ R05: «Счёт лет на стволе с момента посадки». Но leaves BE — это `BerryBushBlockEntity`, их tick counter сбрасывается при каждом `onUpdate`. Реально подсчёт возраста дерева в этом коде ненадёжен. Для корректного 2-летнего порога нужен отдельный persistent counter на стволе или root.

#### `JSDFruitTreeSaplingBlock extends FruitTreeSaplingBlock`

9. **[MAJOR] `createTree()` вызывает `branch.resetCounter()` + `branch.increaseCounter()`, а TFC вызывает `TickingPlantBlockEntity.reset()` + `TickingPlantBlockEntity.addTicks()`.**
   В TFC [`FruitTreeSaplingBlock.createTree()`](file:///d:/GanjaCraft/git/mod%20sources/TerraFirmaCraft/src/main/java/net/dries007/tfc/common/blocks/plant/fruit/FruitTreeSaplingBlock.java#L168-L182) (L177-178):
   ```java
   TickingPlantBlockEntity.reset(level, pos);
   TickingPlantBlockEntity.addTicks(level, pos, ticksToAdd);
   ```
   Эти статические методы ищут BE через `TFCBlockEntities.TICK_COUNTING_PLANT.get()`. Аддон переопределяет `createTree()` и использует прямой доступ через `instanceof JSDTickingPlantBlockEntity branch` (L47) — это **правильно**, потому что обходит проблему несовпадения BE типов.

#### `JSDGrowingFruitTreeBranchBlock extends GrowingFruitTreeBranchBlock`

10. **[MAJOR] `super.randomTick()` вызывает `TickingPlantBlockEntity.reset()` с TFC BE type.**
    В TFC [`GrowingFruitTreeBranchBlock.randomTick()`](file:///d:/GanjaCraft/git/mod%20sources/TerraFirmaCraft/src/main/java/net/dries007/tfc/common/blocks/plant/fruit/GrowingFruitTreeBranchBlock.java#L220-L233) (L227):
    ```java
    TickingPlantBlockEntity.reset(level, pos);
    ```
    Это ищет BE через `TFCBlockEntities.TICK_COUNTING_PLANT.get()`, а аддон регистрирует свой BE как `JSDBlockEntities.TICKING_PLANT`. **Lookup не найдёт BE!** Аналогично, `this.tick()` на L231 вызывает `tick()` (L240) который ищет `instanceof TickingPlantBlockEntity counter` — это полиморфно сработает, но `counter.getTicksSinceUpdate()` может вернуть неверное значение, если `reset()` не сработал.

    **Аддон пытается компенсировать это**, вызывая `JSDTickingPlantBlockEntity.reset()` (L30) перед `super.randomTick()` (L32), но это **только для случая плохого климата**. В хорошем климате `super.randomTick()` идёт в `else`-ветку (L229-232), где `TickingPlantBlockEntity.reset()` тоже может вызываться.

### 2.2 Data Components / Capabilities

11. **[OK]** `JSDDataComponents` корректно использует `Codec.BOOL`, `Codec.INT`, `ByteBufCodecs.BOOL`, `ByteBufCodecs.VAR_INT`. Соответствует паттерну NeoForge 1.21.1.

12. **[OK]** `HeatCapability.get(stack)` в `JSDEvents` (L66) — стандартный API TFC для получения температуры предмета.

### 2.3 Рецепты и кодеки

13. **[OK]** `tfc:pot` рецепты — корректный формат. `fluid_ingredient` + `ingredients` + `fluid_output`.

14. **[OK]** `tfc:heating` рецепты — корректный формат. `temperature` + `result_item` с modifiers.

15. **[OK]** `firmalife:drying` рецепты — корректный формат.

16. **[OK]** `firmalife:stomping` рецепт — корректный формат для vat FL.

17. **[OK]** `tfc:barrel_instant_fluid` для молока — корректный формат.

18. **[MAJOR] Рецепт `grind_coffee` использует `tfc:advanced_shapeless_crafting` с `primary_ingredient`.**
    Используется `jsdrinks:manual_mill` как primary_ingredient. Крафт: мельница + обжаренное зерно → молотый кофе + повреждённая мельница. Это **не** ручная мельница TFC (жёрнов), а свой предмет. Соответствует ТЗ R13.

19. **[OK]** `ItemStackModifiers` — `COPY_COMPONENTS`, `ROAST`, `ADD_BURNT` корректно зарегистрированы. `MapCodec.unit()` и `StreamCodec.unit()` — паттерн для stateless modifiers. Сигнатура `ItemStackModifier.apply(ItemStack, ItemStack, Context)` соблюдена.

### 2.4 Флюиды

20. **[OK]** `MixingFluid.Source::new` / `MixingFluid.Flowing::new` — корректное использование TFC API. `RegistrationHelpers.registerFluid()` — стандартный паттерн TFC.

21. **[OK]** `FluidRendererExtension` с `TFCFluids.ALPHA_MASK` — стандартный паттерн TFC для клиентской окраски жидкостей.

22. **[OK]** Жидкости зарегистрированы без блока и без ведра (`no in-world block, no bucket item!`). Соответствует ТЗ R21.

### 2.5 FirmaLife интеграция (Бонсай)

23. **[OK]** `firmalife:plantable` JSON'ы с `"planter": "bonsai"` — корректный формат для FL bonsai planter.

24. **[MAJOR] Кофе в бонсае: нет механизма отключения 2-летнего ожидания.**
    ТЗ R06: «Кофе в бонсае плодоносит сразу, без ожидания 2 лет». Но `JSDCoffeeLeavesBlock.onUpdate()` проверяет `totalAgeTicks < twoYearsTicks` **для всех листьев** (L54), включая бонсай. Бонсай FL не ставит отдельный листовой блок — он использует `plantable` JSON и свой рендер. **Этот конкретный вопрос не критичен для бонсая**, т.к. бонсай FL не вызывает `onUpdate()` листьев — он работает через свой internal tick system (`LargePlanterBlockEntity`). Бонсай не размещает реальные блоки листьев в мире. Так что ограничение 2 лет **не применится** к бонсаю. Однако это работает «случайно», а не по design.

---

## ЭТАП 3: СВЕРКА С ТЗ

| ID | Статус | Файл/метод | Расхождения |
|----|--------|------------|-------------|
| R01 | ✅ РЕАЛИЗОВАНО | [TeaBushBlock](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/TeaBushBlock.java) | 2-блочный куст с LOWER/UPPER, `canSurvive`, `updateShape`, `setPlacedBy` |
| R02 | ⚠️ ЧАСТИЧНО | [TeaBushBlockEntity](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/entity/TeaBushBlockEntity.java) | **Порог зашит в код (`>= 2`), нет конфига.** Нет проверки dormant/климата — счётчик капает даже в неподходящем климате |
| R03 | ✅ РЕАЛИЗОВАНО | [JSDBlocks](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/registry/JSDBlocks.java) | sapling/branch/growing_branch/leaves зарегистрированы, splicing наследуется от TFC |
| R04 | ✅ РЕАЛИЗОВАНО | [JSDBlocks](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/registry/JSDBlocks.java) (L68-86) | Саженец → сразу дерево. Кустовой стадии нет |
| R05 | ⚠️ ОШИБКА РЕАЛИЗАЦИИ | [JSDCoffeeLeavesBlock.onUpdate()](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/JSDCoffeeLeavesBlock.java#L42-L64) | Проверка возраста ненадёжна: stem-блок не имеет BE у зрелых деревьев (см. пункт 7-8 выше) |
| R06 | ✅ РЕАЛИЗОВАНО | [tea.json](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/resources/data/jsdrinks/firmalife/plantable/tea.json), [coffee.json](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/resources/data/jsdrinks/firmalife/plantable/coffee.json) | `"planter": "bonsai"` корректно. Куст не в тегах плантеров |
| R07 | ✅ РЕАЛИЗОВАНО | [TeaBushBlock.playerWillDestroy()](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/TeaBushBlock.java#L122-L133) | `popResource` саженца при ломке LOWER-части не в creative |
| R08 | ✅ РЕАЛИЗОВАНО | [TeaBushBlock L159-168](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/TeaBushBlock.java#L159-L168), [JSDTeaLeavesBlock L44-53](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/JSDTeaLeavesBlock.java#L44-L53), [JSDCoffeeLeavesBlock L72-80](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/JSDCoffeeLeavesBlock.java#L72-L80) | Нож: больше, без тегов, -1 прочность. Рука: меньше, с тегами |
| R09 | ✅ РЕАЛИЗОВАНО | `JSDDataComponents.SELECT`, рецепты drying/yellow_tea_leaf.json | `select` присваивается при сборе рукой с куста; нужен для жёлтого чая |
| R10 | ✅ РЕАЛИЗОВАНО | `JSDDataComponents.LARGE_LEAF`, TeaPileBlockEntity | `large_leaf` присваивается при сборе рукой с дерева; нужен для пуэра |
| R11 | ✅ РЕАЛИЗОВАНО | `JSDDataComponents.QUALITY`, JSDCoffeeLeavesBlock | `quality` = 1-5 при сборе кофе рукой |
| R12 | ✅ РЕАЛИЗОВАНО | Рецепты drying, crafting, heating | Полная цепочка: ягода → сушка → нож → сырое → сушка → обжарка → мельница |
| R13 | ✅ РЕАЛИЗОВАНО | [JSDItems.MANUAL_MILL](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/registry/JSDItems.java#L22), crafting/grind_coffee.json | Отдельный предмет durability=128, рецепт `tfc:advanced_shapeless_crafting` |
| R14 | ✅ РЕАЛИЗОВАНО | Рецепты pot/coffee_*.json, pot/strong_coffee_*.json | 1×500, 2×500 крепкий, 2×1000, 4×1000 крепкий — всё есть |
| R15 | ⚠️ ОШИБКА РЕАЛИЗАЦИИ | pot/coffee_milk_500.json | **Выход 600 mB вместо 500 mB.** ТЗ: «100 mB молока на 500 mB кофе → тот же вид с молоком» — выходит, что молоко **добавляется** к объёму (500+100=600). Формально ТЗ говорит «тот же вид с молоком», не указывая конкретный выходной объём. Возможно, это design decision, а не ошибка. Но ТЗ также говорит «избыток — съедается только нужное, остаток в том же сосуде» — что подразумевает input 500 → output 500 того же объёма. **Требуется уточнение.** Аналогично barrel: output 600 mB |
| R16 | ✅ РЕАЛИЗОВАНО | drying/white_tea_leaf.json | fresh → drying → white |
| R17 | ✅ РЕАЛИЗОВАНО | heating/moist_green_tea_leaf.json + drying/green_tea_leaf.json | fresh → heating → moist_green → drying → green, с `copy_components` |
| R18 | ✅ РЕАЛИЗОВАНО | drying/yellow_tea_leaf.json | `neoforge:data_component` ingredient с `jsdrinks:select: true` — корректно. **Нет проверки «2 суток TFC» сушки** — FL drying mat имеет свою скорость, не привязанную к ТЗ. Дождь сбрасывает у FL мата стандартно |
| R19 | ✅ РЕАЛИЗОВАНО | stomping/bruised_tea_leaf.json, TeaPileBlock, heating/red_tea_leaf.json | fresh → vat(stomping) → bruised → куча 1 день → fermented → heating → red |
| R20 | ✅ РЕАЛИЗОВАНО | [TeaPileBlockEntity](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/entity/TeaPileBlockEntity.java) | Ферментированный → куча 2 месяца; large_leaf обязателен; на земле — шанс крепкого+порчи; дождь стопит |
| R21 | ✅ РЕАЛИЗОВАНО | [JSDFluids](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/registry/JSDFluids.java#L43) | `// Vessel-only: no in-world block, no bucket item!` |
| R22 | ⚠️ ЧАСТИЧНО | Drinkable JSONs | Кофе→Haste ✅, зелёный→Speed ✅, жёлтый→Speed+Jump ✅, пуэр→Haste ✅, крепкий пуэр→Haste+Strength ✅. **Белый и красный чай: эффекты ОТСУТСТВУЮТ в drinkable JSON!** Реализованы через `JSDEvents` Java-код с Cold Sweat, но если Cold Sweat не установлен — **никаких эффектов вообще нет**. ТЗ: «Нет мода — эти эффекты не вешаются» — корректно по ТЗ |
| R23 | ✅ РЕАЛИЗОВАНО | [TeaBushFeature](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/worldgen/feature/TeaBushFeature.java), [LargeTeaTreeFeature](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/worldgen/feature/LargeTeaTreeFeature.java), [WildCoffeeTreeFeature](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/worldgen/feature/WildCoffeeTreeFeature.java) | 4 feature: bush, tree, large tree, coffee tree. Кофе с 3-year ticks ✅. **Нет обычного чайного дерева feature** (только large) — чайное дерево обычного размера из ТЗ R23 row 2 отсутствует как отдельный worldgen feature. Но `tea_tree` configured/placed features есть → это стандартный TFC fruit tree feature, что корректно |
| R24 | ✅ РЕАЛИЗОВАНО | `JSDDataComponents.BURNT`, heating/burn_*.json | Горелый тег, только на предмете, в жидкость не переносится |
| R25 | ⚠️ ЧАСТИЧНО | Patchouli entries найдены | JEI автоматически подтягивает JSON рецепты. Field Guide записи есть. Полнота не верифицирована |
| R26 | ✅ РЕАЛИЗОВАНО | en_us.json, ru_ru.json | Оба файла присутствуют |
| R27 | ❌ НЕ РЕАЛИЗОВАНО | [TeaBushBlockEntity L50](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/entity/TeaBushBlockEntity.java#L50) | Порог `>= 2` **зашит в код**. Конфига нет |

---

## ЭТАП 4: СТРЕСС-ТЕСТ И ОШИБКИ RUNTIME

### 4.1 Client vs Server

| # | Проблема | Файл | Критичность |
|---|---------|------|-------------|
| S1 | `JSDEvents.onPlayerTick` — корректно проверяет `level.isClientSide` (L51) | [JSDEvents.java](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/event/JSDEvents.java#L51) | OK |
| S2 | `JSDClientEvents` — вся клиентская логика за `FMLEnvironment.dist.isClient()` | [JourneyDrinks.java](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/JourneyDrinks.java#L31) | OK |
| S3 | `ItemBlockRenderTypes.setRenderLayer` deprecated в 1.21.1 | [JSDClientEvents.java L42-49](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/client/JSDClientEvents.java#L42-L49) | Minor — должен использовать `RegisterClientExtensionsEvent` для render types |
| S4 | `TeaBushBlock.useItemOn()` — `level.playSound(player, ...)` вызывается до `isClientSide` проверки | [TeaBushBlock.java L157](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/TeaBushBlock.java#L157) | Minor — `playSound(player, ...)` с non-null player корректно пропускает для этого клиента, но всё равно звук воспроизведётся и на клиенте, и на сервере. Стандартный паттерн TFC/MC |

### 4.2 Просадка TPS

| # | Проблема | Файл | Критичность |
|---|---------|------|-------------|
| T1 | **[MAJOR] `onPlayerTick` — 7×7×5 = 245 блоков каждые 30 тиков** | [JSDEvents.java L58](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/event/JSDEvents.java#L58) | Итерация `BlockPos.betweenClosed(-3..3, -2..2, -3..3)` = 245 позиций, каждая с `getBlockEntity()` + capability lookup. На сервере с 20+ игроков это 245×20 / 30 тиков ≈ **~163 BE lookups/tick**. С кэшированием `getGameTime() % 30` помогает, но с большим количеством игроков рядом с нагревательным оборудованием — **TPS impact**. **Рекомендация:** Добавить проверку `handler.getSlots() > 0` и/или сузить радиус |
| T2 | `TeaPileBlockEntity.serverTick` — вызывается каждый тик через BlockEntityTicker | [TeaPileBlock.java L65](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/TeaPileBlock.java#L65) | Лёгкий код (сравнение long), но если игрок натыкает 100 куч — 100 tick операций. **Добавить ранний exit** если `currentStage >= 2` уже есть (L41) — OK |

### 4.3 Null-безопасность и валидация

| # | Проблема | Файл | Критичность |
|---|---------|------|-------------|
| N1 | `TeaBushBlockEntity.checkYearlyGrowth()` — `level.isClientSide()` check (L36) | OK | |
| N2 | `JSDCoffeeLeavesBlock.onUpdate()` — `plant.getStemPos()` может вернуть pos самого листа (default) | [JSDCoffeeLeavesBlock.java L46-50](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/JSDCoffeeLeavesBlock.java#L46-L50) | Minor — если stemPos не установлен, fallback к самому листу; stem lookup `getBlockEntity(stemPos)` вернёт null для static branch → totalAgeTicks = leaf ticks |
| N3 | `TeaPileBlockEntity` — `storedItem` инициализирован как `ItemStack.EMPTY` | OK | |
| N4 | **[MINOR] `DRINKING_FLUID` ThreadLocal не очищается при логауте** | [JSDEvents.java L83](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/event/JSDEvents.java#L83) | ThreadLocal может утекать. Очистка происходит в `onFinishDrinking` (L115), но если drinking прерывается не через Finish — утечка. Нужен `Cancel` handler |
| N5 | **[MINOR] `TeaBushBlock.transformToTree()` не удаляет верхнюю часть куста перед размещением growing_branch** | [TeaBushBlockEntity.java L57-63](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/entity/TeaBushBlockEntity.java#L57-L63) | LOWER меняется на growing_branch (L58), потом UPPER проверяется. Но `setBlockAndUpdate` на L58 может вызвать `updateShape` на UPPER → UPPER видит что LOWER больше не `TeaBushBlock` → `updateShape` вернёт AIR (L117). Это race condition зависит от порядка обновлений — **потенциальный баг**, но на практике `setBlockAndUpdate` триггерит neighbor update синхронно |
| N6 | **[MAJOR] `TeaBushBlock` — `BerryBushBlockEntity.resetPickedTick(level, pos)` не вызывается правильно** | [TeaBushBlock.java L171-174](file:///d:/GanjaCraft/git/journeyTFC/jsdrinks/src/main/java/com/journey/jsdrinks/block/TeaBushBlock.java#L171-L174) | Используется `bushBE.resetLastPickedCounter()` напрямую через instanceof — это OK. Но выше на L177 `level.setBlockAndUpdate(lowerPos, ...)` **после** сброса picked tick — это перезапишет block state, но BE останется тем же (minecraft не удаляет BE при setValue). OK |

### 4.4 NBT / Сериализация

| # | Проблема | Критичность |
|---|---------|-------------|
| B1 | `TeaBushBlockEntity` — не вызывает `super.loadAdditional()` / `super.saveAdditional()` **перед** своими полями | Minor — в TFC `BerryBushBlockEntity` читает `growthsRemaining` в super. Порядок: super первый в loadAdditional (L67) → нормально, потому что TFC в `BerryBushBlockEntity.loadAdditional` читает `growthsRemaining` и вызывает `super` (TickingPlantBE → TickCounterBE → TFCBlockEntity). Всё в правильном порядке |
| B2 | `TeaPileBlockEntity` — `storedItem` сохраняется через `ItemStack.save(provider)` / `ItemStack.parseOptional(provider, ...)` — стандартный паттерн 1.21.1 | OK |

---

## ЭТАП 5: СВОДНАЯ ТАБЛИЦА ЗАМЕЧАНИЙ

| # | Критичность | Локация | Проблема |
|---|------------|---------|----------|
| **1** | **Blocker** | `TeaBushBlock` → `super.setPlacedBy()` → `BerryBushBlockEntity.reset()` | Статический метод TFC ищет BE через `TFCBlockEntities.BERRY_BUSH`, а не `JSDBlockEntities.TEA_BUSH`. Tick counter не сбрасывается при размещении. Жизненный цикл сезонов может быть сломан |
| **2** | **Blocker** | `GrowingFruitTreeBranchBlock.randomTick()` → `TickingPlantBlockEntity.reset()` | Родительский static метод ищет `TFCBlockEntities.TICK_COUNTING_PLANT`. Аддоновский BE под `JSDBlockEntities.TICKING_PLANT` — не найдётся. Дерево не будет нормально расти |
| **3** | **Major** | `JSDCoffeeLeavesBlock.onUpdate()` L48-50 | `stemPos` BE — `FruitTreeBranchBlock` не имеет BE. Проверка возраста 2 лет ненадёжна для зрелых деревьев |
| **4** | **Major** | `TeaBushBlockEntity.checkYearlyGrowth()` | Нет проверки dormant/климата. Куст в неподходящем климате превращается в дерево |
| **5** | **Major** | `TeaBushBlockEntity` L50 | Порог `>= 2` зашит в код. ТЗ требует конфиг |
| **6** | **Major** | `JSDEvents.onPlayerTick()` | 245 BE lookup каждые 30 тиков на игрока. TPS impact при множестве игроков |
| **7** | **Major** | `coffee_milk_500.json` / barrel recipes | Выход 600 mB вместо ожидаемых 500 mB. Требуется уточнение ТЗ |
| **8** | **Minor** | `JSDEvents` ThreadLocal | `DRINKING_FLUID` не очищается при прерывании питья |
| **9** | **Minor** | `JSDClientEvents` L42-49 | `ItemBlockRenderTypes.setRenderLayer` — deprecated API, должен использовать render type extensions |
| **10** | **Minor** | Несколько классов | Бесполезные override `getLifecycleForCurrentMonth` с вызовом `super` |

---

## ГОТОВЫЕ ПАТЧИ

### Патч 1: Исправление BE lookup для `TeaBushBlock.setPlacedBy()`

**Проблема:** `super.setPlacedBy()` вызывает `BerryBushBlockEntity.reset()` → ищет `TFCBlockEntities.BERRY_BUSH`.

```diff
// TeaBushBlock.java L96-100
 @Override
 public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
     level.setBlockAndUpdate(pos.above(), defaultBlockState().setValue(PART, ITallPlant.Part.UPPER).setValue(LIFECYCLE, state.getValue(LIFECYCLE)));
-    super.setPlacedBy(level, pos, state, placer, stack);
+    // Don't call super.setPlacedBy because TFC's BerryBushBlockEntity.reset() looks up TFCBlockEntities.BERRY_BUSH
+    // which won't find our TeaBushBlockEntity registered under JSDBlockEntities.TEA_BUSH
+    if (level.getBlockEntity(pos) instanceof TeaBushBlockEntity bushBE) {
+        bushBE.resetCounter();
+    }
 }
```

### Патч 2: Исправление BE lookup для `JSDGrowingFruitTreeBranchBlock`

**Проблема:** `super.randomTick()` вызывает `TickingPlantBlockEntity.reset()` с TFC BE type.

```diff
// JSDGrowingFruitTreeBranchBlock.java
 @Override
 protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
     int hydration = getFruitBranchHydration(level, pos);
     float temp = Climate.getAverageTemperature(level, pos);
     if (!this.climateRange.get().checkBoth(hydration, temp, false) && !state.getValue(NATURAL)) {
         JSDTickingPlantBlockEntity.reset(level, pos);
+        // Skip super.randomTick() to avoid TFC's TickingPlantBlockEntity.reset() which uses wrong BE type
+        // Call super.super (FruitTreeBranchBlock) randomTick directly
+        return;
+    }
+    // Manually replicate GrowingFruitTreeBranchBlock.randomTick()'s else-branch
+    // to avoid TFC static methods that reference TFCBlockEntities
+    if (level.getBlockEntity(pos) instanceof JSDTickingPlantBlockEntity counter) {
+        int cycles = (int) (counter.getTicksSinceUpdate() / net.dries007.tfc.config.TFCConfig.SERVER.fruitBranchGrowthTicks.get());
+        if (cycles >= 1) {
+            counter.resetCounter();
+            grow(state, level, pos, rand, cycles);
+        }
     }
-    super.randomTick(state, level, pos, rand);
 }
```

> [!WARNING]
> Этот патч — **наиболее сложный**. `grow()` внутри TFC вызывает `placeGrownFlower()` → `counter.resetCounter()` через прямой instanceof, что работает. Но `placeGrownFlower` также вызывает `getStateForPlacement` и `addLeaves` — и `addLeaves` использует `BerryBushBlockEntity` instanceof, что полиморфно сработает. Однако `TickingPlantBlockEntity.reset()` и `.addTicks()` вызываются из `placeBody()` и `placeGrownFlower()` — **они тоже используют TFC BE type!**
>
> **Полное решение:** переопределить `grow()`, `placeGrownFlower()`, `placeBody()` с правильными BE lookups. Либо **зарегистрировать аддоновские BE также под TFC'шными типами** (но это невозможно из-за ограничений registry).
>
> **Рекомендуемый подход:** создать mixin или переопределить всю цепочку роста. Альтернатива — использовать `BlockEntityType.Builder` который принимает и TFC'шные блоки, и свои, зарегистрировав оба типа.

### Патч 3: Конфиг для порога куст→дерево

```java
// Новый файл: JSDConfig.java
package com.journey.jsdrinks;

import net.neoforged.neoforge.common.ModConfigSpec;

public class JSDConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue TEA_BUSH_TRANSFORM_YEARS;

    static {
        BUILDER.comment("Tea Bush to Tree transformation settings");
        TEA_BUSH_TRANSFORM_YEARS = BUILDER
            .comment("Number of TFC years without harvest before a tea bush transforms into a tree")
            .defineInRange("teaBushTransformYears", 2, 1, 100);

        SPEC = BUILDER.build();
    }
}
```

```diff
// TeaBushBlockEntity.java L50
-            if (seasonsWithoutHarvest >= 2) {
+            if (seasonsWithoutHarvest >= JSDConfig.TEA_BUSH_TRANSFORM_YEARS.get()) {
```

### Патч 4: Проверка dormant/климата в счётчике куста

```diff
// TeaBushBlockEntity.java - checkYearlyGrowth()
-    public void checkYearlyGrowth(Level level, BlockPos pos, Lifecycle currentLifecycle) {
+    public void checkYearlyGrowth(Level level, BlockPos pos, Lifecycle currentLifecycle, boolean climateValid) {
         if (level.isClientSide()) return;

+        // TZ: Dormant / no fruit season / no climate → year doesn't count
+        if (currentLifecycle == Lifecycle.DORMANT || !climateValid) {
+            lastRecordedYear = (int) Calendars.get(level).getCalendarYear();
+            return;
+        }
+
         long currentYear = Calendars.get(level).getCalendarYear();
```

```diff
// TeaBushBlock.java L140-142
-                bushBE.checkYearlyGrowth(level, pos, state.getValue(LIFECYCLE));
+                boolean climateValid = JSDClimateRanges.TEA_BUSH.get().checkBoth(
+                    SeasonalPlantBlock.getFruitBushHydrationFromRootPos(level, pos.below()),
+                    Climate.getAverageTemperature(level, pos), false);
+                bushBE.checkYearlyGrowth(level, pos, state.getValue(LIFECYCLE), climateValid);
```

### Патч 5: Оптимизация TPS — кэширование звука обжарки

```diff
// JSDEvents.java L53-54
-        if (level.getGameTime() % 30 != 0) return;
+        if (level.getGameTime() % 60 != 0) return;  // Reduce from 30 to 60 ticks (~3s)

// Reduce search radius from 3 to 2
-        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-3, -2, -3), playerPos.offset(3, 2, 3))) {
+        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-2, -1, -2), playerPos.offset(2, 1, 2))) {
```

### Патч 6: ThreadLocal cleanup

```diff
// JSDEvents.java — add Cancel handler
+    @SubscribeEvent
+    public static void onCancelDrinking(LivingEntityUseItemEvent.Stop event) {
+        DRINKING_FLUID.remove();
+    }
```
