package wtf.casper.multi.modules.worldsync.data;

import lombok.Getter;
import wtf.casper.amethyst.core.mq.Message;

import java.util.List;

@Getter
public class BlockSnapshotBundle implements Message {
    private final List<BlockSnapshot> blockSnapshots;

    public BlockSnapshotBundle(List<BlockSnapshot> blockSnapshots) {
        this.blockSnapshots = blockSnapshots;
    }
}
