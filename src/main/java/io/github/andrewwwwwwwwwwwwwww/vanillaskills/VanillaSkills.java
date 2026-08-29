package io.github.andrewwwwwwwwwwwwwww.vanillaskills;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.command.SkillCommands;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.PointsConfig;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorCraftingRecipe;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonScale;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonSet;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.RoseGoldSet;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.loot.FortuneTemplateLoot;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.FortuneTemplateRecipe;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.FortuneUpgradeRecipe;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.PlayerSkillData;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.PlayerSkillManager;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillEffects;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillTree;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillTreeManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(VanillaSkills.MOD_ID)
public class VanillaSkills {
    public static final String MOD_ID = "vanillaskills";
    public static final Logger LOGGER = LoggerFactory.getLogger("VanillaSkills");
    /** Stable id for our server-pushed texture pack, so the client de-duplicates re-pushes. */
    private static final java.util.UUID RESOURCE_PACK_ID = java.util.UUID.fromString("5b6c9a10-7e2d-4c3a-9f11-a1b2c3d4e5f6");

    public static MinecraftServer server;

    /** Per-world vanillaskills data/config directory (inside the world save), or null if no world is loaded. */
    public static java.nio.file.Path worldDir() {
        return server == null ? null
                : server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("vanillaskills");
    }
    public static final SkillTreeManager TREE = new SkillTreeManager();
    public static final PlayerSkillManager PLAYERS = new PlayerSkillManager();
    public static final io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.QuestBoard QUESTS =
            new io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.QuestBoard();
    public static final io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.BountyBoards BOARDS =
            new io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.BountyBoards();

    public static final io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBlocks SHARDS =
            new io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBlocks();
    public static final io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.WorldState STATE =
            new io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.WorldState();

    private static final int ROSE_GOLD_INTERVAL = 10;
    private static final int STATUS_REFRESH_INTERVAL = 40;
    private static final int DRAGON_SCALE_DROP = 8;
    private static final int QUEST_ROTATION_INTERVAL = 200; // check the bounty timer every ~10s
    private static final int ELYTRA_FORGE_INTERVAL = 20; // scan items on anvils/grindstones once a second

    private int tickCounter = 0;

