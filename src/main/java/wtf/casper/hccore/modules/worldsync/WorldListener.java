package wtf.casper.hccore.modules.worldsync;

import com.google.auto.service.AutoService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.amethyst.paper.events.AsyncPlayerMoveEvent;
import wtf.casper.hccore.modules.worldsync.data.BlockLocation;
import wtf.casper.hccore.modules.worldsync.data.BlockSnapshot;
import wtf.casper.hccore.modules.worldsync.data.ServerBasedWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AutoService(Listener.class)
public class WorldListener implements Listener {
    private final WorldManager worldManager = Inject.get(WorldManager.class);
    private final Map<UUID, Location> protection = new HashMap<>();

    private final BlockData airBlockData = Material.AIR.createBlockData();


    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (worldManager.aroundBorder(player.getLocation().getBlockX(), player.getLocation().getBlockZ(), 8)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (worldManager.aroundBorder(event.getBlock().getX(), event.getBlock().getZ(), 8)) {
            event.setCancelled(true);
        } else {
            emit(event.getBlock().getLocation(), airBlockData);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (worldManager.aroundBorder(event.getBlock().getX(), event.getBlock().getZ(), 8)) {
            event.setCancelled(true);
        } else {
            emit(event.getBlock().getLocation(), airBlockData);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBurn(BlockBurnEvent event) {
        emit(event.getBlock().getLocation(), event.getBlock().getBlockData());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockDamage(BlockDamageEvent event) {
        emit(event.getBlock().getLocation(), event.getBlock().getBlockData());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            emit(block.getLocation(), block.getBlockData());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBlockFade(BlockFadeEvent event) {
        emit(event.getBlock().getLocation(), event.getBlock().getBlockData());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockFertilize(BlockFertilizeEvent event) {
        for (BlockState blockState : event.getBlocks()) {
            emit(blockState.getLocation(), blockState.getBlockData());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockIgnite(BlockIgniteEvent event) {
        emit(event.getBlock().getLocation(), event.getBlock().getBlockData());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        for (BlockState blockState : event.getReplacedBlockStates()) {
            emit(blockState.getLocation(), blockState.getBlockData());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        emit(event.getBlock().getLocation(), event.getBlock().getBlockData());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onSignChange(SignChangeEvent event) {
        emit(event.getBlock().getLocation(), event.getBlock().getBlockData());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        emit(event.getBlock().getLocation(), event.getBlock().getBlockData());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        emit(event.getBlock().getLocation(), event.getBlock().getBlockData());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockCook(BlockCookEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onLeavesDecay(LeavesDecayEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onMoistureChange(MoistureChangeEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onFluidLevelChange(FluidLevelChangeEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockSpread(BlockSpreadEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            cancel(event, block.getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            cancel(event, block.getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockForm(BlockFormEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockFromTo(BlockFromToEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockGrow(BlockGrowEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler
    public void onLeaveWorld(AsyncPlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        if (worldManager.getWorld().withinBorder(event.getTo().getBlockX(), event.getTo().getBlockZ())) return;
        if (protection.containsKey(event.getPlayer().getUniqueId())) {
            if (protection.get(event.getPlayer().getUniqueId()).distanceSquared(event.getTo()) < 3) return;
            protection.remove(event.getPlayer().getUniqueId());
            return;
        }

        if (worldManager.getTeleporting().contains(event.getPlayer().getUniqueId())) {
            return;
        }

        ServerBasedWorld thisWorld = null;
        for (ServerBasedWorld world : worldManager.getWorlds()) {
            if (world.withinBorder(event.getTo().getBlockX(), event.getTo().getBlockZ())) {
                Location l = event.getTo();
                worldManager.getTeleporting().add(event.getPlayer().getUniqueId());
                worldManager.getRedisConnection().sync().set("world:" + event.getPlayer().getUniqueId(), l.getWorld().getName()+","+l.getX()+","+l.getY()+","+l.getZ()+","+l.getYaw()+","+l.getPitch());
                world.tp(event.getPlayer());
                thisWorld = world;
                break;
            }
        }

        if (thisWorld == null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoinSpawn(PlayerSpawnLocationEvent event) {
        String loc = worldManager.getRedisConnection().sync().get("world:" + event.getPlayer().getUniqueId());
        if (loc == null) {
            return;
        }
        String[] split = loc.split(",");
        if (split.length != 6) {
            return;
        }
        World world = Bukkit.getWorld(split[0]);
        if (world == null) {
            return;
        }
        event.setSpawnLocation(new Location(world, Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5])));
        protection.put(event.getPlayer().getUniqueId(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        worldManager.getTeleporting().remove(event.getPlayer().getUniqueId());
    }

    private void emit(Location location, BlockData blockData) {
        if (!worldManager.getWorld().aroundBorder(location.getBlockX(), location.getBlockZ())) return;

        BlockSnapshot snapshot = new BlockSnapshot(new BlockLocation(location), blockData.getAsString());
        worldManager.publish(snapshot);
    }

    private void cancel(Cancellable cancellable, Location location) {
        if (!worldManager.getWorld().aroundBorderWithin(location.getBlockX(), location.getBlockZ())) return;

        cancellable.setCancelled(true);
    }
}
