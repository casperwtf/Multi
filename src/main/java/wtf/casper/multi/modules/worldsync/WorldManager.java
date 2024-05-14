package wtf.casper.multi.modules.worldsync;

import com.google.auto.service.AutoService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import wtf.casper.amethyst.core.distributedworkload.WorkloadRunnable;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.amethyst.core.utils.ServiceUtil;
import wtf.casper.amethyst.libs.boostedyaml.YamlDocument;
import wtf.casper.amethyst.libs.boostedyaml.block.implementation.Section;
import wtf.casper.amethyst.libs.lettuce.RedisClient;
import wtf.casper.amethyst.libs.lettuce.api.StatefulRedisConnection;
import wtf.casper.amethyst.libs.lettuce.pubsub.RedisPubSubListener;
import wtf.casper.amethyst.libs.lettuce.pubsub.StatefulRedisPubSubConnection;
import wtf.casper.amethyst.libs.storageapi.*;
import wtf.casper.amethyst.libs.storageapi.impl.direct.fstorage.DirectJsonFStorage;
import wtf.casper.amethyst.libs.storageapi.impl.direct.fstorage.DirectMongoFStorage;
import wtf.casper.amethyst.paper.scheduler.SchedulerUtil;
import wtf.casper.multi.Multi;
import wtf.casper.multi.Module;
import wtf.casper.multi.modules.worldsync.data.BlockSnapshot;
import wtf.casper.multi.modules.worldsync.data.BlockSnapshotBundle;
import wtf.casper.multi.modules.worldsync.data.BlockSnapshotFiller;
import wtf.casper.multi.modules.worldsync.data.ServerBasedWorld;
import wtf.casper.multi.packets.worldsync.WorldSyncRedisListener;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoService(Module.class)
@Getter
public class WorldManager implements Module {

    public static String REDIS_CHANNEL = "multi:world-sync:global";
    public static String REDIS_CHANNEL_LOCAL = null;
    public static int RENDER_BLOCKS = (Bukkit.getSimulationDistance() + Bukkit.getViewDistance()) * 16;

    private final List<BlockSnapshot> blockSnapshots = new ArrayList<>();
    private final Multi plugin = Inject.get(Multi.class);

    private final AtomicBoolean IS_READY = new AtomicBoolean(true);

    private RedisClient client;
    private YamlDocument config;
    private StatefulRedisConnection<String, String> redisConnection;
    private StatefulRedisPubSubConnection<String, String> redisPubConnection;
    private StatefulRedisPubSubConnection<String, String> redisSubConnection;
    private WorkloadRunnable workloadRunnable;
    private FieldStorage<UUID, BlockSnapshot> blockStateStorage;

    private ServerBasedWorld world;
    private ServerBasedWorld global;
    private List<ServerBasedWorld> worlds;

    @Override
    public void load() {

    }

    @Override
    public void enable() {
        Inject.bind(WorldManager.class, this);

        this.config = plugin.getYamlDocumentVersioned("world-module.yml");

        setupWorld();
        setupStorage();
        setupRedis();
        setupWorkload();

        if (config.getLong("last-updated") > 0) {
//            loadFromTime(config.getLong("last-updated"));
        }
    }

