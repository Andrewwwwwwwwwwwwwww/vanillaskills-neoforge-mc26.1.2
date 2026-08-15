package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The full skill tree: a set of lanes (categories), each holding nodes positioned within that
 * lane's view. Older trees with no categories are migrated into a single "general" lane.
 */
public class SkillTree {
    public int version = 1;
    public String title = "Skills";
    public int rows = 6;
    public List<SkillCategory> categories = new ArrayList<>();
    public List<SkillNode> nodes = new ArrayList<>();

    public static final String ROOT_ID = "root";
    public static final String DEFAULT_CATEGORY = "general";

    private transient Map<String, SkillNode> byId = new LinkedHashMap<>();
    private transient Map<String, SkillCategory> categoryById = new LinkedHashMap<>();
    private transient Map<Integer, SkillCategory> categoryBySlot = new LinkedHashMap<>();
    private transient Map<String, List<SkillNode>> nodesByCategory = new LinkedHashMap<>();

    /** Rebuild lookups; auto-create lanes for any referenced-but-undeclared category. */
    public void index() {
        if (categories == null) categories = new ArrayList<>();
        if (nodes == null) nodes = new ArrayList<>();
        byId = new LinkedHashMap<>();
        categoryById = new LinkedHashMap<>();
        categoryBySlot = new LinkedHashMap<>();
        nodesByCategory = new LinkedHashMap<>();

        for (SkillCategory cat : categories) {
            if (cat == null || cat.id == null) continue;
            cat.normalize();
            categoryById.put(cat.id, cat);
        }

        for (SkillNode node : nodes) {
            if (node == null || node.id == null) continue;
            node.normalize();
            byId.put(node.id, node);
            String catId = (node.category != null && categoryById.containsKey(node.category))
                    ? node.category : DEFAULT_CATEGORY;
            node.category = catId;
            nodesByCategory.computeIfAbsent(catId, k -> new ArrayList<>()).add(node);
        }

        // Make sure every lane that has nodes exists as a category (migration / safety).
        for (String catId : new ArrayList<>(nodesByCategory.keySet())) {
            if (!categoryById.containsKey(catId)) {
                SkillCategory cat = new SkillCategory(catId, capitalize(catId), "minecraft:book", nextFreeSlot());
                categories.add(cat);
                categoryById.put(catId, cat);
            }
        }
        for (SkillCategory cat : categories) {
            categoryBySlot.put(cat.slot, cat);
        }
    }

    private int nextFreeSlot() {
        for (int i = 0; i < slotCount(); i++) {
            boolean taken = false;
            for (SkillCategory c : categories) {
                if (c.slot == i) { taken = true; break; }
            }
            if (!taken) return i;
        }
        return 0;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public SkillNode byId(String id) {
        return byId.get(id);
    }

    public boolean has(String id) {
        return byId.containsKey(id);
    }

    public List<SkillCategory> categories() {
        return categories;
    }

    public SkillCategory category(String id) {
        return categoryById.get(id);
    }

    public SkillCategory categoryAtSlot(int slot) {
        return categoryBySlot.get(slot);
    }

    public List<SkillNode> nodesIn(String categoryId) {
        return nodesByCategory.getOrDefault(categoryId, List.of());
    }

    public SkillNode nodeInCategoryAtSlot(String categoryId, int slot) {
        for (SkillNode node : nodesIn(categoryId)) {
            if (node.slot == slot) return node;
        }
        return null;
    }

    public int size() {
        return nodes.size();
    }

    public int clampedRows() {
        return Math.max(1, Math.min(6, rows));
    }

    public int slotCount() {
        return clampedRows() * 9;
    }
}
