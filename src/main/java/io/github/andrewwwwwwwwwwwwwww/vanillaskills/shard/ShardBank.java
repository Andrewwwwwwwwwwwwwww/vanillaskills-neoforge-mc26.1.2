package io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Moves Skill Shards between the player's balance and physical {@link ShardItems#unstableShard()} items.
 *
 * <p>Withdrawing spends from the balance; banking returns to it. The two are exactly symmetric — see
 * {@code PlayerSkillManager.depositSkillShards} for why banking must not credit lifetime earned.
 */
public final class ShardBank {
    private ShardBank() {}

    /** How many shards one confirmed click of the withdraw button converts. Configurable per world. */
    public static int withdrawAmount() {
        return io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.SHARD_WITHDRAW_AMOUNT;
    }

    /**
     * Turn {@code amount} banked Skill Shards into physical shards in the player's inventory.
     *
     * @return true if the withdrawal happened
     */
    public static boolean withdraw(ServerPlayer player, int amount) {
        if (amount <= 0) return false;
        if (!VanillaSkills.PLAYERS.spendSkillShards(player, amount)) {
            player.sendSystemMessage(Component.literal(Lang.tr(player,
                    "vanillaskills.msg.shard_withdraw_short", "Not enough Skill Shards (need %d).", amount))
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        ItemStack stack = ShardItems.unstableShard();
        stack.setCount(amount);
        // placeItemBackInInventory drops whatever will not fit, so shards can never be destroyed by a full bag.
        player.getInventory().placeItemBackInInventory(stack);
        // Push the inventory change to the client immediately.
        //
        // Withdrawing happens with our chest GUI open, and adding to the inventory behind an open menu does
        // not mark that menu dirty — so the shard count in the screen dropped while the items appeared not to
        // arrive, and clicking quickly made it look like several had been eaten. The items were always there;
        // the client simply had not been told until the screen closed. A full resync costs nothing at this
        // rate and keeps the two in step no matter how fast the button is clicked.
        if (player.containerMenu != null) player.containerMenu.broadcastFullState();
        player.sendSystemMessage(Component.literal(Lang.tr(player,
                "vanillaskills.msg.shard_withdrawn", "Withdrew %d Skill Shard(s) as items.", amount))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /**
     * Bank <b>one</b> Unstable Skill Shard from the held stack.
     *
     * <p>Deliberately one per click rather than the whole stack: banking is irreversible in the sense that
     * getting the shards back costs another withdraw, so emptying a stack of 64 on a single misclick is a
     * mistake the player cannot undo. One at a time also mirrors the withdraw button, which hands out
     * {@link #withdrawAmount()} per confirmed click rather than everything at once.
     *
     * @return true if a shard was banked
     */
    public static boolean deposit(ServerPlayer player, ItemStack held) {
        if (!ShardItems.isUnstableShard(held) || held.isEmpty()) return false;
        held.shrink(1);
        VanillaSkills.PLAYERS.depositSkillShards(player, 1);
        player.sendSystemMessage(Component.literal(Lang.tr(player,
                "vanillaskills.msg.shard_banked", "Banked %d Skill Shard(s).", 1))
                .withStyle(ChatFormatting.AQUA));
        return true;
    }
}
