package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Optional;

/**
 * Applies and removes the gameplay effects of skill nodes on a player.
 *
 * Attribute effects use transient modifiers keyed by a stable Identifier per
 * (node, effect-index), so reapplying on join/respawn never duplicates them and
 * removing a node cleanly reverses it. Status effects are reapplied periodically.
 */
public final class SkillEffects {
    private SkillEffects() {}

    private static final int STATUS_DURATION_TICKS = 400; // reapplied every ~2s; >10s so Night Vision never flashes

    /** Apply every effect of one node to the player. */
    public static void applyNode(ServerPlayer player, SkillNode node) {
        for (int i = 0; i < node.effects.size(); i++) {
            applyEffect(player, node, i, node.effects.get(i));
        }
    }

    /** Remove every (attribute) effect of one node from the player. */
    public static void removeNode(ServerPlayer player, SkillNode node) {
        for (int i = 0; i < node.effects.size(); i++) {
            SkillEffect effect = node.effects.get(i);
            if ("attribute".equals(effect.type)) {
                AttributeInstance inst = attributeInstance(player, effect.attribute);
                if (inst != null) inst.removeModifier(modifierId(node.id, i));
            }
        }
    }

    private static void applyEffect(ServerPlayer player, SkillNode node, int index, SkillEffect effect) {
        switch (effect.type == null ? "" : effect.type) {
            case "attribute" -> {
                AttributeInstance inst = attributeInstance(player, effect.attribute);
                if (inst == null) {
                    VanillaSkills.LOGGER.warn("Skill '{}' references unknown/absent attribute '{}'", node.id, effect.attribute);
                    return;
                }
                // Step-up (Mountaineer) is owned by StepHeight — skip applying it here while suppressed
                // (sneaking / toggled off) so we don't briefly auto-step right after a join/respawn.
                if ("minecraft:step_height".equals(effect.attribute)
                        && StepHeight.suppressed(player, VanillaSkills.PLAYERS.get(player.getUUID()))) {
                    return;
                }
                AttributeModifier modifier = new AttributeModifier(
                        modifierId(node.id, index), effect.amount, operation(effect.operation));
                inst.addOrUpdateTransientModifier(modifier);
            }
            case "status_effect" -> applyStatus(player, effect);
            case "flag" -> { /* tracked elsewhere; no direct effect */ }
            default -> VanillaSkills.LOGGER.warn("Skill '{}' has unknown effect type '{}'", node.id, effect.type);
        }
    }

    private static void applyStatus(ServerPlayer player, SkillEffect effect) {
        // Player-facing toggle: unlocked Night Vision can be switched off (/skill nightvision).
        // Guarded HERE — the single choke point — so join/respawn/unlock applies respect it too,
        // not just the periodic refresh.
        if ("minecraft:night_vision".equals(effect.effect)
                && VanillaSkills.PLAYERS.get(player.getUUID()).nightVisionDisabled) {
            return;
        }
        Holder<MobEffect> holder = mobEffect(effect.effect);
        if (holder == null) {
            VanillaSkills.LOGGER.warn("Unknown status effect '{}'", effect.effect);
            return;
        }
        player.addEffect(new MobEffectInstance(holder, STATUS_DURATION_TICKS, effect.amplifier, true, false, false));
    }

    /** Re-apply the status effects of all the player's unlocked nodes (called on a timer). */
    public static void refreshStatusEffects(ServerPlayer player, PlayerSkillData data, SkillTree tree) {
        for (String id : data.unlocked) {
            SkillNode node = tree.byId(id);
            if (node == null) continue;
            for (SkillEffect effect : node.effects) {
                if (!"status_effect".equals(effect.type)) continue;
                applyStatus(player, effect); // NV toggle enforced inside applyStatus
            }
        }
    }

    // ---- helpers ----

    public static Identifier modifierId(String nodeId, int index) {
        return Identifier.fromNamespaceAndPath("vanillaskills", "skill." + sanitize(nodeId) + "." + index);
    }

    private static String sanitize(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
    }

    private static AttributeInstance attributeInstance(ServerPlayer player, String attributeId) {
        if (attributeId == null) return null;
        Identifier id = Identifier.tryParse(attributeId);
        if (id == null) return null;
        Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.get(id);
        return holder.map(player::getAttribute).orElse(null);
    }

    private static Holder<MobEffect> mobEffect(String effectId) {
        if (effectId == null) return null;
        Identifier id = Identifier.tryParse(effectId);
        if (id == null) return null;
        return BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
    }

    private static AttributeModifier.Operation operation(String op) {
        if (op == null) return AttributeModifier.Operation.ADD_VALUE;
        return switch (op) {
            case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }
}
