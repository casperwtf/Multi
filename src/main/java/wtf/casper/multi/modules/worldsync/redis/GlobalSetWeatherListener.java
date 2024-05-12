package wtf.casper.multi.modules.worldsync.redis;

import com.google.auto.service.AutoService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import wtf.casper.amethyst.core.mq.redis.RedisListener;
import wtf.casper.multi.packets.worldsync.GlobalSetWeatherPacket;
import wtf.casper.multi.packets.worldsync.WorldSyncRedisListener;

@AutoService(WorldSyncRedisListener.class)
public class GlobalSetWeatherListener extends RedisListener<GlobalSetWeatherPacket> implements WorldSyncRedisListener {

    public GlobalSetWeatherListener() {
        super(GlobalSetWeatherPacket.class);
    }

    @Override
    public void onMessage(GlobalSetWeatherPacket globalSetWeatherPacket) {
        World world = Bukkit.getWorld(globalSetWeatherPacket.getWorld());

        switch (globalSetWeatherPacket.getWeatherState()) {
            case RAIN -> {
                world.setStorm(true);
                world.setWeatherDuration(globalSetWeatherPacket.getDuration());
            }
            case THUNDER -> {
                world.setThundering(true);
                world.setThunderDuration(globalSetWeatherPacket.getDuration());
            }
            case CLEAR -> {
                world.setStorm(false);
                world.setThundering(false);
                world.setClearWeatherDuration(globalSetWeatherPacket.getDuration());
            }
        }
    }
}
