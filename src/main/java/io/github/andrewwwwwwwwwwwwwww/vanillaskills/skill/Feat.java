package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

/**
 * A one-time achievement ("Feat"): discover a structure, defeat a boss, or first enter a dimension.
 * Awarded automatically the first time the condition is met, then shown ticked on the Feats tab.
 * Unlike bounty quests, feats never rotation-reset and can each be earned only once per player.
 *
 * @param type      DISCOVER (be inside a structure), KILL (slay an entity type), or DIMENSION (first
 *                  time in a dimension — used as the Hungering-Portal-gated "enter the End" feat).
 * @param target    structure id (DISCOVER), entity-type id (KILL), or dimension id (DIMENSION).
 * @param dimension for DISCOVER, the dimension the check is restricted to (cheap gate); else null.
 * @param icon      item id shown in the Feats GUI.
 */
public record Feat(String id, Type type, String target, String dimension, int reward, String icon,
                   String title, String desc) {
    public enum Type { DISCOVER, KILL, DIMENSION }
}
