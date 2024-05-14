package wtf.casper.multi.modules.worldsync;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import wtf.casper.amethyst.core.inject.Inject;

//TODO: rewrite
public class BorderRunnable implements Runnable {

    private final WorldManager worldManager = Inject.get(WorldManager.class);

    private final Particle particle = Particle.REDSTONE;
    private final Particle.DustOptions data = new Particle.DustOptions(Color.RED, 1);

    private final int count = 2;

    private final double maxX = worldManager.getGlobal().getMaxX() + 1;
    private final double minX = worldManager.getGlobal().getMinX() - 1;
    private final double maxZ = worldManager.getGlobal().getMaxZ() + 1;
    private final double minZ = worldManager.getGlobal().getMinZ() - 1;

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
        double visibleDistance = 5;
        return (edge - point) * (edge - point) <= visibleDistance * visibleDistance;
    }

    private void displayParticlesOnXAxis(Player p, double spot, double MAX, double MIN) {
        double centerX = p.getLocation().getX() + 0.5;
        double centerY = p.getLocation().getY() + 0.5;
        double centerZ = Math.floor(spot) + 0.5;

        for (int y = 0; y <= 5; y++) {
            y *= 3;

            double locYpos = centerY + (double) y;
            double locYneg = centerY - (double) y;

            for (int x = 0; x <= 30; x += 3) {
                double locXpos = centerX + (double) x;
                double locXneg = centerX - (double) x;

                if (locXpos <= MAX) {
                    p.spawnParticle(particle, locXpos, locYpos, centerZ, count, data);
                    p.spawnParticle(particle, locXpos, locYneg, centerZ, count, data);
                }
                if (locXneg >= MIN) {
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

            for (int x = 0; x <= 30; x += 3) {
                double locZpos = centerZ + (double) x;
                double locZneg = centerZ - (double) x;

                if (locZpos <= MAX) {
                    p.spawnParticle(particle, centerX, locYpos, locZpos, count, data);
                    p.spawnParticle(particle, centerX, locYneg, locZpos, count, data);
                }
                if (locZneg >= MIN) {
                    p.spawnParticle(particle, centerX, locYpos, locZneg, count, data);
                    p.spawnParticle(particle, centerX, locYneg, locZneg, count, data);
                }
            }
        }
    }
}
