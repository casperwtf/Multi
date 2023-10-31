package wtf.casper.hccore.modules.worldsync.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import wtf.casper.amethyst.core.mq.Message;
import wtf.casper.amethyst.libs.storageapi.id.Id;
import wtf.casper.hccore.HCCore;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@Getter @Log @EqualsAndHashCode @ToString
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
        location.getLocation().getWorld().getBlockAt(location.getLocation()).setBlockData(blockData1);
    }
}
