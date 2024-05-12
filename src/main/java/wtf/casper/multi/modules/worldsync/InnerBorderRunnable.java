package wtf.casper.multi.modules.worldsync;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import wtf.casper.amethyst.core.inject.Inject;

public class InnerBorderRunnable implements Runnable {

    private final WorldManager worldManager = Inject.get(WorldManager.class);

    private final Particle particle = Particle.REDSTONE;
    private final Particle.DustOptions data = new Particle.DustOptions(Color.PURPLE, 2);

    private final int count = 2;

    private final double maxX = worldManager.getWorld().getMaxX() + 1;
    private final double minX = worldManager.getWorld().getMinX() - 1;
    private final double maxZ = worldManager.getWorld().getMaxZ() + 1;
    private final double minZ = worldManager.getWorld().getMinZ() - 1;

    private final double maxXGlobal = worldManager.getGlobal().getMaxX() + 1;
    private final double minXGlobal = worldManager.getGlobal().getMinX() - 1;
    private final double maxZGlobal = worldManager.getGlobal().getMaxZ() + 1;
    private final double minZGlobal = worldManager.getGlobal().getMinZ() - 1;


    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            showParticles(player);
        }
    }


    private void showParticles(Player player) {
        double pX = player.getLocation().getX();
        double pZ = player.getLocation().getZ();

        if (playerIsNearBorder(maxX, pX)) {
            displayParticlesOnZAxis(player, maxX, maxZ, minZ);
        }
        if (playerIsNearBorder(minX, pX)) {
            displayParticlesOnZAxis(player, minX, maxZ, minZ);
        }
        if (playerIsNearBorder(maxZ, pZ)) {
            displayParticlesOnXAxis(player, maxZ, maxX, minX);
        }
        if (playerIsNearBorder(minZ, pZ)) {
            displayParticlesOnXAxis(player, minZ, maxX, minX);
        }
    }

    private boolean playerIsNearBorder(double edge, double point) {
        double visibleDistance = 8;
        double distance = edge - point;
        return distance * distance <= visibleDistance * visibleDistance;
    }

    private void displayParticlesOnXAxis(Player p, double spot, double MAX, double MIN) {
        double centerX = p.getLocation().getX() + 0.5;
        double centerY = p.getLocation().getY() + 0.5;
        double centerZ = Math.floor(spot) + 0.5;

        for (int y = 0; y <= 15; y += 2) {
            double locYpos = centerY + (double) y;
            double locYneg = centerY - (double) y;

            for (int x = 0; x <= 15; x += 2) {
                double locXpos = centerX + (double) x;
                double locXneg = centerX - (double) x;

                if (locXpos <= MAX && !alongGlobalBorder(minXGlobal, maxXGlobal, locXpos)) {
                    p.spawnParticle(particle, locXpos, locYpos, centerZ, count, data);
                    p.spawnParticle(particle, locXpos, locYneg, centerZ, count, data);
                }
                if (locXneg >= MIN && !alongGlobalBorder(minXGlobal, maxXGlobal, locXneg)) {
                    p.spawnParticle(particle, locXneg, locYpos, centerZ, count, data);
                    p.spawnParticle(particle, locXneg, locYneg, centerZ, count, data);
                }
            }
        }
    }

    private void displayParticlesOnZAxis(Player p, double spot, double MAX, double MIN) {
        double centerX = Math.floor(spot) + 0.5;
        double centerY = p.getLocation().getY() + 0.5;
        double centerZ = p.getLocation().getZ() + 0.5;

        for (int y = 0; y <= 15; y += 3) {
            double locYpos = centerY + (double) y;
            double locYneg = centerY - (double) y;

            for (int x = 0; x <= 15; x += 3) {
                double locZpos = centerZ + (double) x;
                double locZneg = centerZ - (double) x;

                if (locZpos <= MAX && !alongGlobalBorder(minZGlobal, maxZGlobal, locZpos)) {
                    p.spawnParticle(particle, centerX, locYpos, locZpos, count, data);
                    p.spawnParticle(particle, centerX, locYneg, locZpos, count, data);
                }
                if (locZneg >= MIN && !alongGlobalBorder(minZGlobal, maxZGlobal, locZneg)) {
                    p.spawnParticle(particle, centerX, locYpos, locZneg, count, data);
                    p.spawnParticle(particle, centerX, locYneg, locZneg, count, data);
                }
            }
        }
    }

    private boolean alongGlobalBorder(double min, double max, double actual) {
        return actual <= min && actual >= max;
    }
}
