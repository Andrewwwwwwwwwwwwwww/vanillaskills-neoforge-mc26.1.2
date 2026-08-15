package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Crystalline (Diamond II) set behaviour: while the full set is worn, a fraction of incoming melee
 * damage is reflected back at the attacker. Evaluated live per-hit by {@code CrystalReflectMixin}
 * (so removing a piece reverts it instantly). A small knockback resistance is baked per-piece in
 * {@link ArmorTiers#CRYSTAL} and is additive across the set.
 */
public final class CrystalSet {
    private CrystalSet() {}

    /** Fraction of incoming melee damage reflected to the attacker while the full set is worn.
     *  Live from gameplay.json ({@code crystalReflectFraction}); 0 turns the reflect off. */
    public static float reflectFraction() {
        return io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.CRYSTAL_REFLECT_FRACTION;
    }

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Refreshed well inside its own duration, so the effects never visibly flicker between ticks. */
    private static final int EFFECT_TICKS = 200;

    /**
     * Grants Strength and Resistance I while the full set is worn.
     *
     * <p>These are <b>in addition to</b> the melee reflect, not a replacement for it — Crystalline is the
     * defensive capstone below Dragon, so it keeps the thorns identity and gains staying power on top.
     *
     * <p>Applied ambient and hidden so they read as a property of the armour rather than potions, and
     * re-applied each pass so they lapse on their own shortly after a piece comes off.
     */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isFullSet(player)) continue;
            if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.CRYSTAL_SET_EFFECTS) continue;
            int amp = io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.CRYSTAL_SET_AMPLIFIER;
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.STRENGTH, EFFECT_TICKS, amp, true, false, false));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.RESISTANCE, EFFECT_TICKS, amp, true, false, false));
        }
    }

    /** True only while all four Crystalline pieces are worn (checked live, so it reverts instantly). */
    public static boolean isFullSet(LivingEntity entity) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!ArmorTiers.CRYSTAL.isWorn(entity.getItemBySlot(slot))) return false;
        }
        return true;
    }

    /** Static description shown on a crafted / stored Crystalline piece. */
    public static ItemLore baseLore() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(""));
        lines.add(trStyled("vanillaskills.set.crystal.name_plain", "Crystalline Set", ChatFormatting.LIGHT_PURPLE));
        lines.add(trStyled("vanillaskills.set.crystal.desc1", "Full set: reflect 25% of", ChatFormatting.GRAY));
        lines.add(trStyled("vanillaskills.set.crystal.desc2", "melee damage back at attackers", ChatFormatting.GRAY));
        lines.add(trStyled("vanillaskills.set.crystal.desc3", "plus Strength & Resistance I", ChatFormatting.GRAY));
        return new ItemLore(lines);
    }

    private static Component styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }

    /** Translatable, non-italic lore line — item lore is baked in, so the client resolves the key. */
    private static Component trStyled(String key, String fallback, ChatFormatting color) {
        return Component.translatableWithFallback(key, fallback)
                .withStyle(color).withStyle(s -> s.withItalic(false));
    }
}
