package wtf.casper.hccore;

import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.amethyst.core.utils.ServiceUtil;
import wtf.casper.amethyst.paper.AmethystPlugin;
import wtf.casper.amethyst.paper.utils.BungeeUtil;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class HCCore extends AmethystPlugin {

    private final List<Module> modules = new ArrayList<>();
    public static boolean DEBUG = false;

    @Override
    public void load() {
        Inject.bind(Plugin.class, this);
        Inject.bind(JavaPlugin.class, this);
        Inject.bind(AmethystPlugin.class, this);
        Inject.bind(HCCore.class, this);

        saveDefaultConfig();
        saveConfig();

        DEBUG = getYamlConfig().getBoolean("debug");

        for (Module service : ServiceUtil.getServices(Module.class, this.getClassLoader())) {
            this.getLogger().info("Loading module: " + service.getClass().getSimpleName());
            modules.add(service);
            service.load();
        }
    }

    @Override
    public void enable() {
        for (Module module : modules) {
            module.enable();
        }

        registerCommands(getClassLoader());
        registerListeners(getClassLoader());

        BungeeUtil.registerOutChannel(this, "BungeeCord");
    }

    @Override
    public void disable() {
        for (Module module : modules) {
            module.disable();
        }
    }
}