    @Override
    public void disable() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            Location l = player.getLocation();
            getRedisConnection().sync().set("world:" + player.getUniqueId(), l.getWorld().getName() + "," + l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getYaw() + "," + l.getPitch());
        }

        redisPubConnection.close();
        redisSubConnection.close();
        redisConnection.close();
        client.shutdown();

        blockStateStorage.write().join();
        blockStateStorage.close().join();

        config.set("last-updated", System.currentTimeMillis());
        try {
            config.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupWorld() {
        this.worlds = new ArrayList<>();
        for (String key : this.config.getSection("servers").getRoutesAsStrings(false)) {
            worlds.add(new ServerBasedWorld(key, this.config.getSection("servers." + key)));
        }

        int globalMinX = Integer.MAX_VALUE;
        int globalMinZ = Integer.MAX_VALUE;
        int globalMaxX = Integer.MIN_VALUE;
        int globalMaxZ = Integer.MIN_VALUE;

        for (ServerBasedWorld serverBasedWorld : worlds) {
            if (serverBasedWorld.getName().equals(this.config.getString("this-world"))) {
                this.world = serverBasedWorld;
                break;
            }
        }

        for (ServerBasedWorld serverBasedWorld : worlds) {
            globalMinX = Math.min(globalMinX, serverBasedWorld.getMinX());
            globalMinZ = Math.min(globalMinZ, serverBasedWorld.getMinZ());
            globalMaxX = Math.max(globalMaxX, serverBasedWorld.getMaxX());
            globalMaxZ = Math.max(globalMaxZ, serverBasedWorld.getMaxZ());
        }

        this.global = new ServerBasedWorld("global", globalMinX, globalMinZ, globalMaxX, globalMaxZ, false);

        if (this.world == null) {
            throw new IllegalStateException("World not found");
        }

        this.IS_READY.set(false);
        this.world.loadBorderChunks().whenComplete((unused, throwable) -> {
            if (throwable != null) {
                throw new RuntimeException(throwable);
            }

            this.IS_READY.set(true);
        });

        REDIS_CHANNEL_LOCAL = "multi:world-sync:" + world.getName();
    }

    private void setupStorage() {
        this.blockStateStorage = null;
        StorageType type;
        try {
            type = StorageType.valueOf(config.getString("storage.type"));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid storage type " + config.getString("storage.type") + ". Valid Options: " + StorageType.values().toString());
        }
        switch (type) {
            case MONGODB -> {
                this.blockStateStorage = new DirectMongoFStorage<>(
                        UUID.class,
                        BlockSnapshot.class,
                        fromConfig(getConfig().getSection("storage")),
                        uuid -> {
                            throw new IllegalStateException("BlockSnapshot must be loaded from storage");
                        }
                );
            }
            case JSON -> {
                this.blockStateStorage = new DirectJsonFStorage<>(
                        UUID.class,
                        BlockSnapshot.class,
                        new File(plugin.getDataFolder(), "block-snapshots.json"),
                        uuid -> {
                            throw new IllegalStateException("BlockSnapshot must be loaded from storage");
                        }
                );
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    private void setupWorkload() {
        this.workloadRunnable = new WorkloadRunnable();

        SchedulerUtil.runDelayedTimer(this.workloadRunnable, 30L, 2L);

        SchedulerUtil.runDelayedTimer(() -> {
            if (blockSnapshots.isEmpty()) {
                return;
            }

            this.redisPubConnection.async().publish(REDIS_CHANNEL, new BlockSnapshotBundle(blockSnapshots).jsonSerialize());
            this.blockSnapshots.clear();
        }, 10, 10);

        SchedulerUtil.runDelayedTimerAsync(new BorderRunnable(), 100L, 10L);
        SchedulerUtil.runDelayedTimerAsync(new InnerBorderRunnable(), 100L, 10L);
    }

    private void setupRedis() {
        this.client = RedisClient.create(plugin.getYamlConfig().getString("redis-uri"));
        this.redisConnection = client.connect();
        this.redisPubConnection = client.connectPubSub();
        this.redisSubConnection = client.connectPubSub();
        this.redisSubConnection.sync().subscribe(REDIS_CHANNEL);
        if (REDIS_CHANNEL_LOCAL != null) {
            this.redisSubConnection.sync().subscribe(REDIS_CHANNEL_LOCAL);
        } else {
            throw new IllegalStateException("REDIS_CHANNEL_LOCAL is null");
        }

        for (WorldSyncRedisListener listener : ServiceUtil.getServices(WorldSyncRedisListener.class, List.of())) {
            try {
                this.redisSubConnection.addListener((RedisPubSubListener<String, String>) listener);
            } catch (Exception e) {
                System.out.println("Failed to add listener " + listener.getClass().getSimpleName() + " to redis connection");
                throw new RuntimeException(e);
            }
        }
    }

    private Credentials fromConfig(Section section) {
        return Credentials.of(
                StorageType.valueOf(section.getString("type")),
                section.getString("host"),
                section.getString("username"),
                section.getString("password"),
                section.getString("database"),
                section.getString("collection"),
                section.getString("table"),
                section.getString("uri"),
                section.getInt("port")
        );
    }

    // Aliases for the world listener static methods so you can do it via the world manager
    public void markToNotSaveLocation(Player player) {
        markToNotSaveLocation(player.getUniqueId());
    }

    public void markToNotSaveLocation(UUID uuid) {
        WorldListener.dontSave(uuid);
    }

    public void removeMarkToNotSaveLocation(Player player) {
        removeMarkToNotSaveLocation(player.getUniqueId());
    }

    public void removeMarkToNotSaveLocation(UUID uuid) {
        WorldListener.doSave(uuid);
    }

    public void setLastLocation(Player player, Location location) {
        setLastLocation(player.getUniqueId(), location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public void setLastLocation(UUID uuid, Location location) {
        setLastLocation(uuid, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public void setLastLocation(UUID uuid, String world, double x, double y, double z, float yaw, float pitch) {
        getRedisConnection().sync().set("world:" + uuid, world + "," + x + "," + y + "," + z + "," + yaw + "," + pitch);
    }

    public Optional<Location> getLastLocation(Player player) {
        return getLastLocation(player.getUniqueId());
    }

    public Optional<Location> getLastLocation(UUID uuid) {
        String location = getRedisConnection().sync().get("world:" + uuid);
        if (location == null) {
            return Optional.empty();
        }

        String[] split = location.split(",");
        if (split.length != 6) {
            return Optional.empty();
        }

        String world = split[0];
        double x = Double.parseDouble(split[1]);
        double y = Double.parseDouble(split[2]);
        double z = Double.parseDouble(split[3]);
        float yaw = Float.parseFloat(split[4]);
        float pitch = Float.parseFloat(split[5]);

        return Optional.of(new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch));
    }


    public boolean outsideBorder(Location location) {
        return outsideBorder(location.getBlockX(), location.getBlockZ());
    }

    public boolean outsideBorder(int x, int z) {
        return !withinBorder(x, z);
    }

    public boolean withinBorder(int x, int z) {
        for (ServerBasedWorld serverBasedWorld : getWorlds()) {
            if (serverBasedWorld.withinBorder(x, z)) {
                return true;
            }
        }
        return false;
    }

    public boolean aroundBorder(int x, int z) {
        return aroundBorder(x, z, RENDER_BLOCKS);
    }

    public boolean aroundBorder(int x, int z, int radius) {
        for (ServerBasedWorld serverBasedWorld : getWorlds()) {
            if (serverBasedWorld.aroundBorder(x, z, radius)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMaster() {
        return world.isMaster();
    }

    public void publish(BlockSnapshot blockSnapshot) {
        blockStateStorage.save(blockSnapshot);
        this.blockSnapshots.add(blockSnapshot);
    }

    public List<String> impactedWorlds(int x, int z) {
        List<String> impactedWorlds = new ArrayList<>();
        for (ServerBasedWorld serverBasedWorld : getWorlds()) {
            if (serverBasedWorld.aroundBorderWithin(x, z)) {
                impactedWorlds.add(serverBasedWorld.getName());
            }
        }
        return impactedWorlds;
    }

    public Optional<ServerBasedWorld> getWorld(int x, int z) {
        for (ServerBasedWorld serverBasedWorld : getWorlds()) {
            if (serverBasedWorld.withinBorder(x, z)) {
                return Optional.of(serverBasedWorld);
            }
        }
        return Optional.empty();
    }

    public boolean isReady() {
        return IS_READY.get();
    }

    public void loadFromTime(long time) {
        // get all objects where the time is greater than the time in the db
        blockStateStorage.get(Filter.of("snapshotTime", time, FilterType.GREATER_THAN, SortingType.ASCENDING)).whenComplete((blockSnapshots, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                return;
            }

            for (BlockSnapshot blockSnapshot : blockSnapshots) {
                workloadRunnable.addWorkload(new BlockSnapshotFiller(blockSnapshot));
            }
        });
    }
}