    // NeoForge: registries are frozen outside RegisterEvent, so serializers go through DeferredRegister.
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);
    static {
        RECIPE_SERIALIZERS.register("fortune_upgrade", () -> FortuneUpgradeRecipe.SERIALIZER);
        RECIPE_SERIALIZERS.register("fortune_template", () -> FortuneTemplateRecipe.SERIALIZER);
        RECIPE_SERIALIZERS.register("tool_crafting", () -> io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolCraftingRecipe.SERIALIZER);
        RECIPE_SERIALIZERS.register("armor_crafting", () -> ArmorCraftingRecipe.SERIALIZER);
        RECIPE_SERIALIZERS.register("dragon_ingot", () -> io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonIngotRecipe.SERIALIZER);
        RECIPE_SERIALIZERS.register("dragon_template_dup", () -> io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.DragonTemplateRecipe.SERIALIZER);
        RECIPE_SERIALIZERS.register("shard_crafting", () -> io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardCraftingRecipe.SERIALIZER);
    }

    public VanillaSkills(IEventBus modBus) {
        LOGGER.info("VanillaSkills initializing (NeoForge)");

        RECIPE_SERIALIZERS.register(modBus);
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.creative.VanillaSkillsItemGroup.register(modBus);

        // Loot injection (Fortune + Dragon templates into chest loot).
        FortuneTemplateLoot.register();
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.loot.DragonTemplateLoot.register();
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.loot.ShardLoot.register();
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.loot.CrateLoot.register();

        // Datapack-driven content — NeoForge's counterpart to Fabric's SERVER_DATA reload listener.
        // ⚠ The event is AddServerReloadListenersEvent; AddReloadListenerEvent does not exist in 26.x.
        // It extends Event, so it belongs on the game bus, not the mod bus. Nothing reached from here
        // may touch world state — the first reload runs before ServerStartedEvent assigns `server`.
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) -> e.addListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "content"),
                (ResourceManagerReloadListener) manager -> {
                    io.github.andrewwwwwwwwwwwwwww.vanillaskills.data.VsContent.reload(manager);

                    // Rebuild the tree so a pack edit takes effect on /reload rather than only on
                    // restart. Guarded on `server`: this listener also runs during the FIRST datapack
                    // load, while the server is still being constructed.
                    if (server != null) {
                        TREE.load();
                        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                            PLAYERS.applyAll(player);
                        }
                    }
                }));

        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            server = e.getServer();
            PLAYERS.setPointsConfig(PointsConfig.load());
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.load();
            // Compute total earnable points (P) before building the tree so the default tree can be
            // priced against it (whole tree = P, Night Vision gated at P/3).
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillTreeManager.economyP = PLAYERS.computeTotalEarnable();
            TREE.load();
            QUESTS.load();
            BOARDS.load();
            SHARDS.load();
            STATE.load();
            // Display entities persist, so a crash mid-spin would otherwise leave a ring of items hanging
            // in the world with nothing tracking it.
            for (ServerLevel lvl : e.getServer().getAllLevels()) {
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.crate.CrateReel.sweep(lvl);
            }
            // Redraw every tracked Skill Shard block. Their overlays are entities written into the world,
            // so one built by an older version keeps whatever size and model it was spawned with — 2.0.0
            // moved the anti-z-fighting oversize off the model and onto the entity, and an un-redrawn
            // overlay flickers against the vanilla block underneath. Doubles as the repair for overlays
            // lost to a chunk-delete or an entity wipe. Only touches loaded chunks; the rest catch up as
            // they load.
            int redrawn = SHARDS.refreshAll(server);
            if (redrawn > 0) LOGGER.info("Redrew {} Skill Shard block overlay(s)", redrawn);
        });

        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
            // Settle first: a crate is consumed the moment it is opened, so a reel still spinning at
            // shutdown is holding loot that has already been paid for.
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.crate.CrateReel.finishAll(e.getServer());
            PLAYERS.saveAllAndClear();
            QUESTS.save();
            BOARDS.save();
            SHARDS.save();
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.TaskShards.clear();
        });

        // Join: skill data, recipe-book grants, and the forced texture-pack push (vanilla clients see
        // the gear with zero server.properties setup; configurable in gameplay.json; on by default).
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer player)) return;
            PLAYERS.onJoin(player);
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.LegacyGear.sweep(player); // repoint pre-2.0 gear models
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.RepairMaterials.sweep(player);
            // Our data recipes appear in the book only once the matching skill is unlocked.
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.RecipeUnlocks.sync(player);
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBar.push(player, true);
            // Anything a crate owed them from a reel they logged out of.
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.crate.CrateReel.PendingRewards.deliver(player);

            if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.PUSH_RESOURCE_PACK) return;
            MinecraftServer srv = server; // set on ServerStartedEvent, always before players join
            if (srv == null) return;
            // Skip pure single-player: the host has every texture in the mod jar already, including the
            // assets/minecraft/** overrides the block takeover needs. Keep those bundled — without them a
            // single-player world shows plain reinforced deepslate and lodestone with their vanilla names.
            // Still push on LAN-opened worlds and dedicated servers, where vanilla clients can join.
            if (srv.isSingleplayer() && !srv.isPublished()) return;
            String url = io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.RESOURCE_PACK_URL;
            if (url == null || url.isEmpty()) return;
            player.connection.send(new ClientboundResourcePackPushPacket(
                    RESOURCE_PACK_ID, url,
                    io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.RESOURCE_PACK_SHA1,
                    true,
                    java.util.Optional.of(Component.translatableWithFallback("vanillaskills.resourcepack.prompt", "VanillaSkills+ needs this pack to show the custom gear."))));
        });

        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer player)) return;
            PLAYERS.onLeave(player);
            DragonSet.onPlayerLeave(player.getUUID());
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.StepHeight.onLeave(player.getUUID());
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBar.forget(player);
        });

        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                PLAYERS.applyAll(player);
                // Vanilla fills the fresh body to its BASE max (20) before our max-health modifiers are
                // reapplied, so a Vitality player came back at a fraction of their bar. A death respawn
                // means full health — but only a death respawn: an end-portal return keeps its health,
                // where topping up would be a free heal.
                if (!e.isEndConquered()) player.setHealth(player.getMaxHealth());
                // The rebuilt client resets its XP display, so the shard readout went blank. An immediate
                // forced push LOSES here: vanilla marks its own lastSentExp stale on the rebuild and re-sends
                // the player's REAL experience (level 0) on the next tick, wiping anything we sent first.
                // Clearing our cache instead makes the ~10-tick reconcile re-send AFTER vanilla's zero.
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBar.forget(player);
            }
        });

        // Same client-side reset happens on a portal trip (nether or otherwise) without any respawn
        // event firing. Same race, same fix: clear the cache and let the reconcile outlast vanilla's zero.
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBar.forget(player);
            }
        });

        // Right-click a bounty board's floating-text interaction entity to open the quest GUI.
        // Right-clicks that need to beat vanilla to the punch: opening the Infusing Table, and merging
        // one Stable block into another. Placement itself is vanilla's — the blocks ARE reinforced
        // deepslate and lodestone, so a BlockItem places them correctly on its own; ShardBlockPlaceMixin
        // records the position afterwards.
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (!(e.getEntity() instanceof ServerPlayer sp) || !(e.getLevel() instanceof ServerLevel level)) return;
            // Right-clicking an enchanting table opens the Infusing Table instead.
            if (io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.INFUSING_ENABLED
                    && level.getBlockState(e.getPos()).is(net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE)
                    && !sp.isSecondaryUseActive()) {
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.infuse.InfusingMenu.open(sp, e.getPos());
                e.setCanceled(true);
                return;
            }

            ItemStack held = e.getItemStack();
            boolean stable = io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems.isStableBlock(held);
            boolean unstable = io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems.isUnstableBlock(held);
            if (!stable && !unstable) return;

            // Sneaking opts out of merging and places normally -- the same convention vanilla uses to
            // bypass a block's use action, and the only way to build two Stable blocks side by side.
            if (stable && !sp.isSecondaryUseActive()
                    && SHARDS.kindAt(level, e.getPos()) == io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBlocks.Kind.STABLE) {
                if (SHARDS.merge(level, e.getPos())) {
                    if (!sp.hasInfiniteMaterials()) held.shrink(1);
                    sp.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(sp,
                            "vanillaskills.msg.shard_block_merged", "Merged — area of effect widened (%d/%d).",
                            SHARDS.mergeCountAt(level, e.getPos()), io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBlocks.maxMerge())));
                } else {
                    sp.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(sp,
                            "vanillaskills.msg.shard_block_full", "This block is already fully merged."))
                            .withStyle(net.minecraft.ChatFormatting.RED));
                }
                e.setCanceled(true);
                return;
            }

            // Placement is left entirely to vanilla BlockItem#place, and ShardBlockPlaceMixin records the
            // position afterwards. Hand-rolling it here meant re-implementing every vanilla placement rule:
            // it ignored that right-clicking an interactable block should OPEN it, so aiming at a crafting
            // table with a shard block in hand buried the table instead of opening it.
            return;
        });

        // Right-click a held Unstable Skill Shard to bank the whole stack again, or a crate to open it.
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> {
            if (e.getEntity() instanceof ServerPlayer sp
                    && (io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBank.deposit(sp, e.getItemStack())
                        || io.github.andrewwwwwwwwwwwwwww.vanillaskills.crate.Crates.open(sp, e.getItemStack()))) {
                e.setCanceled(true);
            }
        });

        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                    && e.getEntity() instanceof ServerPlayer sp
                    && e.getTarget() instanceof net.minecraft.world.entity.Interaction
                    && e.getTarget().entityTags().contains(
                            io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.BountyBoards.TAG)) {
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui.QuestMenu.open(sp);
                e.setCanceled(true);
            }
        });

        // Bounty board kill-tracking + dragon scale drops.
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent e) -> {
            var entity = e.getEntity();
            if (e.getSource().getEntity() instanceof ServerPlayer killer) {
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Quests.onKill(killer, entity);
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Feats.onKill(killer, entity);
            }
            // PLAYER kills only. THP can kill the dragon itself as part of how its End fight starts up,
            // and that must not quietly hand out scales — least of all the one-time first-kill bonus.
            if (entity instanceof EnderDragon && entity.level() instanceof ServerLevel level
                    && e.getSource().getEntity() instanceof ServerPlayer) {
                int scaleCount = STATE.claimFirstDragonKill()
                        ? io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.DRAGON_SCALE_FIRST_KILL_DROP
                        : io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.DRAGON_SCALE_DROP;
                if (scaleCount > 0) {
                    ItemStack scales = DragonScale.create();
                    scales.setCount(scaleCount);
                    ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY() + 1.0, entity.getZ(), scales);
                    level.addFreshEntity(drop);
                }
            }
        });

        // Deepslate gate (cancel = can't break) + Cultivator bonus crops on a successful break.
        NeoForge.EVENT_BUS.addListener((BreakBlockEvent e) -> {
            // Shard blocks and generated ore: we take over entirely, so vanilla neither drops the plain
            // base block nor (for the ore) drops nothing at all.
            if (e.getLevel() instanceof ServerLevel shardLevel) {
                boolean isShardBlock = SHARDS.kindAt(shardLevel, e.getPos()) != null;
                boolean isOre = io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardOre.isOre(shardLevel, e.getPos(), e.getState());
                if (isShardBlock || isOre) {
                    // Below Crystalline the two kinds behave differently, deliberately:
                    //   Stable   — refuses to break and says why. It is expensive and its aura is
                    //              infrastructure, so losing one to a diamond pick would be a bad surprise.
                    //   Unstable — breaks and drops nothing, silently, like vanilla ore mined with too weak
                    //              a pickaxe. Losing the block IS the feedback. Generated ore matches it.
                    if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBlocks.canMine(e.getPlayer())) {
                        boolean stable = isShardBlock && SHARDS.kindAt(shardLevel, e.getPos())
                                == io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBlocks.Kind.STABLE;
                        if (stable) {
                            if (e.getPlayer() instanceof ServerPlayer gateSp) {
                                gateSp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                                        Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(gateSp, "vanillaskills.msg.shard_block_tool",
                                                "You need a Crystalline or better pickaxe to mine this."))
                                                .withStyle(net.minecraft.ChatFormatting.RED)));
                            }
                            e.setCanceled(true);
                            return;
                        }
                        if (isShardBlock) {
                            SHARDS.onBroken(shardLevel, e.getPos(), false); // destroyed, nothing dropped
                        } else {
                            shardLevel.removeBlock(e.getPos(), false);
                        }
                        e.setCanceled(true);
                        return;
                    }
                    if (isShardBlock) {
                        SHARDS.onBroken(shardLevel, e.getPos(), !e.getPlayer().hasInfiniteMaterials());
                    } else {
                        if (!e.getPlayer().hasInfiniteMaterials()) {
                            ItemStack oreDrop = io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems.unstableShard();
                            oreDrop.setCount(io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.SHARD_ORE_DROP);
                            net.minecraft.world.level.block.Block.popResource(shardLevel, e.getPos(), oreDrop);
                        }
                        shardLevel.removeBlock(e.getPos(), false);
                    }
                    e.setCanceled(true);
                    return;
                }
                // Spawners drop an Unstable Skill Shard Block instead of the experience they used to give.
                if (io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.SPAWNER_DROPS_SHARD_BLOCK
                        && e.getState().is(net.minecraft.world.level.block.Blocks.SPAWNER)
                        && !e.getPlayer().hasInfiniteMaterials()) {
                    net.minecraft.world.level.block.Block.popResource(shardLevel, e.getPos(),
                            io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems.unstableBlock());
                }
            }
            if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.DeepslateGate.canBreak(e.getPlayer(), e.getState())) {
                e.setCanceled(true);
                return;
            }
            // Fortune IV/V ore boost: one guaranteed extra base drop roll per level above III.
            if (e.getLevel() instanceof ServerLevel fbLevel && e.getPlayer() instanceof ServerPlayer fbSp) {
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.FortuneBoost.onBreak(fbLevel, fbSp, e.getPos(), e.getState());
            }
            // Hard work pays: every break that survived the gates above rolls the rare task-shard chance.
            // The shard-block/ore paths cancel and return before this line, so they never roll — matching
            // Fabric, where a cancelled BEFORE stops the AFTER event. Placement rolls from
            // TaskShardPlaceMixin, and TaskShards itself enforces the per-player cooldown.
            if (e.getLevel() instanceof ServerLevel tsLevel && e.getPlayer() instanceof ServerPlayer tsSp) {
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.TaskShards.roll(tsLevel, tsSp, e.getPos());
            }
            // Cultivator skill: bonus crops when harvesting a mature crop. Each Cultivator level rolls an
            // independent ~50% chance for one extra crop, so the bonus scales clearly with level — at max
            // (5) you average ~2.5 extra per crop, and at level 1 ~0.5.
            if (!(e.getLevel() instanceof ServerLevel level) || !(e.getPlayer() instanceof ServerPlayer sp)) return;
            net.minecraft.world.item.Item product =
                    io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Farming.matureCropProduct(level, e.getPos(), e.getState());
            if (product == null) return;
            int farmLevel = io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.CraftingGate.farmingLevel(sp);
            if (farmLevel <= 0) return;
            int bonus = 0;
            for (int i = 0; i < farmLevel; i++) {
                if (sp.getRandom().nextFloat() < 0.5f) bonus++;
            }
            bonus = Math.min(bonus, io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Farming.bonusCap(e.getState()));
            if (bonus > 0) {
                net.minecraft.world.level.block.Block.popResource(level, e.getPos(), new ItemStack(product, bonus));
            }
        });

        // Hardwood swords & axes inflict a little poison on hit.
        NeoForge.EVENT_BUS.addListener((LivingDamageEvent.Post e) -> {
            if (e.getInflictedDamage() <= 0.0f) return;
            if (!(e.getSource().getEntity() instanceof ServerPlayer attacker)) return;
            ItemStack weapon = attacker.getMainHandItem();
            if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers.has(weapon,
                    io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers.HARDWOOD.markerKey)) return;
            if (!(weapon.is(net.minecraft.world.item.Items.STONE_SWORD)
                    || weapon.is(net.minecraft.world.item.Items.STONE_AXE))) return;
            e.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0)); // Poison I, 2s
        });

        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> onServerTick(e.getServer()));

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> {
            var dispatcher = e.getDispatcher();
            SkillCommands.register(dispatcher);
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.command.HelpCommand.register(dispatcher);
            var questsNode = dispatcher.register(net.minecraft.commands.Commands.literal("quests")
                    .executes(ctx -> {
                        io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui.QuestMenu.open(ctx.getSource().getPlayerOrException());
                        return 1;
                    })
                    .then(net.minecraft.commands.Commands.literal("board")
                            .requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                            .executes(ctx -> { BOARDS.place(ctx.getSource().getPlayerOrException()); return 1; })
                            .then(net.minecraft.commands.Commands.literal("remove")
                                    .executes(ctx -> { BOARDS.removeNear(ctx.getSource().getPlayerOrException()); return 1; }))
                            .then(net.minecraft.commands.Commands.literal("refresh")
                                    .executes(ctx -> {
                                        int n = BOARDS.refreshAll(ctx.getSource().getServer());
                                        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                                "Re-rendered " + n + " bounty board" + (n == 1 ? "" : "s") + "."), true);
                                        return 1;
                                    })))
                    .then(net.minecraft.commands.Commands.literal("reroll")
                            .requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                            .executes(ctx -> {
                                QUESTS.forceReroll();
                                ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                        "Bounties re-rolled."), true);
                                return 1;
                            }))
                    .then(net.minecraft.commands.Commands.literal("graduate")
                            .requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                            .then(net.minecraft.commands.Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                    .executes(ctx -> {
                                        ServerPlayer t = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                        io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Quests.forceGraduate(t);
                                        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                                t.getName().getString() + " graduated to the main bounty board."), true);
                                        return 1;
                                    })))
                    .then(net.minecraft.commands.Commands.literal("starter")
                            .requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                            .then(net.minecraft.commands.Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                    .executes(ctx -> {
                                        ServerPlayer t = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                        io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Quests.resetToStarter(t);
                                        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                                t.getName().getString() + " sent back to the starter board."), true);
                                        return 1;
                                    }))));
            dispatcher.register(net.minecraft.commands.Commands.literal("bounty").redirect(questsNode));
        });

        // Client-only wiring (keybinds, config screen); guarded so the class never loads on a server.
        if (FMLEnvironment.getDist().isClient()) {
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.client.VanillaSkillsClient.init(modBus);
        }
    }

    private void onServerTick(MinecraftServer srv) {
        tickCounter++;
        DragonSet.tick(srv);
        // Every tick: suppress the Mountaineer step-up bonus while sneaking / toggled off (safety).
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.StepHeight.tick(srv, TREE.tree());
        // Throttled (every ~2s, internally): discovery/dimension Feats + STAT-quest baselines.
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Feats.serverTick(srv);
        if (tickCounter % ELYTRA_FORGE_INTERVAL == 0) {
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonElytraForge.tick(srv);
        }
        if (tickCounter % ROSE_GOLD_INTERVAL == 0) {
            RoseGoldSet.tick(srv);
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.CrystalSet.tick(srv);
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorSetTooltips.tick(srv);
        }
        if (tickCounter % STATUS_REFRESH_INTERVAL == 0) {
            SkillTree tree = TREE.tree();
            for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
                PlayerSkillData data = PLAYERS.get(player.getUUID());
                SkillEffects.refreshStatusEffects(player, data, tree);
                // Catches pre-2.0 gear picked up from a chest after the join sweep. Costs one
                // component lookup per slot once a world has been migrated.
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.LegacyGear.sweep(player);
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.RepairMaterials.sweep(player);
                // Reveal recipes for ingredients picked up since the last check. Only awarding on join
                // meant finding your first shard showed nothing until you relogged.
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.RecipeUnlocks.sync(player);
            }
        }
        if (tickCounter % QUEST_ROTATION_INTERVAL == 0) {
            QUESTS.tick(srv);
        }
        if (tickCounter % io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.BountyBoards.REFRESH_INTERVAL == 0) {
            BOARDS.tick(srv, tickCounter);
        }
        // Self-throttling; harms hostiles inside a Stable Skill Shard Block's area.
        SHARDS.tick(srv, tickCounter);
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.NetherRoof.tick(srv, tickCounter); // self-throttling
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBar.tick(srv, tickCounter); // self-throttling
        // Every tick: the reel paces its own steps, and returns immediately when nothing is spinning.
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.crate.CrateReel.tick(srv);
    }
}
