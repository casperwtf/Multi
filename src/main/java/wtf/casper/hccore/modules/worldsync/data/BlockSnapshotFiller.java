package wtf.casper.hccore.modules.worldsync.data;

import wtf.casper.amethyst.core.distributedworkload.Workload;

public class BlockSnapshotFiller implements Workload {

    private final BlockSnapshot blockSnapshot;

    public BlockSnapshotFiller(BlockSnapshot blockSnapshot) {
        this.blockSnapshot = blockSnapshot;
    }

    @Override
    public void compute() {
        blockSnapshot.set();
    }
}
