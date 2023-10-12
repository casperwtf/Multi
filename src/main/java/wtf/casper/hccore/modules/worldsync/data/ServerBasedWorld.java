package wtf.casper.hccore.modules.worldsync.data;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import wtf.casper.amethyst.libs.storageapi.libs.boostedyaml.block.implementation.Section;
import wtf.casper.amethyst.paper.utils.BungeeUtil;
import wtf.casper.hccore.HCCore;
import wtf.casper.hccore.modules.worldsync.WorldManager;

import java.util.List;

@Getter
public class ServerBasedWorld {
    private final String name;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;


    public ServerBasedWorld(String name, int minX, int minZ, int maxX, int maxZ) {
        this.name = name;
        this.minX = Math.min(minX, maxX);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public ServerBasedWorld(String key, Section section) {
        this(key, section.getInt("min-x"), section.getInt("min-z"), section.getInt("max-x"), section.getInt("max-z"));
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
        return radius + minX > x
                || radius + minZ > z
                || maxX - radius < x
                || maxZ - radius < z
                || radius - minX > x
                || radius - minZ > z
                || maxX + radius < x
                || maxZ + radius < z;
    }

    public boolean aroundBorderWithin(int x, int z) {
        return aroundBorderWithin(x, z, WorldManager.RENDER_BLOCKS);
    }

    public boolean aroundBorderWithin(int x, int z, int radius) {
        return radius + minX > x
                || radius + minZ > z
                || maxX - radius < x
                || maxZ - radius < z;
    }

    public void tp(Player player) {
        BungeeUtil.sendPlayerToServer(player, name);
    }

    @Override
    public String toString() {
        return "ServerBasedWorld{" +
                "name='" + name + '\'' +
                ", minX=" + minX +
                ", minZ=" + minZ +
                ", maxX=" + maxX +
                ", maxZ=" + maxZ +
                '}';
    }

    public void loadBorderChunks() {
        for (World world : Bukkit.getWorlds()) {
            for (int x = minX; x <= maxX; x += 16) {
                for (int z = minZ; z <= maxZ; z += 16) {
                    if (aroundBorderWithin(x, z, 8)) {
                        int chunkX = x >> 4;
                        int chunkZ = minZ >> 4;

                        if (world.getEnvironment() == World.Environment.NETHER) {
                            chunkX /= 8;
                            chunkZ /= 8;
                        }

                        Chunk chunkAt = world.getChunkAt(chunkX, chunkZ);
                        chunkAt.load(true);
                        chunkAt.addPluginChunkTicket(HCCore.getPlugin(HCCore.class));
                    }
                }
            }
        }
    }
}
