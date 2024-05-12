package wtf.casper.multi.modules.worldsync.redis;

import com.google.auto.service.AutoService;
import org.bukkit.Bukkit;
import wtf.casper.amethyst.core.mq.redis.RedisListener;
import wtf.casper.multi.packets.worldsync.GlobalSetTimePacket;
import wtf.casper.multi.packets.worldsync.WorldSyncRedisListener;

@AutoService(WorldSyncRedisListener.class)
public class GlobalSetTimeListener extends RedisListener<GlobalSetTimePacket> implements WorldSyncRedisListener {

    public GlobalSetTimeListener() {
        super(GlobalSetTimePacket.class);
    }

    @Override
    public void onMessage(GlobalSetTimePacket globalSetTimePacket) {
        Bukkit.getWorld(globalSetTimePacket.getWorld()).setTime(globalSetTimePacket.getTime());
    }
}
