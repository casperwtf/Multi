package wtf.casper.multi.modules.worldsync.redis;

import com.google.auto.service.AutoService;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import wtf.casper.amethyst.core.mq.redis.RedisListener;
import wtf.casper.multi.packets.worldsync.GlobalSetWorldDifficultyPacket;
import wtf.casper.multi.packets.worldsync.WorldSyncRedisListener;

@AutoService(WorldSyncRedisListener.class)
public class SetWorldDifficultyListener extends RedisListener<GlobalSetWorldDifficultyPacket> implements WorldSyncRedisListener {

    public SetWorldDifficultyListener() {
        super(GlobalSetWorldDifficultyPacket.class);
    }

    @Override
    public void onMessage(GlobalSetWorldDifficultyPacket packet) {
        if (packet == null) return;

        World world = Bukkit.getWorld(packet.getWorld());
        if (world == null) return;

        Difficulty difficulty = Difficulty.getByValue(packet.getDifficulty());
        if (difficulty == null) return;

        world.setDifficulty(difficulty);
    }
}
