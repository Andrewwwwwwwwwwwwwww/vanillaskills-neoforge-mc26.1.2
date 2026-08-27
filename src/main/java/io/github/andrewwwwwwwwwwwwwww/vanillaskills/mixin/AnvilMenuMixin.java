package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Alloys;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The anvil behaviours for VanillaSkills:
 *
 * <p><b>Skill Shards replace experience levels.</b> With experience removed from the game, the anvil is
 * paid in Skill Shards instead — otherwise every operation would be permanently unaffordable, since the
 * player's level is always zero.
 *
 * <p><b>And the price is by materials, not by vanilla's formula.</b> Vanilla's cost curve assumes
 * experience, which regrows; Skill Shards do not, so charging it 1:1 billed a repeatable sink to a finite
 * budget — and the prior-work penalty made an item progressively unrepairable. Instead the cost is what the
 * operation consumes: one shard per repair material, one per enchantment level on the sacrificed item, and
 * a configurable flat fee for a plain rename. See {@code vanillaskills$repriceByMaterials}, and
 * {@code anvilMaterialPricing} to restore vanilla's numbers.
 *
 * <p><b>Steel Shield forging.</b> Put a plain shield in one input slot and a Steel Ingot in the other and it
 * produces a Steel-Infused Shield — the only way to make one, replacing the old crafting-table recipe.
 * Free ({@link #STEEL_FORGE_COST}); {@code mayPickup} is overridden so a zero-cost result can still be
 * taken. Consumes exactly one shield and one ingot per take, so a stack of ingots forges one at a time.
 *
 * <p>Steel Ingots themselves are no longer made here — 2.0 moved them to a furnace, where one iron block
 * smelts into three, which is what freed the anvil for the shield.
 *
 * <p><b>Over-level enchantments.</b> The anvil clamps every enchantment to its max_level. Since
 * VanillaSkills keeps Fortune's max_level at 3 but mints Fortune IV/V directly, the anvil would knock
 * those back to III. After the vanilla result is computed, this un-clamps: for any enchantment already
 * present on the result, if either input carries a higher level, the result is raised to it.
 *
 * <p>Input/result slot fields live in the superclass {@code ItemCombinerMenu}, so they're read via the
 * inherited {@code getSlot}; {@code cost}/{@code repairItemCountCost} are on {@code AnvilMenu} and are
 * shadowed directly.
 */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {

    private static final int STEEL_FORGE_COST = 0; // free — mayPickup is overridden so a 0-cost result can still be taken

    @Shadow private int repairItemCountCost;
    @Shadow @org.spongepowered.asm.mixin.Final private DataSlot cost;

    /** A plain vanilla shield — not one that is already Steel-Infused. */
    private static boolean vanillaskills$isPlainShield(ItemStack stack) {
        return stack.is(Items.SHIELD) && !Markers.isOurs(stack);
    }

    /** True when the inputs are the Steel Shield forge: a plain shield plus a Steel Ingot. */
    private static boolean vanillaskills$isShieldForge(AbstractContainerMenu self) {
        return vanillaskills$isPlainShield(self.getSlot(AnvilMenu.INPUT_SLOT).getItem())
                && Alloys.isSteelIngot(self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem());
    }

    /**
     * Refuses to apply an enchanted book to a tool or armor piece — that is the Infusing Table's job now.
     *
     * <p>Two books combining with each other is left alone: that is still priced in Skill Shards by the
     * generic {@code mayPickup}/{@code onTake} hooks below, exactly like every other anvil operation once
     * experience is removed, so nothing extra was needed to make book+book "cost Skill Shards" — it already
     * did. What needed blocking was specifically an enchanted book landing on a non-book item, which is the
     * one path the Infusing Table was built to replace.
     *
     * <p>Runs before vanilla computes anything, so a blocked combination shows no result at all rather than
     * a result that then fails a later check.
     */
    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$blockBookOnItem(CallbackInfo ci) {
        if (io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.ANVIL_BOOKS_ON_ITEMS) return;
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack left = self.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        if (!left.isEmpty() && !left.is(Items.ENCHANTED_BOOK) && right.is(Items.ENCHANTED_BOOK)) {
            self.getSlot(AnvilMenu.RESULT_SLOT).set(ItemStack.EMPTY);
            this.cost.set(0);
            ci.cancel();
        }
    }

    /**
     * A plain shield + a Steel Ingot forges a Steel-Infused Shield.
     *
     * <p>2.0 moved the Steel Ingot itself out of the anvil and into a furnace (one iron block smelts into
     * three), which freed the anvil to become where the shield is made — the thematically right place, and
     * it replaces the old six-ingot crafting-table recipe.
     */
    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$forgeSteelShield(CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (vanillaskills$isShieldForge(self)) {
            self.getSlot(AnvilMenu.RESULT_SLOT).set(
                    io.github.andrewwwwwwwwwwwwwww.vanillaskills.shield.SteelShield.create());
            this.repairItemCountCost = 1;
            this.cost.set(STEEL_FORGE_COST);
            ci.cancel();
        }
    }

    /**
     * Decides whether the result can be taken. Two overrides, in one place so their order is explicit
     * rather than depending on mixin callback ordering:
     *
     * <ol>
     *   <li>Steel forging costs 0, and vanilla refuses to hand over a zero-cost result.</li>
     *   <li>With experience removed, {@code player.experienceLevel} is permanently 0, so vanilla's
     *       affordability test would block <em>every</em> anvil operation forever. The cost the anvil
     *       computed is instead read as a price in <b>Skill Shards</b>.</li>
     * </ol>
     *
     * <p>⚠ A vanilla client still renders the result greyed out with its red cost label, because it
     * runs this check locally against its own (always-zero) level count. The take itself is
     * server-authoritative and succeeds. This is the same cosmetic-only mismatch as the pre-existing
     * "Too Expensive" label, and is equally unfixable from the server side.
     */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$mayPickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (vanillaskills$isShieldForge(self)) {
            cir.setReturnValue(true); // free shield forge
            return;
        }
        if (io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.EXPERIENCE_ENABLED) return;

        int price = this.cost.get();
        if (price <= 0) {
            // Our pricing can make an operation legitimately free — a plain rename is, by default.
            // Vanilla reads cost 0 as "no operation here" and refuses the pickup, which left a free
            // rename showing a result that could not be taken. A stack sitting in the result slot IS
            // the proof of a valid operation, so a free one is simply allowed.
            if (hasStack) cir.setReturnValue(true);
            return;
        }
        if (player.hasInfiniteMaterials()) {
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(player instanceof net.minecraft.server.level.ServerPlayer sp
                && io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills.PLAYERS.skillShards(sp) >= price);
    }

    /**
     * Charges the anvil's cost in Skill Shards when the result is taken.
     *
     * <p>Vanilla pays with {@code giveExperienceLevels(-cost)}, which the experience mixins now turn
     * into a no-op — so without this the whole anvil would be free. Runs before vanilla's own handling
     * and before the steel-forge take below, which is harmless: steel costs 0, so nothing is charged
     * for it regardless of which callback the mixin runs first.
     */
    @Inject(method = "onTake", at = @At("HEAD"))
    private void vanillaskills$chargeSkillShards(Player player, ItemStack stack, CallbackInfo ci) {
        if (io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.EXPERIENCE_ENABLED) return;
        if (player.hasInfiniteMaterials()) return;
        int price = this.cost.get();
        if (price <= 0) return;
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills.PLAYERS.spendSkillShards(sp, price);
        }
    }

    /** Forging the shield consumes one shield and one ingot (vanilla would clear the whole left slot). */
    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$takeSteelShield(Player player, ItemStack stack, CallbackInfo ci) {
        // Keyed off the inputs rather than the taken stack, which proved unreliable.
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (!vanillaskills$isShieldForge(self)) return; // not our recipe — let vanilla handle it
        ItemStack left = self.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        // No payment here: the forge is free, and the Skill Shard charge above already handles every
        // priced operation. (This used to call giveExperienceLevels, which 2.0 made a no-op anyway.)
        left.shrink(1);
        right.shrink(1);
        self.getSlot(AnvilMenu.INPUT_SLOT).set(left);
        self.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(right);
        this.cost.set(0);
        ((AnvilMenu) (Object) this).createResult(); // refresh — forge another if iron remains
        self.broadcastChanges();
        ci.cancel();
    }

    /**
     * Removes the anvil's "Too Expensive" 40-level cap for EVERY operation. Costs keep scaling
     * exactly as vanilla (prior-work penalty untouched) — they just never block the result, however
     * high they climb. Covers both the result-emptying check and the rename 39-clamp. The constant
     * lives in {@code createResult} on Fabric and NeoForge's split-out {@code createResultInternal};
     * require = 1 lets the same mixin serve both editions. (The Dragon flat repair below is separate
     * and unaffected — it cancels before vanilla pricing runs.)
     */
    @ModifyConstant(method = {"createResult", "createResultInternal"},
            constant = @Constant(intValue = 40), require = 1)
    private int vanillaskills$uncapTooExpensive(int cap) {
        // Config can restore the vanilla cap; default keeps it removed (costs still scale, never block).
        return io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.ANVIL_TOO_EXPENSIVE_CAP
                ? cap : Integer.MAX_VALUE;
    }

    /**
     * Dragon gear + <b>one Dragon Scale</b> = a FULL repair for a flat Skill Shard cost — no prior-work
     * scaling, no "Too Expensive" cap.
     *
     * <p>A Dragon Ingot also works, but nobody should ever use one: an Ingot is 4 Scales plus a Netherite
     * Ingot, so repairing with one cost a full crafting chain and a netherite bar <i>every time the armour
     * got scratched</i>. Since the dragon only drops 8 Scales per kill (32 on the world's first), that made
     * ordinary upkeep depend on re-killing a boss. One Scale for a full repair keeps Scales as the dragon-tier
     * currency without the repair loop eating the tier's own crafting material.
     */
    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$dragonFlatRepair(CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack left = self.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        boolean repairMaterial =
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonScale.isDragonScale(right)
                        || io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonIngot.isDragonIngot(right);
        if (!repairMaterial) return;
        boolean dragonGear = Markers.has(left, io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers.DRAGON.markerKey)
                || io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTiers.DRAGON.isWorn(left);
        if (!dragonGear || !left.isDamaged()) return;
        ItemStack result = left.copy();
        result.setDamageValue(0);
        self.getSlot(AnvilMenu.RESULT_SLOT).set(result);
        this.repairItemCountCost = 1; // exactly one ingot per repair, however big the stack
        this.cost.set(io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.DRAGON_REPAIR_COST);
        ci.cancel();
    }

    /**
     * Reprice the anvil by what the operation consumes, replacing vanilla's level formula.
     *
     * <p>Vanilla's cost is designed around experience, which regrows. Skill Shards do not: an advancement
     * pays once and the world holds a fixed number of them, so charging vanilla's levels 1:1 bills a
     * repeatable sink to a finite budget. A single late-game combine could cost 39 of roughly 2,100, and the
     * prior-work penalty doubles on every visit until an item is effectively unrepairable.
     *
     * <p>So the price becomes what you put in: one shard per repair material consumed, and one per
     * enchantment level on the sacrificed item. Both rates are configurable, as is a flat rename fee.
     *
     * <p>Runs at TAIL so vanilla has already decided <i>whether</i> there is a result and how many repair
     * materials it wants — this only overwrites the number, never the outcome. Its order against the other
     * TAIL injector here does not matter: that one rewrites the result stack's enchantments, while this reads
     * the sacrifice slot's and writes only {@code cost}.
     *
     * <p>Leaves a zero cost alone: the steel forge and the free cases set it deliberately.
     */
    @Inject(method = "createResult", at = @At("TAIL"))
    private void vanillaskills$repriceByMaterials(CallbackInfo ci) {
        if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.ANVIL_MATERIAL_PRICING) return;
        if (io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.EXPERIENCE_ENABLED) return;

        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack result = self.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        if (result.isEmpty()) return;
        if (this.cost.get() <= 0) return; // a deliberate freebie (steel forge, Dragon repair path)

        int price = this.repairItemCountCost
                * io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.ANVIL_REPAIR_PER_MATERIAL;

        // Enchantments come from the right-hand item, so its levels are what the merge is charging for.
        ItemStack sacrifice = self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        price += vanillaskills$enchantmentLevels(sacrifice)
                * io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.ANVIL_ENCHANT_PER_LEVEL;

        // A same-item combine that restores durability consumed a whole item to do it, and that transfer
        // was previously free whenever the sacrifice carried no enchantments — so repairing by feeding an
        // enchanted rod a plain spare cost nothing, while the mirrored arrangement charged per level.
        // Material repairs are excluded (repairItemCountCost prices those per unit above).
        ItemStack left = self.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        if (this.repairItemCountCost <= 0 && !sacrifice.isEmpty()
                && result.isDamageableItem() && result.getDamageValue() < left.getDamageValue()) {
            price += io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.ANVIL_COMBINE_REPAIR_COST;
        }

        if (price <= 0) {
            // Nothing consumed and nothing merged: this is a rename.
            price = io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.ANVIL_RENAME_COST;
        }
        this.cost.set(Math.max(0, price));
    }

    /** Total enchantment levels on a stack, counting stored book enchantments as well as applied ones. */
    private static int vanillaskills$enchantmentLevels(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        ItemEnchantments ench = stack.get(enchantmentsType(stack));
        if (ench == null || ench.isEmpty()) return 0;
        int total = 0;
        for (var entry : ench.entrySet()) total += entry.getIntValue();
        return total;
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void vanillaskills$preserveOverLevelEnchantments(CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack result = self.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        if (result.isEmpty()) return;

        DataComponentType<ItemEnchantments> resultType = enchantmentsType(result);
        ItemEnchantments resultEnch = result.get(resultType);
        if (resultEnch == null || resultEnch.isEmpty()) return;

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(resultEnch);
        boolean changed = false;
        for (int slot : new int[]{AnvilMenu.INPUT_SLOT, AnvilMenu.ADDITIONAL_SLOT}) {
            ItemStack input = self.getSlot(slot).getItem();
            if (input.isEmpty()) continue;
            ItemEnchantments inputEnch = input.get(enchantmentsType(input));
            if (inputEnch == null || inputEnch.isEmpty()) continue;
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : inputEnch.entrySet()) {
                Holder<Enchantment> key = entry.getKey();
                int inputLevel = entry.getIntValue();
                // Only lift enchantments the result already has (un-clamp, never add new).
                if (mutable.getLevel(key) > 0 && inputLevel > mutable.getLevel(key)) {
                    mutable.set(key, inputLevel);
                    changed = true;
                }
            }
        }
        if (changed) {
            result.set(resultType, mutable.toImmutable());
        }
    }

    private static DataComponentType<ItemEnchantments> enchantmentsType(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
    }
}
