package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dragon (Netherite II) set behaviour:
 * - Full set: immunity to fire/lava/dragon's-breath damage (the damage cancel itself lives in
 *   {@code DragonImmunityMixin}; here we also keep the wearer from visibly burning).
 * - Full set: an active "Dragon Charge" dive-dash — hold sneak while airborne to lunge in the look
 *   direction (a downward swoop), on a short cooldown. Pairs with the Elytra-combined chestplate.
 */
public final class DragonSet {
    private DragonSet() {}

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    // Live from gameplay.json - dragonDashSpeed / dragonDashDownBias / dragonDashCooldownTicks.
    private static double dashSpeed() { return io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.DRAGON_DASH_SPEED; }
    private static double dashDownBias() { return io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.DRAGON_DASH_DOWN_BIAS; }
    private static long dashCooldownTicks() { return io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.DRAGON_DASH_COOLDOWN_TICKS; }

    private static final Map<UUID, Long> lastDashTick = new HashMap<>();

    /** True only while all four Dragon pieces are worn (checked live, so it reverts instantly). */
    public static boolean isFullSet(LivingEntity entity) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!ArmorTiers.DRAGON.isWorn(entity.getItemBySlot(slot))) return false;
        }
        return true;
    }

    /** Run each server tick: clears fire on full-set wearers and handles the dive-dash. */
    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isFullSet(player)) {
                lastDashTick.remove(player.getUUID());
                continue;
            }
            if (player.getRemainingFireTicks() > 0) {
                player.clearFire();
            }
            tryDash(player);
        }
    }

    private static void tryDash(ServerPlayer player) {
        if (player.onGround() || !player.isShiftKeyDown()) return;

        long now = player.level().getGameTime();
        Long last = lastDashTick.get(player.getUUID());
        if (last != null && now - last < dashCooldownTicks()) return;
        lastDashTick.put(player.getUUID(), now);

        Vec3 look = player.getLookAngle();
        Vec3 velocity = look.scale(dashSpeed()).add(0.0, -dashDownBias(), 0.0);
        player.setDeltaMovement(velocity);
        player.hurtMarked = true; // forces a velocity packet to the client
        player.fallDistance = 0.0;
    }

    public static void onPlayerLeave(UUID uuid) {
        lastDashTick.remove(uuid);
    }

    /** Static description shown on a crafted / stored Dragon piece. */
    public static ItemLore baseLore() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(""));
        lines.add(trStyled("vanillaskills.set.dragon.name_plain", "Dragon Set", ChatFormatting.LIGHT_PURPLE));
        lines.add(trStyled("vanillaskills.set.dragon.desc1", "Full set: immune to fire, lava", ChatFormatting.GRAY));
        lines.add(trStyled("vanillaskills.set.dragon.desc2", "and dragon's breath", ChatFormatting.GRAY));
        lines.add(trStyled("vanillaskills.set.dragon.desc3", "Hold sneak while airborne to dive-dash", ChatFormatting.GRAY));
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
