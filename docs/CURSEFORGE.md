# Transmutation Terminal

**An AE2-style terminal for ProjectE's Transmutation Table.**

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
- 需要 ProjectE 1.1.0 及以上版本，**客户端和服务端都要安装**。
