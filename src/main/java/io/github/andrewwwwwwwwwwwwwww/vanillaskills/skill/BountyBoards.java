package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Physical bounty boards rendered as a holograms-style floating notice board: a scaled, panelled
 * text display listing the active bounties + a live reset countdown, a slowly-spinning Nether Star
 * above it, and an invisible {@link Interaction} entity anyone can right-click to open the quest GUI.
 * Board anchors persist to {@code world/vanillaskills/questboards.json}.
 */
public class BountyBoards {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String TAG = "vanillaskills_board";
    private static final double REMOVE_RANGE_SQR = 36.0; // 6 blocks

    private static final int LINE_WIDTH = 220;
    public static final int REFRESH_INTERVAL = 100;       // ticks between text refreshes (countdown)

    private List<Entry> boards = new ArrayList<>();

    private static class Entry {
        String dim;
        int x, y, z;
        Entry() {}
        Entry(String dim, int x, int y, int z) { this.dim = dim; this.x = x; this.y = y; this.z = z; }
    }

    private static String dimId(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    public void place(ServerPlayer op) {
        ServerLevel level = (ServerLevel) op.level();
        BlockPos base;
        HitResult hit = op.pick(6.0, 1.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult bhr) {
            base = bhr.getBlockPos();
        } else {
            base = op.blockPosition();
        }
        for (Entry e : boards) {
            if (e.dim.equals(dimId(level)) && e.x == base.getX() && e.y == base.getY() && e.z == base.getZ()) {
                op.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(op,
                        "vanillaskills.msg.board_exists", "There's already a board there.")).withStyle(ChatFormatting.RED));
                return;
            }
        }

        double ax = base.getX() + 0.5, ay = base.getY() + 1.3, az = base.getZ() + 0.5;
        spawnEntities(level, ax, ay, az);

