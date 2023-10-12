package wtf.casper.hccore.modules.worldsync;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import wtf.casper.amethyst.core.distributedworkload.WorkloadRunnable;
import wtf.casper.amethyst.core.inject.Inject;

import wtf.casper.amethyst.libs.lettuce.RedisClient;
import wtf.casper.amethyst.libs.lettuce.api.StatefulRedisConnection;
import wtf.casper.amethyst.libs.lettuce.pubsub.StatefulRedisPubSubConnection;
import wtf.casper.amethyst.libs.storageapi.*;
import wtf.casper.amethyst.libs.storageapi.impl.direct.fstorage.DirectJsonFStorage;
import wtf.casper.amethyst.libs.storageapi.impl.direct.fstorage.DirectMongoFStorage;
import wtf.casper.amethyst.libs.storageapi.libs.boostedyaml.YamlDocument;
import wtf.casper.hccore.HCCore;
import wtf.casper.hccore.Module;
import wtf.casper.hccore.modules.worldsync.data.BlockSnapshot;
import wtf.casper.hccore.modules.worldsync.data.BlockSnapshotBundle;
import wtf.casper.hccore.modules.worldsync.data.BlockSnapshotFiller;
import wtf.casper.hccore.modules.worldsync.data.ServerBasedWorld;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AutoService(Module.class) @Getter
public class WorldManager implements Module {

    public static String REDIS_CHANNEL = "hccore:world-sync";
    public static int RENDER_BLOCKS = (Bukkit.getSimulationDistance() + Bukkit.getViewDistance()) * 16;
    private final List<BlockSnapshot> blockSnapshots = new ArrayList<>();
    private final HCCore plugin = Inject.get(HCCore.class);
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
        setupWorldBorder();
        setupStorage();
        setupRedis();
        setupWorkload();

        if (config.getLong("last-updated") > 0) {
//            loadFromTime(config.getLong("last-updated"));
        }
    }

    @Override
    public void disable() {
        redisPubConnection.close();
        redisSubConnection.close();
        redisConnection.close();
        client.shutdown();

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

        this.world.loadBorderChunks();

        this.global = new ServerBasedWorld("global", globalMinX, globalMinZ, globalMaxX, globalMaxZ);

        if (this.world == null) {
            throw new IllegalStateException("World not found");
        }
    }

    private void setupStorage() {
        this.blockStateStorage = null;
        StorageType type = null;
        try {
            type = StorageType.valueOf(config.getString("storage.type"));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid storage type " + config.getString("storage.type")+ ". Valid Options: " + StorageType.values().toString());
        }
        switch (type) {
            case MONGODB -> {
                this.blockStateStorage = new DirectMongoFStorage<>(
                        UUID.class,
                        BlockSnapshot.class,
                        Credentials.from(config, "storage"),
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

        this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            this.workloadRunnable.run();
        }, 0, 1);

        this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            if (blockSnapshots.isEmpty()) {
                return;
            }

            this.redisPubConnection.sync().publish(REDIS_CHANNEL, new BlockSnapshotBundle(blockSnapshots).serialize());
            this.blockSnapshots.clear();
        }, 10, 1);

        this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new BorderRunnable(), 100L, 10L);
    }

    private void setupRedis() {
        this.client = RedisClient.create(plugin.getYamlConfig().getString("redis-uri"));
        this.redisConnection = client.connect();
        this.redisPubConnection = client.connectPubSub();
        this.redisSubConnection = client.connectPubSub();
        this.redisSubConnection.sync().subscribe(REDIS_CHANNEL);
        this.redisSubConnection.addListener(new WorldRedisListener());
    }

    private void setupWorldBorder() {
        for (World bukkitWorld : plugin.getServer().getWorlds()) {
            int subWorldXCenter = this.world.getMinX() + ((this.world.getMaxX() - this.world.getMinX()) / 2);
            int subWorldZCenter = this.world.getMinZ() + ((this.world.getMaxZ() - this.world.getMinZ()) / 2);
            int subWorldSize = this.world.getMaxX() - this.world.getMinX();

            if (bukkitWorld.getEnvironment() == World.Environment.NETHER) {
                subWorldXCenter /= 8;
                subWorldZCenter /= 8;
                subWorldSize /= 8;
            }

            bukkitWorld.getWorldBorder().setCenter(subWorldXCenter, subWorldZCenter);
            bukkitWorld.getWorldBorder().setSize(subWorldSize + 10);
        }
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

    public void publish(BlockSnapshot blockSnapshot) {
        blockStateStorage.save(blockSnapshot);
        this.blockSnapshots.add(blockSnapshot);
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
