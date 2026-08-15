package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

/**
 * A single effect granted by a skill node. Deserialized directly from JSON by Gson.
 *
 * type = "attribute":     uses {@link #attribute}, {@link #operation}, {@link #amount}
 * type = "status_effect": uses {@link #effect}, {@link #amplifier}
 * type = "flag":          uses {@link #name}
 */
public class SkillEffect {
    public String type = "attribute";

    // attribute
    public String attribute;
    public String operation = "add_value";
    public double amount;

    // status_effect
    public String effect;
    public int amplifier = 0;

    // flag
    public String name;

    public SkillEffect() {}

    public static SkillEffect attribute(String attribute, String operation, double amount) {
        SkillEffect e = new SkillEffect();
        e.type = "attribute";
        e.attribute = attribute;
        e.operation = operation;
        e.amount = amount;
        return e;
    }

    public static SkillEffect flag(String name) {
        SkillEffect e = new SkillEffect();
        e.type = "flag";
        e.name = name;
        return e;
    }

    public static SkillEffect status(String effect, int amplifier) {
        SkillEffect e = new SkillEffect();
        e.type = "status_effect";
        e.effect = effect;
        e.amplifier = amplifier;
        return e;
    }
}
