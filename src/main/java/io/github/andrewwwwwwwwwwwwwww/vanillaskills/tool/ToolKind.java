package io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool;

import java.util.List;

/**
 * The five tool kinds, each with its display word and the crafting-grid shape(s) that make it
 * (material cells + stick cells, row-major over the recipe's trimmed bounding box). Axe and hoe
 * list both orientations since shaped recipes are mirrorable.
 */
public enum ToolKind {
    PICKAXE("Pickaxe", List.of(
            new Shape(3, 3, new int[]{0, 1, 2}, new int[]{4, 7}))),
    AXE("Axe", List.of(
            new Shape(2, 3, new int[]{0, 1, 2}, new int[]{3, 5}),
            new Shape(2, 3, new int[]{0, 1, 3}, new int[]{2, 4}))),
    SHOVEL("Shovel", List.of(
            new Shape(1, 3, new int[]{0}, new int[]{1, 2}))),
    HOE("Hoe", List.of(
            new Shape(2, 3, new int[]{0, 1}, new int[]{3, 5}),
            new Shape(2, 3, new int[]{0, 1}, new int[]{2, 4}))),
    SWORD("Sword", List.of(
            new Shape(1, 3, new int[]{0, 1}, new int[]{2}))),
    SPEAR("Spear", List.of(
            new Shape(3, 3, new int[]{2}, new int[]{4, 6}),   // tip top-right, shaft down-left
            new Shape(3, 3, new int[]{0}, new int[]{4, 8})));  // mirror: tip top-left, shaft down-right

    public final String word;
    public final List<Shape> shapes;

    ToolKind(String word, List<Shape> shapes) {
        this.word = word;
        this.shapes = shapes;
    }

    public String lower() {
        return name().toLowerCase();
    }

    public record Shape(int width, int height, int[] mat, int[] stick) {
        public boolean isMat(int i) {
            return contains(mat, i);
        }

        public boolean isStick(int i) {
            return contains(stick, i);
        }

        private static boolean contains(int[] arr, int v) {
            for (int x : arr) if (x == v) return true;
            return false;
        }
    }
}
