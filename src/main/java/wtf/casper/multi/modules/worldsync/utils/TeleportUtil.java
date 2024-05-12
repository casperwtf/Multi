package wtf.casper.multi.modules.worldsync.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.multi.modules.worldsync.WorldManager;
import wtf.casper.multi.modules.worldsync.data.ServerBasedWorld;

import java.util.Optional;

public class TeleportUtil {

    private static WorldManager WORLD_MANAGER = Inject.get(WorldManager.class);

    public static void teleportPlayer(Player player, Location location) {
        if (WORLD_MANAGER.getWorld().withinBorder(location.getBlockX(), location.getBlockZ())) {
            player.teleport(location);
            return;
        }

        Optional<ServerBasedWorld> world = WORLD_MANAGER.getWorld(location.getBlockX(), location.getBlockZ());
        if (world.isEmpty()) {
            throw new IllegalStateException("World not found for location: " + location);
        }

        ServerBasedWorld basedWorld = world.get();

        basedWorld.tp(player);
    }
}
