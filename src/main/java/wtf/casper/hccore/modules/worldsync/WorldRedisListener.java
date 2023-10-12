package wtf.casper.hccore.modules.worldsync;

import wtf.casper.amethyst.core.AmethystCore;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.amethyst.core.mq.redis.RedisListener;
import wtf.casper.amethyst.libs.lettuce.pubsub.RedisPubSubListener;
import wtf.casper.hccore.modules.worldsync.data.BlockSnapshot;
import wtf.casper.hccore.modules.worldsync.data.BlockSnapshotBundle;
import wtf.casper.hccore.modules.worldsync.data.BlockSnapshotFiller;

public class WorldRedisListener implements RedisPubSubListener<String, String> {

    private final WorldManager worldManager = Inject.get(WorldManager.class);


//    @Override
//    public void onMessage(BlockSnapshotBundle blockSnapshot) {
//        System.out.println("Received block snapshot bundle");
//        if (blockSnapshot == null) return;
//        System.out.println("BlockSnapshotBundle contains " + blockSnapshot.getBlockSnapshots().size() + " snapshots");
//        for (BlockSnapshot snapshot : blockSnapshot.getBlockSnapshots()) {
//            System.out.println("snapshot: " + snapshot.toString());
//            worldManager.getWorkloadRunnable().addWorkload(new BlockSnapshotFiller(snapshot));
//        }
//    }

    @Override
    public void message(String channel, String message) {
        if (!channel.equals(WorldManager.REDIS_CHANNEL)) {
            return;
        }

        BlockSnapshotBundle bundle = AmethystCore.getGson().fromJson(message, BlockSnapshotBundle.class);

        if (bundle == null) return;
        for (BlockSnapshot snapshot : bundle.getBlockSnapshots()) {
            worldManager.getWorkloadRunnable().addWorkload(new BlockSnapshotFiller(snapshot));
        }

    }

    @Override
    public void message(String s, String k1, String s2) {

    }

    @Override
    public void subscribed(String s, long l) {

    }

    @Override
    public void psubscribed(String s, long l) {

    }

    @Override
    public void unsubscribed(String s, long l) {

    }

    @Override
    public void punsubscribed(String s, long l) {

    }
}
