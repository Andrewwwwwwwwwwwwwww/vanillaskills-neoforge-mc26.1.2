package io.github.andrewwwwwwwwwwwwwww.vanillaskills.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads one VanillaSkills content type out of every loaded datapack.
 *
 * <p>Content lives at {@code data/<namespace>/vanillaskills/<type>/<name>.json}, so any datapack —
 * including the mod's own bundled defaults — can contribute entries. Each file may be:
 *
 * <ul>
 *   <li>a tag-style object: <code>{"replace": false, "values": [ … ]}</code></li>
 *   <li>a bare array of entries</li>
 *   <li>a single entry object</li>
 * </ul>
 *
 * <p><b>Merge rules</b> mirror vanilla tags, so pack authors do not have to learn a new model.
 * Resource stacks are read lowest-priority first, and {@code "replace": true} discards what
 * lower-priority packs contributed <i>to that same file id</i> — not to the whole content type.
 * Across different files, an entry id declared later overrides an earlier one. To replace the mod's
 * built-ins wholesale, a pack overrides {@code vanillaskills:builtin.json} for that type with
 * {@code "replace": true}.
 *
 * <p><b>This never throws.</b> A malformed file or entry is logged with the file id and the pack it
 * came from, then skipped — one broken pack file must not take a server down.
 */
public final class VsJsonLoader {
    private VsJsonLoader() {}

    private static final Gson GSON = new Gson();

    /** Datapack subdirectory that holds all VanillaSkills content types. */
    public static final String ROOT = "vanillaskills";

    /**
     * Load and merge every entry of one content type.
     *
     * @param type the subdirectory under {@code vanillaskills/}, e.g. {@code "feat"}
     * @return the merged entries in declaration order; never null, possibly empty
     */
    public static <T extends VsEntry> List<T> load(ResourceManager manager, String type, Class<T> clazz) {
        FileToIdConverter converter = FileToIdConverter.json(ROOT + "/" + type);
        Map<String, T> merged = new LinkedHashMap<>();
        int rejected = 0;

        for (Map.Entry<Identifier, List<Resource>> file : converter.listMatchingResourceStacks(manager).entrySet()) {
            Identifier fileId = file.getKey();
            // One accumulator per FILE id, so "replace" carries the same meaning it does for tags.
            List<T> perFile = new ArrayList<>();

            // A resource stack is ordered lowest -> highest priority (the convention vanilla's
            // TagLoader relies on), so reading in order lets a higher-priority pack append to or
            // replace what an earlier one contributed.
            for (Resource resource : file.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonElement root = JsonParser.parseReader(reader);
                    if (readFile(root, clazz, perFile, fileId, resource.sourcePackId())) {
                        rejected++;
                    }
                } catch (Exception e) {
                    rejected++;
                    VanillaSkills.LOGGER.error("Skipping unreadable {} file {} from pack '{}'",
                            type, fileId, resource.sourcePackId(), e);
                }
            }

            for (T entry : perFile) merged.put(entry.id(), entry);
        }

        if (rejected > 0) {
            VanillaSkills.LOGGER.warn("Loaded {} '{}' entries, skipping {} malformed one(s) — see errors above",
                    merged.size(), type, rejected);
        }
        return List.copyOf(merged.values());
    }

    /** Parse one file into {@code out}. Returns true if anything in it had to be skipped. */
    private static <T extends VsEntry> boolean readFile(JsonElement root, Class<T> clazz, List<T> out,
                                                        Identifier fileId, String packId) {
        JsonArray values;
        if (root != null && root.isJsonObject() && root.getAsJsonObject().has("values")) {
            JsonObject obj = root.getAsJsonObject();
            if (obj.has("replace") && obj.get("replace").getAsBoolean()) {
                out.clear(); // this pack is authoritative for this file
            }
            JsonElement raw = obj.get("values");
            if (!raw.isJsonArray()) {
                VanillaSkills.LOGGER.error("'values' must be an array in {} (pack '{}')", fileId, packId);
                return true;
            }
            values = raw.getAsJsonArray();
        } else if (root != null && root.isJsonArray()) {
            values = root.getAsJsonArray();
        } else if (root != null && root.isJsonObject()) {
            values = new JsonArray();
            values.add(root); // convenience: a file holding one entry
        } else {
            VanillaSkills.LOGGER.error("Expected an object or array in {} (pack '{}')", fileId, packId);
            return true;
        }

        boolean skipped = false;
        for (JsonElement element : values) {
            T entry;
            try {
                entry = GSON.fromJson(element, clazz);
            } catch (Exception e) {
                VanillaSkills.LOGGER.error("Malformed entry in {} (pack '{}')", fileId, packId, e);
                skipped = true;
                continue;
            }
            if (entry == null || !entry.normalize()) {
                VanillaSkills.LOGGER.error("Rejected invalid entry in {} (pack '{}'): {}", fileId, packId, element);
                skipped = true;
                continue;
            }
            // A file that redeclares an id it already contributed replaces its own earlier entry.
            out.removeIf(existing -> existing.id().equals(entry.id()));
            out.add(entry);
        }
        return skipped;
    }
}
