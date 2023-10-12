package wtf.casper.hccore.modules.worldsync.data;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import wtf.casper.amethyst.core.mq.Message;
import wtf.casper.amethyst.libs.storageapi.id.Id;
import wtf.casper.hccore.HCCore;

import java.util.UUID;
import java.util.logging.Level;

@Getter
public class BlockSnapshot implements Message {
    @Id
    private final UUID id = UUID.randomUUID();
    private final long snapshotTime = System.currentTimeMillis();
    private final BlockLocation location;
    private final String blockData;

    public BlockSnapshot(BlockLocation location, String blockData) {
        this.location = location;
        this.blockData = blockData;
    }

    public void set() {
        BlockData blockData1 = Bukkit.createBlockData(blockData);
        location.getLocation().getChunk().load(true);
        location.getLocation().getBlock().setBlockData(blockData1);
    }

    @Override
    public String toString() {
        return "BlockSnapshot{" +
                "id=" + id +
                ", snapshotTime=" + snapshotTime +
                ", location=" + location +
                ", blockData='" + blockData + '\'' +
                '}';
    }
}
