package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the Mountaineer (step-up) skill's {@code minecraft:step_height} bonus dynamically, so it is
 * SUPPRESSED while the player is sneaking (hold shift → vanilla step height, so you carefully step
 * onto/around ledges near lava instead of auto-stepping) and while toggled off via
 * {@code /skill toggle stepup}.
 *
 * <p>Done as server-side attribute management (the step_height attribute is synced to the client, so
 * removing the modifier makes even a vanilla client stop auto-stepping within a couple of ticks).
 *
 * <p><b>Debounced.</b> We only actually add/remove the modifier once the desired state has held for
 * {@link #DEBOUNCE_TICKS} consecutive ticks. This is critical: {@code isShiftKeyDown()} can flicker
 * between ticks, and reacting to every flicker churned the attribute every tick (a sync packet per
 * tick → severe client stutter — the 1.2.2 lag). Debouncing means the attribute changes at most once
 * per real sneak/stand transition. {@link #applied} tracks what's currently on the attribute so we
 * never issue a redundant attribute op.
 */
public final class StepHeight {
    private StepHeight() {}

    private static final String STEP_ATTR = "minecraft:step_height";
    private static final String STEP_LANE_PREFIX = "mountaineer"; // node ids: mountaineer_1..3
    private static final int DEBOUNCE_TICKS = 3; // ~150ms of stable state before we touch the attribute

    /** uuid -> what is currently applied to the attribute (true = suppressed / bonus removed). */
    private static final Map<UUID, Boolean> applied = new HashMap<>();
    /** uuid -> consecutive ticks the desired state has differed from {@link #applied}. */
    private static final Map<UUID, Integer> pendingAge = new HashMap<>();

    /** True when the step-up bonus should be OFF right now (sneaking, or toggled off). */
    public static boolean suppressed(ServerPlayer player, PlayerSkillData data) {
        return player.isShiftKeyDown() || (data != null && data.stepUpDisabled);
    }

    /** Whether the player has any step-up node unlocked (cheap prefix scan). */
    public static boolean hasStepSkill(PlayerSkillData data) {
        if (data == null) return false;
        for (String id : data.unlocked) {
            if (id.startsWith(STEP_LANE_PREFIX)) return true;
        }
        return false;
    }

    /** Called every server tick. Cheap steady path; only touches the attribute on a debounced change. */
    public static void tick(MinecraftServer server, SkillTree tree) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            PlayerSkillData data = VanillaSkills.PLAYERS.get(uuid);
            if (!hasStepSkill(data)) {           // no step skill → nothing to manage
                if (applied.remove(uuid) != null) pendingAge.remove(uuid);
                continue;
            }
            boolean desired = suppressed(player, data);
            Boolean app = applied.get(uuid);
            if (app == null) {                   // first evaluation → set immediately, no debounce
                applied.put(uuid, desired);
                pendingAge.remove(uuid);
                reconcile(player, data, tree, desired);
                continue;
            }
            if (app == desired) {                // stable & matching → clear any pending, done
                pendingAge.remove(uuid);
                continue;
            }
            // desired differs from what's applied — count consecutive ticks, flip only when it sticks.
            int age = pendingAge.merge(uuid, 1, Integer::sum);
            if (age >= DEBOUNCE_TICKS) {
                applied.put(uuid, desired);
                pendingAge.remove(uuid);
                reconcile(player, data, tree, desired);
            }
        }
    }

    /** Add or remove the step_height modifiers of the player's unlocked step nodes to match {@code sup}. */
    static void reconcile(ServerPlayer player, PlayerSkillData data, SkillTree tree, boolean sup) {
        AttributeInstance inst = stepInstance(player);
        if (inst == null) return;
        for (String id : data.unlocked) {
            SkillNode node = tree.byId(id);
            if (node == null) continue;
            for (int i = 0; i < node.effects.size(); i++) {
                SkillEffect e = node.effects.get(i);
                if (!"attribute".equals(e.type) || !STEP_ATTR.equals(e.attribute)) continue;
                Identifier modId = SkillEffects.modifierId(node.id, i);
                if (sup) {
                    inst.removeModifier(modId);
                } else {
                    inst.addOrUpdateTransientModifier(new AttributeModifier(
                            modId, e.amount, AttributeModifier.Operation.ADD_VALUE));
                }
            }
        }
    }

    /** Force a fresh (immediate, un-debounced) re-evaluation next tick — used right after the toggle. */
    public static void invalidate(UUID uuid) {
        applied.remove(uuid);
        pendingAge.remove(uuid);
    }

    public static void onLeave(UUID uuid) {
        applied.remove(uuid);
        pendingAge.remove(uuid);
    }

    private static AttributeInstance stepInstance(ServerPlayer player) {
        Identifier id = Identifier.tryParse(STEP_ATTR);
        if (id == null) return null;
        Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.get(id);
        return holder.map(player::getAttribute).orElse(null);
    }
}
