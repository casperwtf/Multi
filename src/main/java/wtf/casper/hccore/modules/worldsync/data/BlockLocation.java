package wtf.casper.hccore.modules.worldsync.data;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

@Getter
public class BlockLocation {
    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public BlockLocation(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockLocation(Location location) {
        this(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z);
    }

    @Override
    public String toString() {
        return "BlockLocation{" +
                "world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
