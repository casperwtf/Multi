package wtf.casper.multi.modules.worldsync.redis;

import com.google.auto.service.AutoService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import wtf.casper.amethyst.core.mq.redis.RedisListener;
import wtf.casper.multi.packets.worldsync.GlobalSetTimePacket;
import wtf.casper.multi.packets.worldsync.WorldSyncRedisListener;

@AutoService(WorldSyncRedisListener.class)
public class SetTimeListener extends RedisListener<GlobalSetTimePacket> implements WorldSyncRedisListener {

    public SetTimeListener() {
        super(GlobalSetTimePacket.class);
    }

    @Override
    public void onMessage(GlobalSetTimePacket globalSetTimePacket) {
        World world = Bukkit.getWorld(globalSetTimePacket.getWorld());
        if (world == null) return;

        world.setTime(globalSetTimePacket.getTime());
    }
}
