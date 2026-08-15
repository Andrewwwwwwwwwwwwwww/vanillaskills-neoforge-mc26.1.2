package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Repairable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * One armor tier. Pieces are built by stamping a vanilla armor item with overriding components:
 * attribute modifiers (armor / toughness / knockback / movement speed), durability, a name, a
 * marker (for effects + identification), a repair list, and a model hook for resource packs.
 */
public class ArmorTier {
    public final String id;
    public final String displayName;
    public final int nameColor;
    public final String markerKey;
    private final Item[] baseItems;       // indexed by ArmorPiece.ordinal()
    // The five numeric fields below are the tier's BALANCE, and are the only mutable state here: they are
    // overwritten from gameplay.json on every config load (see GameplayConfig#applyGear). Everything else
    // about a tier — base items, materials, repair rules, lore — is structural and stays in code.
    private int[] armor;                  // per piece
    private double toughness;             // per piece
    private double knockback;             // per piece
    private double perPieceSpeed;         // ADD_MULTIPLIED_BASE per piece (sums across set)
    private int[] durability;             // per piece
    /** The as-shipped values, so gameplay.json can be written with real defaults and reset cleanly. */
    public final int[] defaultArmor;
    public final double defaultToughness;
    public final double defaultKnockback;
    public final double defaultSpeed;
    public final int[] defaultDurability;
    private final HolderSet<Item> repairItems;
    public final Predicate<ItemStack> material;
    private final Supplier<ItemLore> staticLore; // optional description (e.g. set bonus), nullable

    public ArmorTier(String id, String displayName, int nameColor, String markerKey,
                     Item[] baseItems, int[] armor, double toughness, double knockback,
                     double perPieceSpeed, int[] durability, HolderSet<Item> repairItems,
                     Predicate<ItemStack> material, Supplier<ItemLore> staticLore) {
        this.id = id;
        this.displayName = displayName;
        this.nameColor = nameColor;
        this.markerKey = markerKey;
        this.baseItems = baseItems;
        this.armor = armor;
        this.toughness = toughness;
        this.knockback = knockback;
        this.perPieceSpeed = perPieceSpeed;
        this.durability = durability;
        this.defaultArmor = armor.clone();
        this.defaultToughness = toughness;
        this.defaultKnockback = knockback;
        this.defaultSpeed = perPieceSpeed;
        this.defaultDurability = durability.clone();
        this.repairItems = repairItems;
        this.material = material;
        this.staticLore = staticLore;
    }

    /**
     * Overwrite this tier's balance from config. Null or wrong-length arrays keep the current values, so a
     * partly-filled gameplay.json tunes only what it names.
     *
     * <p>⚠ Applies to gear built <b>from now on</b>. Existing pieces carry their stats baked into their own
     * components; {@link LegacyGear} is what brings those forward.
     */
    public void tune(int[] armor, Double toughness, Double knockback, Double speed, int[] durability) {
        if (armor != null && armor.length == 4) this.armor = armor.clone();
        if (toughness != null) this.toughness = toughness;
        if (knockback != null) this.knockback = knockback;
        if (speed != null) this.perPieceSpeed = speed;
        if (durability != null && durability.length == 4) this.durability = durability.clone();
    }

    /** Which piece of this tier the stack is, by base item, or null if it is not one of ours. */
    public ArmorPiece pieceOf(ItemStack stack) {
        for (ArmorPiece piece : ArmorPiece.values()) {
            if (stack.is(baseItems[piece.ordinal()])) return piece;
        }
        return null;
    }

    public int armor(ArmorPiece piece) { return armor[piece.ordinal()]; }
    public int durability(ArmorPiece piece) { return durability[piece.ordinal()]; }
    public double toughness() { return toughness; }
    public double knockback() { return knockback; }
    public double perPieceSpeed() { return perPieceSpeed; }

    public boolean isWorn(ItemStack stack) {
        return Markers.has(stack, markerKey);
    }

    public ItemStack create(ArmorPiece piece) {
        int i = piece.ordinal();
        ItemStack stack = new ItemStack(baseItems[i]);
        Markers.stamp(stack, markerKey, "vanillaskills:" + id + "_" + piece.lower(),
                Markers.name("vanillaskills.gear." + id + "." + piece.lower(),
                        displayName + " " + pieceWord(piece), nameColor));
        stack.set(DataComponents.REPAIRABLE, new Repairable(repairItems));
        applyStats(stack, piece);
        if (staticLore != null) {
            stack.set(DataComponents.LORE, staticLore.get());
        }

        // Give each tier its own worn-armor equipment asset (vanillaskills:<id>) so resource packs
        // can retexture the worn armour per tier. Keeps the base item's slot + equip sound.
        net.minecraft.world.item.equipment.Equippable baseEquippable = stack.get(DataComponents.EQUIPPABLE);
        if (baseEquippable != null) {
            stack.set(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable
                    .builder(baseEquippable.slot())
                    .setEquipSound(baseEquippable.equipSound())
                    .setAsset(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
                            Identifier.fromNamespaceAndPath("vanillaskills", id)))
                    .build());
        }
        return stack;
    }

    /**
     * Write this tier's current durability and attribute modifiers onto a stack.
     *
     * <p>Split out of {@link #create} so an existing piece can be brought onto retuned numbers without
     * being rebuilt — rebuilding would discard its enchantments, its damage and any anvil rename. Only
     * {@code MAX_DAMAGE} and {@code ATTRIBUTE_MODIFIERS} are touched; the existing damage is clamped so a
     * lowered durability cannot leave a piece more damaged than it is now allowed to be.
     */
    public void applyStats(ItemStack stack, ArmorPiece piece) {
        int i = piece.ordinal();
        stack.set(DataComponents.MAX_DAMAGE, durability[i]);
        if (stack.getDamageValue() > durability[i] - 1) {
            stack.setDamageValue(Math.max(0, durability[i] - 1));
        }

        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        b.add(Attributes.ARMOR, modifier(piece, "armor", armor[i], AttributeModifier.Operation.ADD_VALUE), piece.group);
        if (toughness > 0) {
            b.add(Attributes.ARMOR_TOUGHNESS, modifier(piece, "toughness", toughness, AttributeModifier.Operation.ADD_VALUE), piece.group);
        }
        if (knockback > 0) {
            b.add(Attributes.KNOCKBACK_RESISTANCE, modifier(piece, "knockback", knockback, AttributeModifier.Operation.ADD_VALUE), piece.group);
        }
        if (perPieceSpeed != 0) {
            b.add(Attributes.MOVEMENT_SPEED, modifier(piece, "speed", perPieceSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), piece.group);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());
    }

    private AttributeModifier modifier(ArmorPiece piece, String attr, double amount, AttributeModifier.Operation op) {
        Identifier id = Identifier.fromNamespaceAndPath("vanillaskills", this.id + "." + piece.lower() + "." + attr);
        return new AttributeModifier(id, amount, op);
    }

    private static String pieceWord(ArmorPiece piece) {
        return switch (piece) {
            case HELMET -> "Helmet";
            case CHESTPLATE -> "Chestplate";
            case LEGGINGS -> "Leggings";
            case BOOTS -> "Boots";
        };
    }
}
