package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Progression gate: deepslate and its ore variants can only be broken with a Steel-tier or better
 * pickaxe (Steel, Diamond, Crystalline, Netherite, Dragon) — iron and below can't. This forces
 * players to unlock the Toolsmith Steel node before mining the deep layer.
 */
public final class DeepslateGate {
    private DeepslateGate() {}

    private static final Set<Block> DEEPSLATE = Set.of(
            Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE,
            Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);

    public static boolean isDeepslate(BlockState state) {
        return DEEPSLATE.contains(state.getBlock());
    }

    /** A Steel-tier-or-better pickaxe: vanilla diamond/netherite, or a custom Steel/Crystal/Dragon tool. */
    public static boolean qualifies(ItemStack stack) {
        if (stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.NETHERITE_PICKAXE)) return true;
        return Markers.has(stack, ToolTiers.STEEL.markerKey)
                || Markers.has(stack, ToolTiers.CRYSTAL.markerKey)
                || Markers.has(stack, ToolTiers.DRAGON.markerKey);
    }

    /** True if this player may break the given block; messages them (action bar) when blocked. */
    public static boolean canBreak(Player player, BlockState state) {
        if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.DEEPSLATE_GATE) return true;
        if (player.isCreative() || !isDeepslate(state)) return true;
        if (qualifies(player.getMainHandItem())) return true;
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                    Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(sp,"vanillaskills.msg.deepslate","You need a Steel-tier or better pickaxe to mine deepslate."))
                            .withStyle(ChatFormatting.RED)));
        }
        return false;
    }
}
