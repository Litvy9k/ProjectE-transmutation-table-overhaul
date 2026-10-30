# TransTable Overhaul

**An AE2-style terminal for ProjectE's Transmutation Table, plus a crafting terminal version.**

Tired of flipping through pages of 12 items in the Transmutation Table? This mod replaces the ProjectE Transmutation Table and Transmutation Tablet screen with a scrollable, searchable, sortable terminal inspired by Applied Energistics 2.

## Features

- **Scrollable item grid** showing every item you have learned, with how many you can make with your current EMC in the corner. Items you can't afford are greyed out.
- **AE2-style controls**
  - Left click: take 1
  - Right click: take half a stack
  - Ctrl + click: take a full stack
  - Shift + click: put a full stack straight into your inventory
  - Click the grid while holding an item: learn it and burn it into EMC
- **Search syntax**: plain text matches names and item ids, `@mod`, `#tag`, `$tooltip`, `-term` to exclude, space = AND, `|` = OR. Right click the search box to clear it.
- **Toolbar settings** (saved automatically): sort by name / EMC / mod, ascending or descending, show all items or only affordable ones, small / medium / full-screen terminal size.
- **JEI and EMI support**: press R / U on grid items to see recipes, optional two-way search sync, and recipe viewer panels stay out of the way.
- **Works everywhere ProjectE's screen opens**: the Transmutation Table, the Transmutation Tablet, and the Curios slot keybind.
- All of ProjectE's slots are still there: Klein Star slots, the EMC limit (lock) slot, the burn slot and the unlearn slot.
- English and Simplified Chinese translations.

## Transmutation Crafting Table

Craft it from a Transmutation Table on top of a piece of Dark Matter. It works like the terminal above, plus a 3x3 crafting grid like an AE2 crafting terminal:

- **JEI "+" transfer**: learned ingredients go into the grid as **EMC placeholders** that are paid for with EMC on every craft; anything else comes from your inventory (Shift + "+" fills a whole stack).
- Every filled grid slot shows a small gem (EMC) or chest (inventory) marker. Inventory items that run out turn into an EMC placeholder if you have learned them, so you can keep crafting, just like an AE2 crafting terminal.
- **⇄ button**: switch between EMC and your inventory. The grid converts right away: placeholders are swapped for items from your inventory, or inventory items go back and become placeholders. **✕ button**: clear the grid.
- When the grid forms a recipe, the EMC paid per craft is shown next to the result (red if you can't afford it).
- Click the result to take it, shift-click to craft one stack. **Taking a crafted item that has an EMC value also learns it**, so it shows up in the terminal right away.
- When several recipes match the grid, a button next to the result lets you pick which one to craft.

## Requirements

- Minecraft 1.21.1 with NeoForge
- [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) 1.1.0 or newer
- Optional: JEI or EMI

Install it on **both the client and the server**. Every transmutation is checked on the server (knowledge, EMC, inventory space). If a server doesn't have the mod, the client falls back to ProjectE's original screen.

## FAQ

**Does this change any ProjectE mechanics?** No. It only replaces the screen, and costs and learning work exactly like ProjectE.

**Is this affiliated with ProjectE or Applied Energistics 2?** No. It is an independent add-on, and all textures are original.

---

# 转化终端

**把 ProjectE 转化桌变成 AE2 风格的终端。**

- 可滚动的物品网格，显示所有已学习物品和当前 EMC 能转化的数量，EMC 不够的物品会变灰。
- 左键取 1 个，右键取半组，Ctrl + 点击取一组，Shift + 点击直接放进背包。拿着物品点网格就学习并转化为 EMC。
- 搜索语法：`@模组`、`#标签`、`$提示文字`，`-` 排除，空格表示"且"，`|` 表示"或"。
- 可以按名称 / EMC / 模组排序，切换升序降序，选择只显示买得起的物品，调整终端大小。
- 支持 JEI 和 EMI：可以按 R / U 查配方，也可以同步搜索。
- 转化桌、转化平板、Curios 快捷键打开的都是新界面。
- 新方块**合成转化桌**（转化桌放在暗物质上合成）：
  - 带 3×3 合成格，支持 JEI 的"+"转移按钮。已学习的材料作为 EMC 占位放入，每次合成扣除 EMC；其余从背包取。⇄ 按钮切换来源，合成格里的材料会立即在 EMC 占位和背包实物之间转换。
  - 每个有物品的格子都显示宝石（EMC）或箱子（背包）标记。背包材料用完后，如果已学习，会留下 EMC 占位继续合成，和 AE2 合成终端一样。
  - 能组成配方时，合成格旁边显示每次合成消耗的 EMC（不够时显示为红色）。
  - 点击取出产物，Shift + 点击合成一组；取出有 EMC 值的产物时会同时学习它。
  - 有多个配方匹配时，结果槽旁边会出现切换配方按钮。
- 需要 ProjectE 1.1.0 及以上版本，**客户端和服务端都要安装**。
