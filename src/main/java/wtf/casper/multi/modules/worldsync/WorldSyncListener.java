package wtf.casper.multi.modules.worldsync;

import com.google.auto.service.AutoService;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.papermc.paper.event.world.WorldGameRuleChangeEvent;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.amethyst.core.mq.Message;
import wtf.casper.amethyst.paper.scheduler.SchedulerUtil;
import wtf.casper.multi.packets.worldsync.GlobalSetGamerulePacket;
import wtf.casper.multi.packets.worldsync.GlobalSetTimePacket;
import wtf.casper.multi.packets.worldsync.GlobalSetWeatherPacket;
import wtf.casper.multi.packets.worldsync.GlobalSetWorldDifficultyPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@AutoService(Listener.class) @Log
public class WorldSyncListener implements Listener {

    private final boolean isMaster;
    private final WorldManager worldManager;
    private final Map<String, Difficulty> lastDifficulty = new HashMap<>();

    public WorldSyncListener() {
        WorldManager manager = Inject.get(WorldManager.class);
        this.worldManager = manager;
        this.isMaster = manager.isMaster();

        if (isMaster) {
            setupScheduler();
        }
    }

    private void setupScheduler() {
        SchedulerUtil.runDelayedTimerAsync(() -> {
            for (World world : Bukkit.getWorlds()) {
                handleWorld(world);
            }
        }, 20L, 20L);
    }

    private void handleWorld(World world) {
        // handle difficulty
        if (!lastDifficulty.containsKey(world.getName())) {
            lastDifficulty.put(world.getName(), world.getDifficulty());
        } else {
            if (lastDifficulty.get(world.getName()) != world.getDifficulty()) {
                emit(new GlobalSetWorldDifficultyPacket(world.getName(), world.getDifficulty().getValue()));
                lastDifficulty.put(world.getName(), world.getDifficulty());
            }
        }

        // sync time
        emit(new GlobalSetTimePacket(world.getName(), world.getTime()));

        // weather fallback
        if (world.isThundering()) {
            emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.THUNDER, world.getThunderDuration()));
        } else if (world.hasStorm()) {
            emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.RAIN, world.getWeatherDuration()));
        } else {
            emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.CLEAR, world.getClearWeatherDuration()));
        }
    }

    @EventHandler
    public void onGameruleChange(WorldGameRuleChangeEvent event) {
        if (!isMaster) {
            event.setCancelled(true);
            return;
        }

        emit(new GlobalSetGamerulePacket(event.getWorld().getName(), event.getGameRule().getName(), event.getValue()));
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
            if (world.isThundering()) {
                emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.THUNDER, world.getThunderDuration()));
            } else {
                emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.RAIN, world.getWeatherDuration()));
            }
        } else {
            emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.CLEAR, world.getClearWeatherDuration()));
        }
    }

    @EventHandler
    public void onThunderChange(ThunderChangeEvent event) {
        if (!isMaster) {
            event.setCancelled(true);
            return;
        }

        World world = event.getWorld();
        if (!world.hasStorm()) {
            emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.CLEAR, world.getClearWeatherDuration()));
        }

        if (world.isThundering()) {
            emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.THUNDER, world.getThunderDuration()));
        } else {
            emit(new GlobalSetWeatherPacket(world.getName(), GlobalSetWeatherPacket.WeatherState.RAIN, world.getWeatherDuration()));
        }
    }

    private void emit(Message message) {
        worldManager.getRedisPubConnection().async().publish(WorldManager.REDIS_CHANNEL, message.jsonSerialize());
    }
}
