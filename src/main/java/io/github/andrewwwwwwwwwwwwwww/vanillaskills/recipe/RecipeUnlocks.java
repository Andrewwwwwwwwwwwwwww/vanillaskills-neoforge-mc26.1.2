package io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.CraftingGate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Gates our data-driven recipes in the recipe book behind the skills that unlock the matching tier.
 *
 * <p>These used to be handed out unconditionally on join, which meant a brand-new player opened the book and
 * found Rose Gold already sitting there — the whole progression spoiled on the first login, and no signal
 * that unlocking the skill had done anything.
 *
 * <p>Only <b>discovery</b> is gated here. Whether the item can actually be crafted is
 * {@link CraftingGate}'s job, and its rule is unchanged: raw ingots were never skill-locked, only finished
 * gear. So this hides the entry until the tier means something to you, without altering what you may craft.
 *
 * <p>Recipes are removed again as well as granted, so {@code /skill reset} genuinely resets the book instead
 * of leaving entries behind from a refunded skill.
 */
public final class RecipeUnlocks {
    private RecipeUnlocks() {}

    /** A data recipe and the skill flag that reveals it. */
    private record Gated(ResourceKey<Recipe<?>> recipe, String flagA, String flagB) {}

    private static final List<Gated> GATED = List.of(
            // Either the armour or the tool skill for the tier is enough to have "found" the material.
            new Gated(key("rose_gold_ingot"), "craft_armor_rose_gold", "craft_tool_rose_gold"));

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("vanillaskills", path));
    }

    /**
     * Bring a player's recipe book in line with the skills they currently hold.
     *
     * <p>Safe to call often — awarding a recipe the player already knows, or removing one they never had,
     * are both no-ops in vanilla.
     */
    public static void sync(ServerPlayer player) {
        if (player.level().getServer() == null) return;

        List<ResourceKey<Recipe<?>>> toAward = new ArrayList<>();
        List<RecipeHolder<?>> toRemove = new ArrayList<>();

        for (Gated gated : GATED) {
            boolean unlocked = CraftingGate.hasFlag(player, gated.flagA())
                    || CraftingGate.hasFlag(player, gated.flagB());
            if (unlocked) {
                toAward.add(gated.recipe());
            } else {
                player.level().getServer().getRecipeManager().byKey(gated.recipe()).ifPresent(toRemove::add);
            }
        }

        if (!toAward.isEmpty()) player.awardRecipesByKey(toAward);
        if (!toRemove.isEmpty()) player.resetRecipes(toRemove);
    }
}
