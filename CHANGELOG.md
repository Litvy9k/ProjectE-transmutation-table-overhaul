# Changelog

## 1.1.0 - Minecraft 1.21.1 (NeoForge)

- New block: **Transmutation Crafting Table** (Transmutation Table on top of Dark Matter), an AE2 crafting terminal style
  terminal with a 3x3 crafting grid
  - JEI "+" recipe transfer: learned ingredients become EMC placeholders that are paid for on every craft, the rest
    comes from your inventory; ✕ button to clear the grid
  - ⇄ button switches the preferred source and converts the grid right away (placeholders ⇄ inventory items)
  - Each filled grid slot shows whether it uses EMC (gem) or inventory items (chest); inventory items that run out
    turn into an EMC placeholder if they are learned, like an AE2 crafting terminal
  - Shows the EMC paid per craft when the grid forms a recipe (red when you can't afford it)
  - Click the result to take it, shift-click to craft one stack; taking a result with an EMC value also learns it
- Mod renamed to **TransTable Overhaul** (shown in JEI, Jade and the mod list)

## 1.0.0 - Minecraft 1.21.1 (NeoForge)

First release.

- Replaces the ProjectE Transmutation Table / Tablet screen with an AE2-style terminal
- Scrollable grid of learned items with craftable counts
- Search syntax: `@mod`, `#tag`, `$tooltip`, `-exclude`, `|` for OR
- Sort by name / EMC / mod, view all or affordable items only, three terminal sizes
- JEI and EMI integration (recipe lookup, exclusion areas, optional search sync)
- English and Simplified Chinese