        boards.add(new Entry(dimId(level), base.getX(), base.getY(), base.getZ()));
        save();
        op.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(op,
                "vanillaskills.msg.board_placed",
                "Bounty board placed — right-click the floating text to open the quests."))
                .withStyle(ChatFormatting.GREEN));
    }

    private static void spawnEntities(ServerLevel level, double ax, double ay, double az) {
        Display.TextDisplay text = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
        text.setText(boardText());
        text.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        text.setBackgroundColor(0); // transparent — no panel, just text
        text.setLineWidth(LINE_WIDTH);
        text.setViewRange(1.5f);
        configure(text);
        text.snapTo(ax, ay, az, 0.0f, 0.0f);
        level.addFreshEntity(text);

        Interaction interaction = new Interaction(EntityType.INTERACTION, level);
        interaction.setWidth(2.4f);
        interaction.setHeight(1.8f);
        interaction.setResponse(true);
        configure(interaction);
        interaction.snapTo(ax, ay - 0.9, az, 0.0f, 0.0f); // anchor is bottom centre
        level.addFreshEntity(interaction);
    }

    private static void configure(Entity e) {
        e.setNoGravity(true);
        e.setInvulnerable(true);
        e.addTag(TAG);
    }

    /**
     * The board's text: title, a generic prompt (bounties are per-player), and a live reset countdown.
     *
     * <p>This is a text_display entity — one entity seen by everyone — so it can't be translated
     * per-player server-side. Translation keys let each CLIENT render the board in its own language
     * (keys ship in the jar and the auto-pushed resource pack); the fallbacks keep it readable in
     * English for anyone without them.
     */
    private static Component boardText() {
        MutableComponent c = Component.translatableWithFallback(
                        "vanillaskills.board.title", "✦  BOUNTY BOARD  ✦")
                .withStyle(s -> s.withColor(0xFFD700).withBold(true).withItalic(false));
        long rem = VanillaSkills.QUESTS.nextRotationMs() - System.currentTimeMillis();
        Component when = rem <= 0
                ? Component.translatableWithFallback("vanillaskills.board.any_moment", "any moment")
                : Component.literal((rem / 3_600_000) + "h " + (rem % 3_600_000 / 60_000) + "m");
        c.append(Component.literal("\n\n"));
        c.append(Component.translatableWithFallback("vanillaskills.board.view", "Right-click to view your bounties")
                .withStyle(s -> s.withColor(0xFFFFFF).withItalic(false)));
        c.append(Component.literal("\n"));
        c.append(Component.translatableWithFallback("vanillaskills.board.rotation", "⏳ New bounties in %s", when)
                .withStyle(s -> s.withColor(0xFFD54A).withItalic(false)));
        c.append(Component.literal("\n"));
        c.append(Component.translatableWithFallback("vanillaskills.board.open", "▶ Right-click to open ◀")
                .withStyle(s -> s.withColor(0xBBBBBB).withItalic(false)));
        c.append(Component.literal("\n"));
        c.append(Component.translatableWithFallback("vanillaskills.board.command", "or use /quests")
                .withStyle(s -> s.withColor(0x888888).withItalic(false)));
        return c;
    }

    /** Called periodically to refresh the board text (so the countdown stays current). */
    public void tick(MinecraftServer server, long tickCount) {
        if (tickCount % REFRESH_INTERVAL != 0) return;
        Component text = boardText();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getEntities(EntityTypeTest.forClass(Entity.class),
                    en -> en.entityTags().contains(TAG))) {
                if (e instanceof Display.TextDisplay td) td.setText(text);
            }
        }
    }

    private static ServerLevel levelFor(MinecraftServer server, String dim) {
        for (ServerLevel level : server.getAllLevels()) {
            if (dimId(level).equals(dim)) return level;
        }
        return null;
    }

    /** Re-render every placed board in a loaded chunk (despawn its old entities, spawn fresh ones). */
    public int refreshAll(MinecraftServer server) {
        int count = 0;
        for (Entry e : boards) {
            ServerLevel level = levelFor(server, e.dim);
            if (level == null) continue;
            BlockPos base = new BlockPos(e.x, e.y, e.z);
            if (!level.isLoaded(base)) continue; // chunk not loaded — skip
            AABB box = new AABB(base).inflate(4.0);
            for (Entity ent : level.getEntitiesOfClass(Entity.class, box, en -> en.entityTags().contains(TAG))) {
                ent.discard();
            }
            spawnEntities(level, e.x + 0.5, e.y + 1.6, e.z + 0.5);
            count++;
        }
        return count;
    }

    public void removeNear(ServerPlayer op) {
        ServerLevel level = (ServerLevel) op.level();
        String dim = dimId(level);
        BlockPos p = op.blockPosition();
        Entry best = null;
        double bestD = Double.MAX_VALUE;
        for (Entry e : boards) {
            if (!e.dim.equals(dim)) continue;
            double dx = e.x - p.getX(), dy = e.y - p.getY(), dz = e.z - p.getZ();
            double d = dx * dx + dy * dy + dz * dz;
            if (d <= REMOVE_RANGE_SQR && d < bestD) { best = e; bestD = d; }
        }
        if (best == null) {
            op.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(op,"vanillaskills.msg.board_none","No bounty board within 6 blocks.")).withStyle(ChatFormatting.RED));
            return;
        }
        AABB box = new AABB(new BlockPos(best.x, best.y, best.z)).inflate(4.0);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box, en -> en.entityTags().contains(TAG))) {
            e.discard();
        }
        boards.remove(best);
        save();
        op.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(op,"vanillaskills.msg.board_removed","Bounty board removed.")).withStyle(ChatFormatting.GREEN));
    }

    // ---- persistence ----

    public void load() {
        Path path = path();
        try {
            if (Files.exists(path)) {
                Entry[] loaded = GSON.fromJson(Files.readString(path), Entry[].class);
                boards = loaded != null ? new ArrayList<>(List.of(loaded)) : new ArrayList<>();
            }
        } catch (Exception e) {
            VanillaSkills.LOGGER.error("Failed to load questboards.json", e);
        }
    }

    public void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(boards));
        } catch (Exception e) {
            VanillaSkills.LOGGER.error("Failed to save questboards.json", e);
        }
    }

    private static Path path() {
        MinecraftServer server = VanillaSkills.server;
        return server.getWorldPath(LevelResource.ROOT).resolve("vanillaskills").resolve("questboards.json");
    }
}
