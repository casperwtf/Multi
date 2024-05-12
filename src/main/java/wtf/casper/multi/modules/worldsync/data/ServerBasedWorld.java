package wtf.casper.multi.modules.worldsync.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import wtf.casper.amethyst.core.obj.Pair;
import wtf.casper.amethyst.libs.boostedyaml.block.implementation.Section;
import wtf.casper.amethyst.paper.scheduler.SchedulerUtil;
import wtf.casper.amethyst.paper.utils.BungeeUtil;
import wtf.casper.multi.Multi;
import wtf.casper.multi.modules.worldsync.WorldManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Getter
@EqualsAndHashCode
@ToString
@Log
public class ServerBasedWorld {
    private final String name;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final boolean isMaster;

    public ServerBasedWorld(String name, int minX, int minZ, int maxX, int maxZ, boolean isMaster) {
        this.name = name;
        this.minX = Math.min(minX, maxX);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxZ = Math.max(minZ, maxZ);
        this.isMaster = isMaster;
    }

    public ServerBasedWorld(String key, Section section) {
        this(key, section.getInt("min-x"), section.getInt("min-z"), section.getInt("max-x"), section.getInt("max-z"), section.getBoolean("master", false));
    }

    public boolean outsideBorder(int x, int z) {
        return !withinBorder(x, z);
    }

    public boolean withinBorder(int x, int z) {
        return minX <= x
                && minZ <= z
                && maxX >= x
                && maxZ >= z;
    }

    public boolean aroundBorder(int x, int z) {
        return aroundBorder(x, z, WorldManager.RENDER_BLOCKS);
    }

    public boolean aroundBorder(int x, int z, int radius) {
        return (x >= minX - radius && x <= maxX + radius)
                || (z >= minZ - radius && z <= maxZ + radius);
    }

    public boolean aroundBorderWithin(int x, int z) {
        return aroundBorderWithin(x, z, WorldManager.RENDER_BLOCKS);
    }

    public boolean aroundBorderWithin(int x, int z, int radius) {
        return withinBorder(x, z) && aroundBorder(x, z, radius);
    }

    public void tp(Player player) {
        BungeeUtil.sendPlayerToServer(player, name);
    }

    public CompletableFuture<Void> loadBorderChunks() {
        List<CompletableFuture<Void>> chunks = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NETHER) {
                continue;
            }

            for (int x = minX; x <= maxX; x += 16) {
                for (int z = minZ; z <= maxZ; z += 16) {

                    if (!aroundBorderWithin(x, z)) { // sanity check
                        continue;
                    }

                    int chunkX = x >> 4;
                    int chunkZ = z >> 4;

                    chunks.add(world.getChunkAtAsync(chunkX, chunkZ, true).handle((chunk, throwable) -> {
                        if (throwable != null) {
                            log.warning("Failed to load chunk " + chunkX + ", " + chunkZ + " in world " + world.getName());
                            throwable.printStackTrace();
                            return null;
                        }

                        chunk.setForceLoaded(true);

                        return null;
                    }));
                }
            }
        }

        return CompletableFuture.allOf(chunks.toArray(new CompletableFuture[0]));
    }
}
