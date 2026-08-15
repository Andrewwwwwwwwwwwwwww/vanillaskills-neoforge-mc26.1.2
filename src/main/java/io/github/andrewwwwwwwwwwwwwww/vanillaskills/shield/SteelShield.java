package io.github.andrewwwwwwwwwwwwwww.vanillaskills.shield;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.world.item.enchantment.Repairable;

import java.util.List;

/**
 * A vanilla shield infused with steel: a marked shield with much greater durability and a "thorns"
 * effect (handled by ShieldThornsMixin) that hurts melee attackers when their hit is blocked. The
 * base shield's blocking behaviour (BLOCKS_ATTACKS component) is inherited, so it still blocks.
 */
public final class SteelShield {
    private SteelShield() {}

    public static final String MARKER = "vs_steel_shield";
    /** Durability. Live from gameplay.json ({@code steelShieldDurability}); a vanilla shield is 336. */
    public static int durability() {
        return io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.STEEL_SHIELD_DURABILITY;
    }
    /** Movement penalty while the shield is held, as a fraction of base speed. The steel that buys the
     *  durability is heavy — carrying it should cost something even when not actively blocking. */
    private static double heldSlowdown() {
        return io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.STEEL_SHIELD_SLOWDOWN;
    }
    /** Damage dealt back to a melee attacker while the shield is held. */
    public static float thornsDamage() {
        return io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.STEEL_SHIELD_THORNS;
    }
    private static final int COLOR = 0xB8C0C8;

    /** Server-side convenience: uses the running server's registries (e.g. the crafting recipe). */
    public static ItemStack create() {
        return create(VanillaSkills.server != null ? VanillaSkills.server.registryAccess() : null);
    }

    /**
     * Builds a Steel-Infused Shield, pulling the {@code vanillaskills:steel} banner pattern from the
     * given registries. Callers must pass a valid provider — on a client connected to a remote server
     * (e.g. the creative-menu builder) {@code VanillaSkills.server} is null, so the creative tab passes
     * {@code params.holders()} instead, otherwise the shield would be created with no pattern (plain wood).
     */
    public static ItemStack create(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(Items.SHIELD);
        stack.set(DataComponents.ITEM_NAME, Markers.name("vanillaskills.item.steel_shield", "Steel-Infused Shield", COLOR));
        Markers.applyMarker(stack, MARKER);
        // Render as a steel shield via a custom banner pattern: the vanilla shield renderer draws
        // bannered shields in full 3D everywhere (inventory/held/blocking), and plain shields with
        // no banner stay wooden — so both shields look right. Texture = vanillaskills:steel pattern.
        if (registries != null) {
            HolderGetter<BannerPattern> patterns = registries.lookupOrThrow(Registries.BANNER_PATTERN);
            ResourceKey<BannerPattern> steel = ResourceKey.create(Registries.BANNER_PATTERN,
                    Identifier.fromNamespaceAndPath("vanillaskills", "steel"));
            stack.set(DataComponents.BASE_COLOR, DyeColor.LIGHT_GRAY);
            stack.set(DataComponents.BANNER_PATTERNS,
                    new BannerPatternLayers.Builder().addIfRegistered(patterns, steel, DyeColor.WHITE).build());
            // The banner pattern only exists to render the shield as steel — hide its tooltip line
            // (otherwise the tooltip shows the raw "block.vanillaskills.banner.steel.white" key).
            stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT
                    .withHidden(DataComponents.BANNER_PATTERNS, true)
                    .withHidden(DataComponents.BASE_COLOR, true));
        }
        stack.set(DataComponents.MAX_DAMAGE, durability());
        stack.set(DataComponents.REPAIRABLE, new Repairable(repairItems()));
        // Slows the wielder while held in either hand. Applied as an item attribute modifier rather than
        // ticked, so it appears and disappears exactly with the item and needs no bookkeeping.
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                Identifier.fromNamespaceAndPath("vanillaskills", "steel_shield.weight"),
                                heldSlowdown(),
                                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        net.minecraft.world.entity.EquipmentSlotGroup.HAND)
                .build());
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                line("vanillaskills.item.steel_shield.desc1", "Hardened with steel for great durability.", ChatFormatting.GRAY),
                line("vanillaskills.item.steel_shield.desc2", "Blocking a melee hit injures the attacker.", ChatFormatting.GRAY),
                line("vanillaskills.item.steel_shield.desc3", "Heavy — slows you while held.", ChatFormatting.GRAY))));
        return stack;
    }

    public static boolean isSteelShield(ItemStack stack) {
        return stack.is(Items.SHIELD) && Markers.has(stack, MARKER);
    }

    private static HolderSet<Item> repairItems() {
        return HolderSet.direct(List.<Holder<Item>>of(Items.IRON_INGOT.builtInRegistryHolder()));
    }

    /** A translatable lore line: the client renders {@code key} from its language, falling back to the
     *  English {@code fallback} (item lore is baked into the stack, so it's resolved client-side). */
    private static Component line(String key, String fallback, ChatFormatting color) {
        return Component.translatableWithFallback(key, fallback).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}
