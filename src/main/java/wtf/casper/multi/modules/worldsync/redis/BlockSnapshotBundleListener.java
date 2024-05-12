package wtf.casper.multi.modules.worldsync.redis;

import com.google.auto.service.AutoService;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.amethyst.core.mq.redis.RedisListener;
import wtf.casper.multi.modules.worldsync.WorldManager;
import wtf.casper.multi.modules.worldsync.data.BlockSnapshot;
import wtf.casper.multi.modules.worldsync.data.BlockSnapshotBundle;
import wtf.casper.multi.modules.worldsync.data.BlockSnapshotFiller;
import wtf.casper.multi.packets.worldsync.WorldSyncRedisListener;

@AutoService(WorldSyncRedisListener.class)
public class BlockSnapshotBundleListener extends RedisListener<BlockSnapshotBundle> implements WorldSyncRedisListener {

    private final WorldManager worldManager = Inject.get(WorldManager.class);

    public BlockSnapshotBundleListener() {
        super(BlockSnapshotBundle.class);
    }

    @Override
    public void onMessage(BlockSnapshotBundle bundle) {
        if (bundle == null) return;
        for (BlockSnapshot snapshot : bundle.getBlockSnapshots()) {
            worldManager.getWorkloadRunnable().addWorkload(new BlockSnapshotFiller(snapshot));
        }
    }
}
