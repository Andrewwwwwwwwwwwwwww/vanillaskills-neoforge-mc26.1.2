package io.github.andrewwwwwwwwwwwwwww.vanillaskills.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only wiring. Registers two rebindable keys (Options → Controls → "VanillaSkills") that
 * open the Skill Tree and the Bounty Board.
 *
 * <p>The GUIs are server-side chest menus, so the keys simply run the server's {@code /skill} and
 * {@code /quests} commands. They only fire when the connected server actually declares those commands
 * (the command tree is synced to the client) — i.e. only on a server running VanillaSkills — so on a
 * vanilla / non-mod server the keys do nothing instead of spamming "Unknown command".
 */
public final class VanillaSkillsClient {
    private VanillaSkillsClient() {}

    private static KeyMapping openSkills;
    private static KeyMapping openQuests;

    public static void init(IEventBus modBus) {
        // Load global client preferences (e.g. the opt-in narrator disable).
        ClientConfig.load();

        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            KeyMapping.Category category = KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath("vanillaskills", "keybinds"));

            // Defaults: ] for the skill tree, [ for the bounty board — both unbound in vanilla, unlike the
            // commonly-used B/V. Players can rebind under Options -> Controls -> VanillaSkills.
            openSkills = new KeyMapping("key.vanillaskills.open_skills", GLFW.GLFW_KEY_RIGHT_BRACKET, category);
            openQuests = new KeyMapping("key.vanillaskills.open_quests", GLFW.GLFW_KEY_LEFT_BRACKET, category);
            e.register(openSkills);
            e.register(openQuests);
        });

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            if (openSkills == null || openQuests == null) return;
            Minecraft client = Minecraft.getInstance();
            while (openSkills.consumeClick()) runServerCommand(client, "skill");
            while (openQuests.consumeClick()) runServerCommand(client, "quests");
        });
    }

    private static void runServerCommand(Minecraft client, String command) {
        if (client.player == null) return;
        ClientPacketListener connection = client.getConnection();
        if (connection == null) return;
        // Only send if the connected server declares this command (i.e. it's running VanillaSkills).
        if (connection.getCommands().getRoot().getChild(command) == null) return;
        connection.sendCommand(command);
    }
}
