package io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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
 * Registering a real block would kick vanilla clients (see REWORK §2.1), so instead VanillaSkills <b>takes
 * over</b> two vanilla blocks outright: reinforced deepslate and lodestone (see {@link #baseBlock}). Both are
 * retextured in the pushed resource pack, and the datapack removes every vanilla way to obtain one — so a
 * block of either type in the world is, by construction, ours.
 *
 * <p>This replaced an earlier design that left the vanilla block in place and covered it with an oversized
 * {@code Display.ItemDisplay}. That overlay was lit by the light level at its own position — inside a solid
 * block, i.e. zero — so it rendered black, and the client culled it whenever the chunk re-meshed, which made
 * the texture "revert" on any nearby block update. Owning the block outright removes the overlay, and with it
 * the lighting, culling and hand-rolled-placement problems all at once.
 *
 * <p>Positions are still tracked, because the Stable block needs its merge count and both kinds need to know
 * a player put them there rather than worldgen.
 */
public class ShardBlocks {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Tags the display entity so it can be found and cleaned up without tracking its id. */
    public static final String TAG = "vanillaskills_shard_block";

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
     *   <li><b>Unstable</b> → reinforced deepslate. Vanilla places it in ancient city floors and nowhere
     *       else, and our datapack rewrites those to obsidian at generation, so every reinforced deepslate
     *       in a new world is ours. It has no recipe and an empty loot table, so a player cannot obtain or
     *       place one by any other route.</li>
     *   <li><b>Stable</b> → lodestone. Removed from vanilla the same way: its recipe is neutered and it is
     *       stripped from the two chest tables that carried it. Our datapack then adds it to
     *       {@code #minecraft:beacon_base_blocks}, which is safe precisely because no other lodestone can
     *       exist to be promoted.</li>
     * </ul>
     */
    public static net.minecraft.world.level.block.Block baseBlock(Kind kind) {
        return kind == Kind.STABLE ? Blocks.LODESTONE : Blocks.REINFORCED_DEEPSLATE;
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

    /**
     * The kind of shard block at this position, or null if it is not one of ours.
     *
     * <p>Answered from the <b>block in the world</b>, not the tracking map. Both base blocks are taken over
     * from vanilla, so their presence is the identity — which means a block placed by any route at all
     * (creative menu, {@code /setblock}, a hopper into a dispenser) behaves correctly, not just one placed
     * through our own use handler. Relying on the map is what made a placed block behave like plain vanilla.
     */
    public Kind kindAt(ServerLevel level, BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.LODESTONE)) return Kind.STABLE;
        if (state.is(Blocks.REINFORCED_DEEPSLATE)) return Kind.UNSTABLE;
        return null;
    }

    /**
     * How many Stable blocks are merged into the one at this position (0 if not ours).
     *
     * <p>This is the one question the block alone cannot answer, and the only reason the tracking map still
     * exists. An untracked Stable block is simply unmerged, so it reports 1 rather than 0 — a block placed
     * outside our handler is still a real Stable block, just one nobody has merged into yet.
     */
    public int mergeCountAt(ServerLevel level, BlockPos pos) {
        Entry e = index.get(key(dimId(level), pos));
        if (e != null) return e.merged;
        return kindAt(level, pos) == Kind.STABLE ? 1 : 0;
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

    /** Record a shard block at {@code pos}. The caller sets the world block. */
    public void register(ServerLevel level, BlockPos pos, Kind kind) {
        String k = key(dimId(level), pos);
        if (index.containsKey(k)) return;
        Entry e = new Entry(dimId(level), pos, kind);
        blocks.add(e);
        index.put(k, e);
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
        clearDisplays(level, pos); // legacy overlays from the pre-takeover design
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
     * Handle a player breaking a tracked block: forget it, clear the world block, and drop our item.
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

    /**
     * Delete any leftover display overlay at this position.
     *
     * <p>Nothing creates these any more — the blocks are real and retextured. It stays because overlays
     * written into a world by an earlier version are still sitting in it, and would otherwise hover over
     * the block forever with nothing tracking them.
     */
    private static void clearDisplays(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(0.6);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box, en -> en.entityTags().contains(TAG))) {
            e.discard();
        }
    }

    /**
     * Bring every tracked block in a loaded chunk onto the current design.
     *
     * <p>Heals the base block if it was placed by a version that used a different one, and deletes the
     * display overlay that version left hovering over it. Runs on server start; positions in unloaded
     * chunks are simply skipped, and there is nothing to catch up on in a world built by this version.
     */
    public int refreshAll(MinecraftServer server) {
        int count = 0;
        for (Entry e : blocks) {
            ServerLevel level = levelFor(server, e.dim);
            if (level == null || !level.isLoaded(e.pos())) continue;
            Kind kind = Kind.parse(e.kind);
            // Heal the base block if this position was placed by a version that used a different one.
            // 2.0.0 moved the Unstable base off crying obsidian, whose drip particles escaped the overlay.
            Block want = baseBlock(kind);
            if (!level.getBlockState(e.pos()).is(want)) {
                level.setBlockAndUpdate(e.pos(), want.defaultBlockState());
            }
            clearDisplays(level, e.pos());
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
