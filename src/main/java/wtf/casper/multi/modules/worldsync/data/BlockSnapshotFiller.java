package wtf.casper.multi.modules.worldsync.data;

import wtf.casper.amethyst.core.distributedworkload.Workload;

public class BlockSnapshotFiller implements Workload {

    private final BlockSnapshot blockSnapshot;

    public BlockSnapshotFiller(BlockSnapshot blockSnapshot) {
        this.blockSnapshot = blockSnapshot;
    }

    @Override
    public void compute() {
        if (!blockSnapshot.getLocation().getLocation().isChunkLoaded()) {
            blockSnapshot.getLocation().getLocation().getWorld()
                    .getChunkAtAsync(blockSnapshot.getLocation().getLocation())
                    .thenRun(blockSnapshot::set);

            return;
        }
        blockSnapshot.set();
    }
}
