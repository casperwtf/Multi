package wtf.casper.multi.modules.worldsync;

import com.google.auto.service.AutoService;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.amethyst.core.mq.Message;
import wtf.casper.amethyst.paper.scheduler.SchedulerUtil;
import wtf.casper.multi.packets.worldsync.GlobalSetTimePacket;
import wtf.casper.multi.packets.worldsync.GlobalSetWeatherPacket;

@AutoService(Listener.class) @Log
public class WorldSyncListener implements Listener {

    private final boolean isMaster;
    private final WorldManager worldManager;

    public WorldSyncListener() {
        WorldManager manager = Inject.get(WorldManager.class);
        this.worldManager = manager;
        this.isMaster = manager.isMaster();

        if (isMaster) {
            // sync time & weather async
            SchedulerUtil.runDelayedTimerAsync(() -> {
                for (World world : Bukkit.getWorlds()) {
                    emit(new GlobalSetTimePacket(world.getName(), world.getTime()));

                    if (world.isThundering()) {
                        emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.THUNDER, world.getThunderDuration()));
                    } else if (world.hasStorm()) {
                        emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.RAIN, world.getWeatherDuration()));
                    } else {
                        emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.CLEAR, world.getClearWeatherDuration()));
                    }
                }
            }, 20L, 20L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldTime(TimeSkipEvent event) {
        event.setCancelled(true);

        if (!isMaster) {
            return;
        }

        emit(new GlobalSetTimePacket(event.getWorld().getName(), event.getWorld().getTime() + event.getSkipAmount()));
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!isMaster) {
            event.setCancelled(true);
            return;
        }

        World world = event.getWorld();
        if (event.toWeatherState()) {
            emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.RAIN, world.getWeatherDuration()));
        } else {
            emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.CLEAR, world.getClearWeatherDuration()));
        }
    }

    private void emit(Message message) {
        worldManager.getRedisPubConnection().async().publish(WorldManager.REDIS_CHANNEL, message.jsonSerialize());
    }
}
