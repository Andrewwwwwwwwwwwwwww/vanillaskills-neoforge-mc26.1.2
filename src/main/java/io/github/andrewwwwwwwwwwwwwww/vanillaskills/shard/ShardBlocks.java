package io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Placed Skill Shard blocks — the Unstable block and the Stable block that harms nearby hostiles.
 *
 * <h2>How a "custom block" works here without registering one</h2>
 * The world holds a real vanilla block (see {@link #baseBlock}); VanillaSkills remembers which positions are
 * ours and what each one is, and dresses each with a {@link Display.ItemDisplay} carrying a full-cube custom
 * model, drawn fractionally oversized so it hides the block underneath completely. What a player sees is
 * entirely our texture; the vanilla block only supplies light, collision and beacon-base membership. Registering a real block would kick vanilla clients (see REWORK §2.1), and the marker-entity
 * pattern is the same one the reference pack uses for its own custom blocks.
 *
 * <p>The tracking map is the source of truth, not the block in the world — so a Stable block keeps its merge
 * count, its aura and its identity even though it is, to vanilla, an ordinary amethyst block.
 */
public class ShardBlocks {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Tags the display entity so it can be found and cleaned up without tracking its id. */
    public static final String TAG = "vanillaskills_shard_block";

    /**
     * The real block placed in the world for each kind.
     *
     * <p>The Stable block sits on {@link Blocks#DIAMOND_BLOCK} specifically so that it is a valid beacon
     * base <em>natively</em>. The alternative was adding our base block to {@code #minecraft:beacon_base_blocks},
     * which would have promoted <b>every</b> block of that type in the world to a beacon base — a side effect
     * far worse than the feature is worth. The Unstable block has no such requirement, so it stays amethyst.
     *
     * <p>Either way the visible block is the item-display overlay; the base only shows if that is ever lost.
     */
    /**
     * The vanilla block actually placed in the world under our display.
     *
     * <p>A datapack cannot register blocks — the block registry is code-side, and registering into it is
     * exactly what disconnects vanilla clients — so every "custom block" here is a real vanilla block wearing
     * a full-cube display entity. The display covers it completely, so the base contributes <b>no</b>
     * appearance. It is chosen purely for the three things a display cannot provide: light emission,
     * collision, and what a beacon considers a valid base.
     *
     * <ul>
     *   <li><b>Unstable</b> → crying obsidian, for its light level 10. Amethyst emits nothing, and no vanilla
     *       full block sits at glow lichen's 7; 10 is the nearest that still reads as a soft glow rather than
     *       a lamp. Being violet also means a failed display degrades to something plausible.</li>
     *   <li><b>Stable</b> → diamond block, because it is already in {@code #minecraft:beacon_base_blocks},
     *       which is what lets the Stable block work as a beacon base without tagging a block type globally
     *       and promoting every diamond block in the world.</li>
     * </ul>
     */
    public static net.minecraft.world.level.block.Block baseBlock(Kind kind) {
        return kind == Kind.STABLE ? Blocks.DIAMOND_BLOCK : Blocks.CRYING_OBSIDIAN;
    }

    /**
     * How many Stable blocks can be merged into one. Merging stacks the radius, so at the default of 4 a
     * fully merged block reaches 25x25x25. Read live from {@code gameplay.json}.
     */
    public static int maxMerge() {
        return io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.SHARD_MAX_MERGE;
    }

    public enum Kind {
        UNSTABLE, STABLE;

        static Kind parse(String s) {
            return "stable".equalsIgnoreCase(s) ? STABLE : UNSTABLE;
        }

        String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Serialized form of one placed block. */
    private static class Entry {
        String dim;
        int x, y, z;
        String kind;
        int merged = 1; // STABLE only: how many blocks have been merged in (1..MAX_MERGE)

        Entry() {}

        Entry(String dim, BlockPos pos, Kind kind) {
            this.dim = dim;
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.kind = kind.key();
        }

        BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private List<Entry> blocks = new ArrayList<>();
    /** dim|x,y,z -> entry. Rebuilt on load and kept in step with {@link #blocks}. */
    private final transient Map<String, Entry> index = new HashMap<>();

    private static String dimId(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    private static String key(String dim, BlockPos pos) {
        return dim + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private void reindex() {
        index.clear();
        for (Entry e : blocks) index.put(key(e.dim, e.pos()), e);
    }

    // ---- queries ----

    /** The kind of shard block at this position, or null if it is not one of ours. */
    public Kind kindAt(ServerLevel level, BlockPos pos) {
        Entry e = index.get(key(dimId(level), pos));
        return e == null ? null : Kind.parse(e.kind);
    }

    /** How many Stable blocks are merged into the one at this position (1 if unmerged, 0 if not ours). */
    public int mergeCountAt(ServerLevel level, BlockPos pos) {
        Entry e = index.get(key(dimId(level), pos));
        return e == null ? 0 : e.merged;
    }

    /**
     * Whether this player's held tool can break an Unstable Skill Shard Block.
     *
     * <p>Crystalline or better, which deliberately excludes a plain vanilla diamond pickaxe — Crystalline
     * sits <em>above</em> diamond in this mod's ladder, so shard blocks stay behind that tier.
     */
    public static boolean canMine(net.minecraft.world.entity.player.Player player) {
        if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.SHARD_MINING_GATE) return true;
        if (player.isCreative()) return true;
        ItemStack tool = player.getMainHandItem();
        return tool.is(net.minecraft.world.item.Items.NETHERITE_PICKAXE)
                || io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers.has(tool,
                        io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers.CRYSTAL.markerKey)
                || io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers.has(tool,
                        io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers.DRAGON.markerKey);
    }

    // ---- placing / breaking ----

    /** Record a shard block at {@code pos} and give it its display. The caller sets the world block. */
    public void register(ServerLevel level, BlockPos pos, Kind kind) {
        String k = key(dimId(level), pos);
        if (index.containsKey(k)) return;
        Entry e = new Entry(dimId(level), pos, kind);
        blocks.add(e);
        index.put(k, e);
        spawnDisplay(level, pos, kind);
        save();
    }

    /**
     * Remove the record at {@code pos} and return what was there, or null.
     *
     * <p>Only forgets the position — the caller decides what happens to the world block and the drops,
     * because breaking, exploding and merging all want different outcomes.
     */
    private Entry unregister(ServerLevel level, BlockPos pos) {
        Entry e = index.remove(key(dimId(level), pos));
        if (e == null) return null;
        blocks.remove(e);
        clearDisplays(level, pos);
        save();
        return e;
    }

    /** The item(s) a broken shard block should drop, honouring how many were merged into it. */
    public static List<ItemStack> dropsFor(Kind kind, int merged) {
        List<ItemStack> out = new ArrayList<>();
        if (kind == Kind.STABLE) {
            ItemStack stack = ShardItems.stableBlock();
            stack.setCount(Math.max(1, merged)); // merging is reversible: you get every block back
            out.add(stack);
        } else {
            out.add(ShardItems.unstableBlock());
        }
        return out;
    }

    /**
     * Handle a player breaking a tracked block: forget it, clear the world block, and drop our item
     * instead of the amethyst block vanilla would have dropped.
     *
     * @return true if this position was ours and has been fully handled by us
     */
    public boolean onBroken(ServerLevel level, BlockPos pos, boolean dropItems) {
        Entry e = unregister(level, pos);
        if (e == null) return false;
        level.removeBlock(pos, false);
        if (dropItems) {
            for (ItemStack drop : dropsFor(Kind.parse(e.kind), e.merged)) {
                Block.popResource(level, pos, drop);
            }
        }
        return true;
    }

    /**
     * Merge a held Stable block into the placed one, widening its aura.
     *
     * @return true if the merge happened; false when it is already at {@link #MAX_MERGE}
     */
    public boolean merge(ServerLevel level, BlockPos pos) {
        Entry e = index.get(key(dimId(level), pos));
        if (e == null || Kind.parse(e.kind) != Kind.STABLE) return false;
        if (e.merged >= maxMerge()) return false;
        e.merged++;
        save();
        return true;
    }

    // ---- display entities ----

    private static void spawnDisplay(ServerLevel level, BlockPos pos, Kind kind) {
        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
        ItemStack model = kind == Kind.STABLE ? ShardItems.stableBlock() : ShardItems.unstableBlock();
        // getSlot(0) is the public route to an item display's stack; setItemStack itself is private, and
        // going through the slot avoids needing an access widener on Fabric and a transformer on NeoForge.
        display.getSlot(0).set(model);
        display.setNoGravity(true);
        display.setInvulnerable(true);
        display.addTag(TAG);
        // Centred on the block, and drawn very slightly oversized so it hides the vanilla block underneath
        // rather than z-fighting with it — two exactly coplanar faces flicker.
        //
        // The oversize used to be baked into the model (elements running -0.2..16.2), which meant the model
        // could not inherit minecraft:block/cube_all. That parent is where a block item gets its display
        // transforms from, so without it the same item rendered as a flat unrotated square in the inventory
        // and in hand. The model is now a plain cube_all and the oversize lives here instead.
        scale(display, 1.025f);
        display.snapTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0f, 0.0f);
        level.addFreshEntity(display);
    }

    /**
     * Resize a display. Scale lives in the transformation, whose setter is private in 26.2 — see
     * {@code DisplayTransformAccessor} for why this goes through an invoker.
     */
    private static void scale(Display.ItemDisplay display, float factor) {
        ((io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin.DisplayTransformAccessor) display)
                .vanillaskills$setTransformation(new com.mojang.math.Transformation(
                        new org.joml.Vector3f(0.0f, 0.0f, 0.0f),
                        new org.joml.Quaternionf(),
                        new org.joml.Vector3f(factor, factor, factor),
                        new org.joml.Quaternionf()));
    }

    private static void clearDisplays(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(0.6);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box, en -> en.entityTags().contains(TAG))) {
            e.discard();
        }
    }

    /** Re-render every tracked block in a loaded chunk (op tool, and a repair for lost displays). */
    public int refreshAll(MinecraftServer server) {
        int count = 0;
        for (Entry e : blocks) {
            ServerLevel level = levelFor(server, e.dim);
            if (level == null || !level.isLoaded(e.pos())) continue;
            clearDisplays(level, e.pos());
            spawnDisplay(level, e.pos(), Kind.parse(e.kind));
            count++;
        }
        return count;
    }

    private static ServerLevel levelFor(MinecraftServer server, String dim) {
        for (ServerLevel level : server.getAllLevels()) {
            if (dimId(level).equals(dim)) return level;
        }
        return null;
    }

    // ---- aura ----

    /** Damage hostile mobs standing inside any Stable block's area. Call every tick; self-throttles. */
    public void tick(MinecraftServer server, long tickCount) {
        if (tickCount % io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.SHARD_AURA_INTERVAL != 0) return;
        float damage = io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.SHARD_AURA_DAMAGE;
        if (damage <= 0.0f) return; // aura disabled by config — skip the scan entirely
        int radius = io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.SHARD_AURA_RADIUS;
        for (Entry e : blocks) {
            if (Kind.parse(e.kind) != Kind.STABLE) continue;
            ServerLevel level = levelFor(server, e.dim);
            if (level == null) continue;
            BlockPos pos = e.pos();
            if (!level.isLoaded(pos)) continue; // unloaded chunks cost nothing

            // A merged block widens the same cube rather than stacking a second one on top.
            double reach = (double) radius * Math.max(1, e.merged) + 0.5;
            AABB box = new AABB(pos).inflate(reach);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, ShardBlocks::isHostile)) {
                target.hurtServer(level, level.damageSources().magic(), damage);
                spawnAuraParticles(level, target);
            }
        }
    }

    private static boolean isHostile(LivingEntity entity) {
        return entity instanceof Enemy && entity.isAlive();
    }

    /**
     * The visible tell that the aura is what is hurting something.
     *
     * <p>Magic damage is otherwise completely silent — a mob just loses health for no apparent reason, which
     * reads as a bug rather than a feature. Sent with {@code sendParticles}, so the server picks them and
     * every client sees them without needing the mod.
     *
     * <p>Enchant runes give the "arcane" read and match the block's violet; a couple of witch motes on top
     * keep it from looking like an enchanting table. Counts are small because this fires on every aura tick
     * for every mob in range.
     */
    private static void spawnAuraParticles(ServerLevel level, LivingEntity target) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();
        double spread = target.getBbWidth() * 0.5 + 0.1;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                x, y, z, 6, spread, target.getBbHeight() * 0.35, spread, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                x, y, z, 2, spread, target.getBbHeight() * 0.35, spread, 0.0);
    }

    // ---- persistence ----

    public void load() {
        Path path = path();
        try {
            if (Files.exists(path)) {
                Entry[] loaded = GSON.fromJson(Files.readString(path), Entry[].class);
                blocks = loaded != null ? new ArrayList<>(List.of(loaded)) : new ArrayList<>();
            }
        } catch (Exception e) {
            VanillaSkills.LOGGER.error("Failed to load shardblocks.json", e);
            blocks = new ArrayList<>();
        }
        reindex();
        VanillaSkills.LOGGER.info("Loaded {} placed shard block(s)", blocks.size());
    }

    public void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(blocks));
        } catch (Exception e) {
            VanillaSkills.LOGGER.error("Failed to save shardblocks.json", e);
        }
    }

    private static Path path() {
        MinecraftServer server = VanillaSkills.server;
        return server.getWorldPath(LevelResource.ROOT).resolve("vanillaskills").resolve("shardblocks.json");
    }
}
