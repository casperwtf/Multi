package wtf.casper.multi.modules.worldsync;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import com.google.auto.service.AutoService;
import lombok.extern.java.Log;
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
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.amethyst.paper.events.AsyncPlayerMoveEvent;
import wtf.casper.amethyst.paper.hooks.combat.CombatManager;
import wtf.casper.multi.modules.worldsync.data.BlockLocation;
import wtf.casper.multi.modules.worldsync.data.BlockSnapshot;
import wtf.casper.multi.modules.worldsync.data.ServerBasedWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@AutoService(Listener.class) @Log
public class WorldListener implements Listener {
    private final WorldManager worldManager = Inject.get(WorldManager.class);
    private final Map<UUID, Location> protection = new HashMap<>();

    private final BlockData airBlockData = Material.AIR.createBlockData();

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (worldManager.aroundBorder(player.getLocation().getBlockX(), player.getLocation().getBlockZ(), 16)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(PreCreatureSpawnEvent event) {
        if (worldManager.getGlobal().outsideBorder(event.getSpawnLocation().getBlockX(), event.getSpawnLocation().getBlockZ())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (worldManager.aroundBorder(event.getPlayer().getLocation().getBlockX(), event.getPlayer().getLocation().getBlockZ(), 16)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (worldManager.outsideBorder(event.getChunk().getX() * 16, event.getChunk().getZ() * 16)) {
            event.setSaveChunk(false);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!worldManager.getWorld().withinBorder(event.getBlock().getLocation().getBlockX(), event.getBlock().getLocation().getBlockZ())) {
            event.setCancelled(true);
            return;
        }

        if (worldManager.getWorld().aroundBorder(event.getBlock().getLocation().getBlockX(), event.getBlock().getLocation().getBlockZ(), 16)) {
            event.setCancelled(true);
            return;
        }

//        emit(event.getBlock().getLocation(), event.getBlockReplacedState().getBlockData());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!worldManager.getWorld().withinBorder(event.getBlock().getLocation().getBlockX(), event.getBlock().getLocation().getBlockZ())) {
            event.setCancelled(true);
            return;
        }

        if (worldManager.getWorld().aroundBorder(event.getBlock().getLocation().getBlockX(), event.getBlock().getLocation().getBlockZ(), 16)) {
            event.setCancelled(true);
            return;
        }

//        emit(event.getBlock().getLocation(), airBlockData);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        if (!worldManager.getWorld().withinBorder(event.getClickedBlock().getLocation().getBlockX(), event.getClickedBlock().getLocation().getBlockZ())) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBurn(BlockBurnEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            cancel(event, block.getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBlockFade(BlockFadeEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockFertilize(BlockFertilizeEvent event) {
        for (BlockState blockState : event.getBlocks()) {
            cancel(event, blockState.getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockIgnite(BlockIgniteEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        for (BlockState blockState : event.getReplacedBlockStates()) {
            cancel(event, blockState.getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onSignChange(SignChangeEvent event) {
        cancel(event, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        cancel(event, event.getBlock().getLocation());
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
    public void onWorldChangeTime(TimeSkipEvent event) {
        event.setCancelled(true); //TODO: implement global time skip
    }

    @EventHandler
    public void onAsyncPlayerJoin(AsyncPlayerPreLoginEvent event) {
        if (!worldManager.isReady()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, "Server is still loading, please try again in a few seconds.");
        }
    }

    @EventHandler
    public void onPlayerTryLeaveInCombat(AsyncPlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        if (!worldManager.getWorld().aroundBorderWithin(event.getTo().getBlockX(), event.getTo().getBlockZ(), 16)) {
            return;
        }

        if (CombatManager.isInCombat(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeaveWorld(AsyncPlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        if (protection.containsKey(event.getPlayer().getUniqueId())) {

            if (protection.get(event.getPlayer().getUniqueId()).distanceSquared(event.getTo()) < 3) {
                return;
            }

            protection.remove(event.getPlayer().getUniqueId());
            return;
        }

        Optional<ServerBasedWorld> toWorld = worldManager.getWorld(event.getTo().getBlockX(), event.getTo().getBlockZ());

        if (toWorld.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        if (toWorld.get().getName().equals(worldManager.getWorld().getName())) {
            return;
        }

        if (protection.containsKey(event.getPlayer().getUniqueId())) {
            if (protection.get(event.getPlayer().getUniqueId()).distanceSquared(event.getTo()) < 3) {
                return;
            }

            protection.remove(event.getPlayer().getUniqueId());
        }

        if (worldManager.getTeleporting().containsKey(event.getPlayer().getUniqueId())) {
            if (worldManager.getTeleporting().get(event.getPlayer().getUniqueId()).equals(toWorld.get().getName())) {
                event.setCancelled(true);
                return;
            }
        }

        worldManager.getTeleporting().put(event.getPlayer().getUniqueId(), toWorld.get().getName());
        toWorld.get().tp(event.getPlayer());

        Location l = event.getPlayer().getLocation();
        worldManager.getRedisConnection().sync().set("world:" + event.getPlayer().getUniqueId(), l.getWorld().getName() + "," + l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getYaw() + "," + l.getPitch());

        log.info("Teleporting " + event.getPlayer().getName() + " from " + worldManager.getWorld().getName() + " to " + toWorld.get().getName());
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

        Location l = event.getPlayer().getLocation();
        worldManager.getRedisConnection().sync().set("world:" + event.getPlayer().getUniqueId(), l.getWorld().getName() + "," + l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getYaw() + "," + l.getPitch());
    }

    private void emit(Location location, BlockData blockData) {
        if (!worldManager.getWorld().aroundBorder(location.getBlockX(), location.getBlockZ()))
            return;

        BlockSnapshot snapshot = new BlockSnapshot(new BlockLocation(location), blockData.getAsString());
        worldManager.publish(snapshot);
    }

    private void cancel(Cancellable cancellable, Location location) {
        if (!worldManager.getWorld().aroundBorderWithin(location.getBlockX(), location.getBlockZ())) {
            return;
        }

        cancellable.setCancelled(true);
    }
}
