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

    /**
     * Reveal any of our recipes whose ingredients the player is carrying.
     *
     * <p>This is vanilla's own discovery rule — you learn a recipe by finding something it uses — applied to
     * every recipe the mod adds, rather than to a hand-written list. Nothing was unlocking most of them, so
     * they simply never appeared in the book however many shards or ingots you were holding.
     *
     * <p>Ingredients come from each recipe's own {@code RecipeDisplay}, the same source
     * {@link ComponentAutofill} fills the grid from. That matters twice over: it needs no per-recipe table
     * so a new recipe is covered the moment it exists, and it compares real {@code ItemStack}s, so a marked
     * Steel Ingot is recognised while a plain iron ingot is not.
     */
    private static void awardForHeldIngredients(ServerPlayer player,
                                                List<ResourceKey<Recipe<?>>> toAward) {
        var server = player.level().getServer();
        if (server == null) return;
        var context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(player.level());

        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (!holder.id().identifier().getNamespace().equals("vanillaskills")) continue;
            if (holdsAnyIngredient(player, holder, context)) toAward.add(holder.id());
        }
    }

    /** True if the player carries at least one stack any of this recipe's displays asks for. */
    private static boolean holdsAnyIngredient(ServerPlayer player, RecipeHolder<?> holder,
                                              net.minecraft.util.context.ContextMap context) {
        for (var display : holder.value().display()) {
            for (var slot : ingredientsOf(display)) {
                for (net.minecraft.world.item.ItemStack want : slot.resolveForStacks(context)) {
                    if (want.isEmpty()) continue;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        net.minecraft.world.item.ItemStack have = player.getInventory().getItem(i);
                        if (!have.isEmpty()
                                && net.minecraft.world.item.ItemStack.isSameItemSameComponents(have, want)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** The ingredient slots of a display, for the shapes that have them. */
    private static java.util.List<net.minecraft.world.item.crafting.display.SlotDisplay> ingredientsOf(
            net.minecraft.world.item.crafting.display.RecipeDisplay display) {
        if (display instanceof net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay shaped) {
            return shaped.ingredients();
        }
        if (display instanceof net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay shapeless) {
            return shapeless.ingredients();
        }
        if (display instanceof net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay furnace) {
            return java.util.List.of(furnace.ingredient());
        }
        return java.util.List.of();
    }

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

        awardForHeldIngredients(player, toAward);

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
