package io.github.andrewwwwwwwwwwwwwww.vanillaskills.creative;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Alloys;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorPiece;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTier;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTiers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.FortuneTemplate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A creative-inventory tab holding all of the mod's custom items (alloys, armor, the Fortune
 * template). Note: the creative menu is built client-side, so this tab only appears for players
 * who have the mod on their client (singleplayer, or anyone who installs it) — on a dedicated
 * server with vanilla clients it won't show, but the items are still obtainable by crafting.
 */
public final class VanillaSkillsItemGroup {
    private VanillaSkillsItemGroup() {}

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "vanillaskills");

    static {
        TABS.register("items", () -> CreativeModeTab.builder()
                .title(Component.translatableWithFallback("vanillaskills.itemgroup", "VanillaSkills"))
                .icon(io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.DragonUpgradeTemplate::create)
                .displayItems((params, output) -> {
                    output.accept(Alloys.roseGoldIngot());
                    output.accept(Alloys.steelIngot());
                    output.accept(Alloys.crystallizedDiamond());
                    output.accept(io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonScale.create());
                    output.accept(io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonIngot.create());
                    output.accept(io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.DragonUpgradeTemplate.create());
                    output.accept(FortuneTemplate.create());
                    output.accept(io.github.andrewwwwwwwwwwwwwww.vanillaskills.shield.SteelShield.create(params.holders()));
                    for (ArmorTier tier : ArmorTiers.TIERS) {
                        for (ArmorPiece piece : ArmorPiece.values()) {
                            output.accept(tier.create(piece));
                        }
                    }
                    for (io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTier tier
                            : io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers.TIERS) {
                        for (io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolKind kind
                                : io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolKind.values()) {
                            output.accept(tier.create(kind));
                        }
                    }
                })
                .build());
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
