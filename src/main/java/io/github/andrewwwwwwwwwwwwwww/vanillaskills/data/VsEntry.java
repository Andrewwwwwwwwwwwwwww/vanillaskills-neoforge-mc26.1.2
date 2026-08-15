package io.github.andrewwwwwwwwwwwwwww.vanillaskills.data;

/**
 * One entry loaded from a datapack by {@link VsJsonLoader}.
 *
 * <p>Implementations are plain mutable classes with public fields, deserialized directly by Gson —
 * the same pattern the mod already uses for {@code SkillNode}, {@code SkillCategory} and the config
 * classes. Gson leaves absent fields at their declared defaults and nulls out absent objects, so
 * {@link #normalize()} is where defaults are filled in and invalid entries are rejected.
 */
public interface VsEntry {

    /** This entry's unique id within its content type. A later pack redeclaring the same id wins. */
    String id();

    /**
     * Fill in defaults and validate. Called once per entry immediately after parsing.
     *
     * @return false to reject the entry — the loader logs which file it came from and skips it,
     *         rather than letting a malformed entry reach gameplay code.
     */
    default boolean normalize() {
        return true;
    }
}
