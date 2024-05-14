package wtf.casper.multi.modules.worldsync.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.multi.modules.worldsync.WorldManager;
import wtf.casper.multi.modules.worldsync.data.ServerBasedWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TeleportUtil {

    private static WorldManager WORLD_MANAGER = Inject.get(WorldManager.class);

    private final static Map<UUID, String> teleporting = new HashMap<>();

    public static boolean isTeleporting(Player player) {
        return teleporting.containsKey(player.getUniqueId());
    }

    public static boolean isTeleporting(UUID player) {
        return teleporting.containsKey(player);
    }

    public static String getTeleportingWorld(Player player) {
        return teleporting.get(player.getUniqueId());
    }

    public static String getTeleportingWorld(UUID player) {
        return teleporting.get(player);
    }

    public static void markTeleporting(Player player, String worldName) {
        teleporting.put(player.getUniqueId(), worldName);
    }

    public static void teleportPlayer(Player player, Location location) {
        if (WORLD_MANAGER.getWorld().withinBorder(location.getBlockX(), location.getBlockZ())) {
            player.teleport(location);
            return;
        }

        Optional<ServerBasedWorld> world = WORLD_MANAGER.getWorld(location.getBlockX(), location.getBlockZ());
        if (world.isEmpty()) {
            throw new IllegalStateException("World not found for location: " + location);
        }

        WORLD_MANAGER.markToNotSaveLocation(player);
        WORLD_MANAGER.setLastLocation(player, location);

        ServerBasedWorld basedWorld = world.get();
        basedWorld.tp(player);
    }
}
