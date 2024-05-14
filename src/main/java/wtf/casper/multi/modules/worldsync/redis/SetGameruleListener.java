package wtf.casper.multi.modules.worldsync.redis;

import org.bukkit.Bukkit;
import org.bukkit.World;
import wtf.casper.amethyst.core.mq.redis.RedisListener;
import wtf.casper.multi.packets.worldsync.GlobalSetGamerulePacket;
import wtf.casper.multi.packets.worldsync.WorldSyncRedisListener;

public class SetGameruleListener extends RedisListener<GlobalSetGamerulePacket> implements WorldSyncRedisListener {

    public SetGameruleListener() {
        super(GlobalSetGamerulePacket.class);
    }

    @Override
    public void onMessage(GlobalSetGamerulePacket packet) {
        if (packet == null) return;

        World world = Bukkit.getWorld(packet.getWorld());
        if (world == null) return;

        world.setGameRuleValue(packet.getGamerule(), packet.getValue());
    }
}
