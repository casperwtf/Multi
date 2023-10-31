package wtf.casper.hccore.modules.worldsync.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import wtf.casper.amethyst.core.obj.Pair;
import wtf.casper.amethyst.libs.boostedyaml.block.implementation.Section;
import wtf.casper.amethyst.paper.scheduler.SchedulerUtil;
import wtf.casper.amethyst.paper.utils.BungeeUtil;
import wtf.casper.hccore.HCCore;
import wtf.casper.hccore.modules.worldsync.WorldManager;

import java.util.*;

@Getter @EqualsAndHashCode @ToString
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
        return (x >= minX - radius && x <= maxX + radius)
                || (z >= minZ - radius && z <= maxZ + radius);
    }

    public boolean aroundBorderWithin(int x, int z) {
        return aroundBorderWithin(x, z, WorldManager.RENDER_BLOCKS);
    }

    public boolean aroundBorderWithin(int x, int z, int radius) {
        return aroundBorder(x, z, radius) && withinBorder(x, z);
    }

    public void tp(Player player) {
        BungeeUtil.sendPlayerToServer(player, name);
    }

    public void loadBorderChunks() {

        Map<UUID, Set<Pair<Integer, Integer>>> chunks = new HashMap<>();

        for (World world : Bukkit.getWorlds()) {
            chunks.put(world.getUID(), new HashSet<>());

            Set<Pair<Integer, Integer>> set = chunks.get(world.getUID());

            for (int x = minX; x <= maxX; x += 16) {
                for (int z = minZ; z <= maxZ; z += 16) {
                    if (!aroundBorderWithin(x, z)) {
                        continue;
                    }

                    int chunkX = x >> 4;
                    int chunkZ = z >> 4;

                    if (world.getEnvironment() == World.Environment.NETHER) {
                        continue;
//                        chunkX /= 8;
//                        chunkZ /= 8;
                    }

                    if (world.isChunkLoaded(chunkX, chunkZ)) {
                        continue;
                    }

                    set.add(Pair.of(chunkX, chunkZ));
                }
            }
        }

        if (chunks.isEmpty()) {
            return;
        }

        for (UUID worldUid : chunks.keySet()) {
            World world = Bukkit.getWorld(worldUid);

            Set<Pair<Integer, Integer>> set = chunks.get(worldUid);
            if (set.isEmpty()) {
                continue;
            }

            HCCore plugin = HCCore.getPlugin(HCCore.class);
            Iterator<Pair<Integer, Integer>> iterator = set.iterator();

            for (int i = 0; i < Math.min(1.0, (double) chunks.size() / 10); i++) {
                Pair<Integer, Integer> next = iterator.next();
                Location location = world.getBlockAt(next.getFirst() * 16, 0, next.getSecond() * 16).getLocation();

                SchedulerUtil.runLater(() -> {
                    if (!world.getPluginChunkTickets(next.getFirst(), next.getSecond()).contains(plugin)) {
                        world.loadChunk(next.getFirst(), next.getSecond());
                        world.addPluginChunkTicket(next.getFirst(), next.getSecond(), plugin);
                    }
                }, location, i * 20L * 5);
            }
        }
    }
}
