package io.github.andrewwwwwwwwwwwwwww.vanillaskills.command;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Alloys;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorPiece;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTier;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTiers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonIngot;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonScale;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.crate.Crates;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.data.CrateDef;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.data.VsContent;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.DragonUpgradeTemplate;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.FortuneTemplate;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.shield.SteelShield;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolKind;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTier;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Every item VanillaSkills adds, by id, for {@code /skill give}.
 *
 * <p>None of these are registered items — they are vanilla items carrying our markers and components — so
 * {@code /give} cannot produce them and never will. Without this there is no way to obtain most of the mod's
 * content except by playing all the way to it, which makes testing anything in the back half of the game
 * impractical.
 *
 * <p>The map is rebuilt on each call because crates are datapack-defined and can change on {@code /reload}.
 * It is small (about 80 entries) and only touched by an operator command, so rebuilding costs nothing.
 */
public final class ModItems {
    private ModItems() {}

    /** id -> factory, in a sensible browsing order. */
    public static Map<String, Supplier<ItemStack>> all() {
        Map<String, Supplier<ItemStack>> out = new LinkedHashMap<>();

        // Skill Shards
        out.put("unstable_skill_shard", ShardItems::unstableShard);
        out.put("unstable_skill_shard_block", ShardItems::unstableBlock);
        out.put("stable_skill_shard_block", ShardItems::stableBlock);

        // Alloys and materials
        out.put("rose_gold_ingot", Alloys::roseGoldIngot);
        out.put("steel_ingot", Alloys::steelIngot);
        out.put("crystallized_diamond", Alloys::crystallizedDiamond);
        out.put("dragon_scale", DragonScale::create);
        out.put("dragon_ingot", DragonIngot::create);

        // Templates and the shield
        out.put("fortune_template", FortuneTemplate::create);
        out.put("dragon_template", DragonUpgradeTemplate::create);
        out.put("steel_shield", SteelShield::create);

        // Gear: <tier>_<piece> / <tier>_<tool>
        for (ArmorTier tier : ArmorTiers.TIERS) {
            for (ArmorPiece piece : ArmorPiece.values()) {
                out.put(tier.id + "_" + piece.name().toLowerCase(Locale.ROOT), () -> tier.create(piece));
            }
        }
        for (ToolTier tier : ToolTiers.TIERS) {
            for (ToolKind kind : ToolKind.values()) {
                out.put(tier.id + "_" + kind.name().toLowerCase(Locale.ROOT), () -> tier.create(kind));
            }
        }

        // Crates — datapack-defined, so this list follows whatever the packs currently declare.
        for (CrateDef def : VsContent.crates()) {
            out.put("crate_" + def.id, () -> Crates.create(def));
        }

        // Enchanted books for the levels the mod mints but the game cannot roll. Without these there is no
        // way to obtain a Fortune IV/V book to shelve, so the Infusing Table's book-burning rule is
        // untestable and the Fortune upgrade path unreachable except by grinding the full recipe chain.
        out.put("fortune_4_book", () -> enchantedBook(net.minecraft.world.item.enchantment.Enchantments.FORTUNE, 4));
        out.put("fortune_5_book", () -> enchantedBook(net.minecraft.world.item.enchantment.Enchantments.FORTUNE, 5));
        out.put("unboxing_book", () -> enchantedBook(
                net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.ENCHANTMENT,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("vanillaskills", "unboxing")), 3));

        return out;
    }

    /**
     * An enchanted book holding one enchantment at an exact level.
     *
     * <p>Resolved against the server's live registry rather than a static holder, because enchantments are a
     * datapack registry — {@code vanillaskills:unboxing} only exists once the pack has loaded.
     */
    private static ItemStack enchantedBook(
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key, int level) {
        ItemStack book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
        var server = io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills.server;
        if (server == null) return book;
        var registry = server.registryAccess()
                .lookup(net.minecraft.core.registries.Registries.ENCHANTMENT).orElse(null);
        if (registry == null) return book;
        var holder = registry.get(key).orElse(null);
        if (holder == null) return book;
        net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(book,
                mutable -> mutable.set(holder, level));
        return book;
    }

    /** One stack by id, or null if the id is unknown. */
    public static ItemStack create(String id) {
        Supplier<ItemStack> factory = all().get(id);
        return factory == null ? null : factory.get();
    }
}
