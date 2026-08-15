package io.github.andrewwwwwwwwwwwwwww.vanillaskills.crate;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.data.CrateDef;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The crate opening, as a slot machine: one lane per item the crate can pay out, each spinning its own reel
 * and stopping left to right on what you actually won.
 *
 * <p><b>The reveal is theatre; the roll already happened.</b> The loot table is rolled the instant the crate
 * is opened and the result is held here, so nothing about the animation can change the outcome, and a player
 * who disconnects halfway through still gets everything.
 *
 * <p><b>Lane count is the crate's maximum, not this roll's count.</b> A pool that rolls 2–3 times always
 * shows three lanes; a roll that only produced two fills the third with a barrier. Showing a variable number
 * of lanes would leak the result before the reels stopped — you would know you had won three things the
 * moment the machine appeared.
 *
 * <p>Skill Shards are deliberately excluded from the lanes. Every crate contains them, so a lane that always
 * shows the same thing carries no information and just makes the machine wider.
 *
 * <p><b>Why the reels scroll rather than spin.</b> Moving entities every tick depends on client-side
 * interpolation, and display entities smooth teleports over a duration this code cannot set — both
 * {@code Display#setTransformation} and the teleport-duration accessor are private in 26.2. Instead each lane
 * is three fixed positions whose <i>items</i> shift down a row per step, which reads as a reel scrolling past
 * a payline while every frame is exactly what the server intended.
 */
public final class CrateReel {
    private CrateReel() {}

    /** Tag every reel entity carries, so orphans can be found and removed without tracking them. */
    public static final String TAG = "vanillaskills_crate_reel";

    /** Rows per lane. The middle one is the payline; the outer two exist to sell the scroll. */
    private static final int ROWS = 3;
    private static final int PAYLINE = 1;

    /** Ticks between one lane stopping and the next, so they land left to right instead of together. */
    private static final int LANE_STAGGER = 8;

    /** One machine per player. A second crate opened mid-spin resolves the first immediately. */
    private static final Map<UUID, Reel> active = new ConcurrentHashMap<>();

    private static final class Reel {
        final UUID playerId;
        /** [lane][row] — row 0 is top, {@link #PAYLINE} is the payline. */
        final Display.ItemDisplay[][] cells;
        /** What each lane is spinning through while it has not landed. */
        final List<ItemStack> filler;
        /** What each lane lands on. An empty stack means that lane pays out nothing. */
        final List<ItemStack> results;
        /** The whole roll, handed over when the machine finishes. */
        final List<ItemStack> reward;
        final int[] stopAt;
        final boolean[] stopped;
        int age;
        int nextStepAt;
        int stepDelay = 1;
        int settledAt = -1;

        Reel(ServerPlayer player, int lanes, List<ItemStack> filler, List<ItemStack> results,
             List<ItemStack> reward) {
            this.playerId = player.getUUID();
            this.cells = new Display.ItemDisplay[lanes][ROWS];
            this.filler = filler;
            this.results = results;
            this.reward = reward;
            this.stopAt = new int[lanes];
            this.stopped = new boolean[lanes];
            for (int i = 0; i < lanes; i++) {
                this.stopAt[i] = GameplayConfig.CRATE_REEL_TICKS + i * LANE_STAGGER;
            }
        }

        boolean allStopped() {
            for (boolean s : stopped) if (!s) return false;
            return true;
        }
    }

    /**
     * Start a machine.
     *
     * @param filler  items the reels scroll through; never empty
     * @param results one entry per lane; an empty stack becomes a barrier
     * @param reward  the entire roll, granted when the machine finishes
     */
    public static void start(ServerPlayer player, CrateDef def, List<ItemStack> filler,
                             List<ItemStack> results, List<ItemStack> reward) {
        if (!(player.level() instanceof ServerLevel level) || filler.isEmpty() || results.isEmpty()) {
            grant(player, reward);
            return;
        }
        // A second crate while one is spinning: settle the first now rather than stacking machines.
        finish(level.getServer(), player.getUUID(), true);

        Reel reel = new Reel(player, results.size(), filler, results, reward);
        Vec3 centre = centreFor(player);

        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0e-4) flat = new Vec3(0.0, 0.0, 1.0);
        flat = flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);       // horizontal, across the player's view
        float faceYaw = player.getYRot();
        double spacing = GameplayConfig.CRATE_REEL_SCALE * GameplayConfig.CRATE_REEL_SPACING;

        for (int lane = 0; lane < results.size(); lane++) {
            double across = (lane - (results.size() - 1) / 2.0) * spacing;
            for (int row = 0; row < ROWS; row++) {
                double up = (PAYLINE - row) * spacing;
                Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
                display.setNoGravity(true);
                display.setInvulnerable(true);
                display.addTag(TAG);
                display.getSlot(0).set(styled(randomFiller(level, filler), false));
                scale(display, (float) GameplayConfig.CRATE_REEL_SCALE);
                display.snapTo(centre.x + right.x * across, centre.y + up,
                        centre.z + right.z * across, faceYaw, 0.0f);
                level.addFreshEntity(display);
                reel.cells[lane][row] = display;
            }
        }

        active.put(player.getUUID(), reel);
        level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.PLAYERS, 0.7f, 0.8f);
    }

    /** Eye height, in front of where the player is looking, so the machine frames their view. */
    private static Vec3 centreFor(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0e-4) flat = new Vec3(0.0, 0.0, 1.0);
        flat = flat.normalize().scale(GameplayConfig.CRATE_REEL_DISTANCE);
        return player.position().add(flat.x, player.getEyeHeight() - 0.1, flat.z);
    }

    /** Drive every running machine. Called once per server tick; each paces itself. */
    public static void tick(MinecraftServer server) {
        if (active.isEmpty()) return;
        for (UUID id : List.copyOf(active.keySet())) {
            Reel reel = active.get(id);
            if (reel == null) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {                       // logged out mid-spin
                finish(server, id, true);
                continue;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                finish(server, id, true);
                continue;
            }
            reel.age++;

            if (!reel.allStopped() && reel.age >= reel.nextStepAt) {
                step(level, player, reel);
                // Ease off as the machine runs down, so the last lanes visibly crawl to a stop.
                if (reel.age > GameplayConfig.CRATE_REEL_TICKS / 2 && reel.stepDelay < 6) reel.stepDelay++;
                reel.nextStepAt = reel.age + reel.stepDelay;
                if (reel.allStopped()) {
                    reel.settledAt = reel.age;
                    onSettle(level, player, reel);
                }
            }

            // Hold on the result, then pay out. The hard cap means a stuck machine can never keep the loot.
            if ((reel.settledAt >= 0 && reel.age > reel.settledAt + GameplayConfig.CRATE_REEL_HOLD)
                    || reel.age > GameplayConfig.CRATE_REEL_TICKS * 6) {
                finish(server, id, true);
            }
        }
    }

    /** One scroll step: every still-spinning lane shifts down a row and pulls a new item in at the top. */
    private static void step(ServerLevel level, ServerPlayer player, Reel reel) {
        for (int lane = 0; lane < reel.cells.length; lane++) {
            if (reel.stopped[lane]) continue;

            if (reel.age >= reel.stopAt[lane]) {
                stopLane(level, player, reel, lane);
                continue;
            }
            // Shift bottom-up so each row takes the one above it before that row is overwritten.
            for (int row = ROWS - 1; row > 0; row--) {
                ItemStack above = reel.cells[lane][row - 1].getSlot(0).get();
                reel.cells[lane][row].getSlot(0).set(styled(above, false));
            }
            reel.cells[lane][0].getSlot(0).set(styled(randomFiller(level, reel.filler), false));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(),
                SoundSource.PLAYERS, 0.4f, 1.2f);
    }

    /** Land one lane on its result and mark the payline. */
    private static void stopLane(ServerLevel level, ServerPlayer player, Reel reel, int lane) {
        reel.stopped[lane] = true;
        ItemStack result = reel.results.get(lane);
        boolean nothing = result.isEmpty();

        reel.cells[lane][PAYLINE].getSlot(0).set(nothing ? barrier() : styled(result, true));
        // The rows either side keep spinning-looking filler, so the payline reads as the stopped position
        // rather than the lane simply emptying.
        for (int row = 0; row < ROWS; row++) {
            if (row == PAYLINE) continue;
            reel.cells[lane][row].getSlot(0).set(styled(randomFiller(level, reel.filler), false));
        }

        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                SoundSource.PLAYERS, 0.6f, nothing ? 0.7f : 1.5f);
        Display.ItemDisplay cell = reel.cells[lane][PAYLINE];
        if (!nothing) {
            level.sendParticles(ParticleTypes.END_ROD, cell.getX(), cell.getY(), cell.getZ(),
                    8, 0.15, 0.15, 0.15, 0.02);
        }
    }

    /** Every lane has landed. */
    private static void onSettle(ServerLevel level, ServerPlayer player, Reel reel) {
        int won = 0;
        for (ItemStack result : reel.results) if (!result.isEmpty()) won++;
        if (won == 0) return;

        for (Display.ItemDisplay[] lane : reel.cells) {
            Display.ItemDisplay cell = lane[PAYLINE];
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cell.getX(), cell.getY(), cell.getZ(),
                    10, 0.2, 0.2, 0.2, 0.1);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.7f, 1.3f);
    }

    private static ItemStack randomFiller(ServerLevel level, List<ItemStack> filler) {
        return filler.get(level.getRandom().nextInt(filler.size()));
    }

    /** The "this lane won nothing" marker. */
    private static ItemStack barrier() {
        ItemStack stack = new ItemStack(Items.BARRIER);
        stack.set(DataComponents.ITEM_NAME, Component.translatableWithFallback(
                        "vanillaskills.crate.nothing", "Nothing")
                .withStyle(ChatFormatting.DARK_GRAY).withStyle(s -> s.withItalic(false)));
        return stack;
    }

    /**
     * Shrink a display. Items render at one full block by default, which at machine scale is a wall of cubes.
     *
     * <p>Scale lives in the transformation, whose setter is private in 26.2 — see
     * {@code DisplayTransformAccessor} for why this goes through an invoker rather than a reimplementation.
     */
    private static void scale(Display.ItemDisplay display, float factor) {
        ((io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin.DisplayTransformAccessor) display)
                .vanillaskills$setTransformation(new com.mojang.math.Transformation(
                        new org.joml.Vector3f(0.0f, 0.0f, 0.0f),
                        new org.joml.Quaternionf(),
                        new org.joml.Vector3f(factor, factor, factor),
                        new org.joml.Quaternionf()));
    }

    private static ItemStack styled(ItemStack source, boolean selected) {
        ItemStack copy = source.copy();
        copy.setCount(1);
        copy.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, selected);
        return copy;
    }

    /**
     * End a machine: remove its entities and pay out.
     *
     * <p>The reward is granted exactly once, because the reel is removed from the map before anything else.
     */
    public static void finish(MinecraftServer server, UUID playerId, boolean payOut) {
        Reel reel = active.remove(playerId);
        if (reel == null) return;
        for (Display.ItemDisplay[] lane : reel.cells) {
            for (Display.ItemDisplay cell : lane) {
                if (cell != null) cell.discard();
            }
        }
        if (!payOut || server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            grant(player, reel.reward);
        } else {
            // Offline: the crate was already consumed, so the loot must not evaporate.
            PendingRewards.store(playerId, reel.reward);
        }
    }

    /** Settle every running machine — used on shutdown so nothing is lost with the server. */
    public static void finishAll(MinecraftServer server) {
        for (UUID id : List.copyOf(active.keySet())) finish(server, id, true);
    }

    private static void grant(ServerPlayer player, List<ItemStack> reward) {
        for (ItemStack drop : reward) player.getInventory().placeItemBackInInventory(drop);
    }

    /**
     * Remove any reel entities left behind by a crash. Display entities persist, so without this a hard
     * shutdown mid-spin would leave a machine hanging in the world with nothing tracking it.
     */
    public static void sweep(ServerLevel level) {
        AABB everywhere = new AABB(-3.0e7, level.getMinY(), -3.0e7, 3.0e7, level.getMaxY(), 3.0e7);
        for (Entity e : level.getEntitiesOfClass(Entity.class, everywhere,
                en -> en.entityTags().contains(TAG))) {
            e.discard();
        }
    }

    /** Reward that could not be handed over because the player was offline. */
    public static final class PendingRewards {
        private PendingRewards() {}

        private static final Map<UUID, List<ItemStack>> pending = new LinkedHashMap<>();

        static synchronized void store(UUID playerId, List<ItemStack> reward) {
            pending.computeIfAbsent(playerId, k -> new ArrayList<>()).addAll(reward);
        }

        /** Hand over anything owed. Call on join. */
        public static synchronized void deliver(ServerPlayer player) {
            List<ItemStack> owed = pending.remove(player.getUUID());
            if (owed == null || owed.isEmpty()) return;
            for (ItemStack drop : owed) player.getInventory().placeItemBackInInventory(drop);
        }
    }
}
