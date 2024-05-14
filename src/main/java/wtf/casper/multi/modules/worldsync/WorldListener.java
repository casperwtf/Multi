package wtf.casper.multi.modules.worldsync;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import com.google.auto.service.AutoService;
import io.papermc.paper.event.entity.EntityMoveEvent;
import lombok.extern.java.Log;
import org.bukkit.*;
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
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.amethyst.paper.events.AsyncPlayerMoveEvent;
import wtf.casper.amethyst.paper.hooks.combat.CombatController;
import wtf.casper.multi.modules.worldsync.data.BlockLocation;
import wtf.casper.multi.modules.worldsync.data.BlockSnapshot;
import wtf.casper.multi.modules.worldsync.data.ServerBasedWorld;
import wtf.casper.multi.modules.worldsync.utils.TeleportUtil;

import java.util.*;

@AutoService(Listener.class) @Log
public class WorldListener implements Listener {
    private final WorldManager worldManager = Inject.get(WorldManager.class);
    private final Map<UUID, Location> protection = new HashMap<>();

    private final BlockData airBlockData = Material.AIR.createBlockData();

    private static final List<UUID> DONT_SAVE = new ArrayList<>(); // used to prevent saving on quit if we want to override the last location of the user

    public static void dontSave(UUID uuid) {
        DONT_SAVE.add(uuid);
    }

    public static void doSave(UUID uuid) {
        DONT_SAVE.remove(uuid);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (worldManager.getWorld().withinBorder(event.getTo().getBlockX(), event.getTo().getBlockZ())) {
            return;
        }

        event.setCancelled(true);
        //TODO: setup system to send you to right server & coords if you TP out of bounds
    }

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
        if (worldManager.getGlobal().aroundBorder(event.getSpawnLocation().getBlockX(), event.getSpawnLocation().getBlockZ())) {
            event.setCancelled(true);
        }
    }

    @EventHandler //TODO: stress test
    public void onEntityMove(EntityMoveEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }

        Location eventTo = event.getTo();

        if (worldManager.aroundBorder(eventTo.getBlockX(), eventTo.getBlockZ(), 16)) {
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
    // not doing block sync across worlds atm

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

    // not doing block sync across worlds atm
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

    // cancel all this shit
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

        if (CombatController.getHook().getAttacker(event.getPlayer()).isPresent()) {
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

        if (TeleportUtil.isTeleporting(event.getPlayer().getUniqueId())) {
            if (TeleportUtil.getTeleportingWorld(event.getPlayer().getUniqueId()).equals(toWorld.get().getName())) {
                event.setCancelled(true);
                return;
            }
        }

        TeleportUtil.teleportPlayer(event.getPlayer(), event.getTo());

        log.info("Teleporting " + event.getPlayer().getName() + " from " + worldManager.getWorld().getName() + " to " + toWorld.get().getName());
    }

    @EventHandler
    public void onPlayerJoinSpawn(PlayerSpawnLocationEvent event) {
        worldManager.getLastLocation(event.getPlayer().getUniqueId()).ifPresent(location -> {
            event.setSpawnLocation(location);
            event.getPlayer().teleport(location);
        });
        protection.put(event.getPlayer().getUniqueId(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        TeleportUtil.unmarkTeleporting(event.getPlayer());

        if (DONT_SAVE.remove(event.getPlayer().getUniqueId())) {
            return;
        }

        Location l = event.getPlayer().getLocation();
        worldManager.setLastLocation(event.getPlayer().getUniqueId(), l);
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
