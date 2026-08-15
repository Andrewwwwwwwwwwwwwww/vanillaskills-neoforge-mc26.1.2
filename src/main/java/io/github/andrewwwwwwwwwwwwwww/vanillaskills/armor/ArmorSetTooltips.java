package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Live "what you're missing" set-bonus tooltips for every tier with a full-set bonus. While a piece is
 * worn, its lore becomes a checklist (count/4, +/- each slot, and whether the bonus is active);
 * pieces sitting in the inventory are reset to the static description. Modelled on the original Rose
 * Gold behaviour, generalised so all set tiers behave identically.
 */
public final class ArmorSetTooltips {
    private ArmorSetTooltips() {}

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] SLOT_NAMES = {"Helmet", "Chestplate", "Leggings", "Boots"};

    /** Display config for one set tier's live checklist. */
    private record SetDef(ArmorTier tier, String name, ChatFormatting color,
                          String activeLine, String hintLine, Supplier<ItemLore> baseLore) {}

    private static final List<SetDef> SETS = List.of(
            new SetDef(ArmorTiers.ROSE_GOLD, "Rose Gold Set", ChatFormatting.AQUA,
                    "Immune to negative effects", "Wear all 4 for immunity", RoseGoldSet::baseLore),
            new SetDef(ArmorTiers.CRYSTAL, "Crystalline Set", ChatFormatting.LIGHT_PURPLE,
                    "Reflecting 25% of melee damage", "Wear all 4 to reflect damage", CrystalSet::baseLore),
            new SetDef(ArmorTiers.DRAGON, "Dragon Set", ChatFormatting.LIGHT_PURPLE,
                    "Fire immunity + dive-dash active", "Wear all 4 for fire immunity & dash", DragonSet::baseLore));

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (SetDef def : SETS) updateSet(player, def);
        }
    }

    private static void updateSet(ServerPlayer player, SetDef def) {
        ItemStack[] worn = new ItemStack[4];
        boolean[] present = new boolean[4];
        int count = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = player.getItemBySlot(ARMOR_SLOTS[i]);
            worn[i] = stack;
            if (def.tier().isWorn(stack)) {
                present[i] = true;
                count++;
            }
        }

        // Live checklist on each worn piece of this tier.
        if (count > 0) {
            ItemLore live = liveLore(def, present, count);
            for (int i = 0; i < 4; i++) {
                if (present[i]) ensureLore(worn[i], live);
            }
        }

        // Reset pieces of this tier sitting in the inventory (not worn) to the static description.
        ItemLore base = def.baseLore().get();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == worn[0] || stack == worn[1] || stack == worn[2] || stack == worn[3]) continue;
            if (def.tier().isWorn(stack)) ensureLore(stack, base);
        }
    }

    private static ItemLore liveLore(SetDef def, boolean[] present, int count) {
        // Lore is baked into the item, so it can't be translated per-player server-side — use
        // translation keys with English fallbacks and let the client render them (see Markers.name).
        String base = "vanillaskills.set." + def.tier().id;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(""));
        lines.add(tr(base + ".name", def.name() + " (%s/4)", def.color(), count));
        for (int i = 0; i < 4; i++) {
            // Keep the +/- marker out of the key so translators only see the piece word.
            Component piece = Component.translatableWithFallback(
                    "vanillaskills.gear.piece." + SLOT_NAMES[i].toLowerCase(), SLOT_NAMES[i]);
            lines.add(Component.literal(present[i] ? "+ " : "- ").append(piece)
                    .withStyle(present[i] ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                    .withStyle(s -> s.withItalic(false)));
        }
        lines.add(count == 4
                ? tr(base + ".active", def.activeLine(), ChatFormatting.GREEN)
                : tr(base + ".hint", def.hintLine(), ChatFormatting.GRAY));
        return new ItemLore(lines);
    }

    private static Component tr(String key, String fallback, ChatFormatting color, Object... args) {
        return Component.translatableWithFallback(key, fallback, args)
                .withStyle(color).withStyle(s -> s.withItalic(false));
    }

    private static void ensureLore(ItemStack stack, ItemLore desired) {
        ItemLore current = stack.get(net.minecraft.core.component.DataComponents.LORE);
        if (!desired.equals(current)) {
            stack.set(net.minecraft.core.component.DataComponents.LORE, desired);
        }
    }

    private static Component styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}
