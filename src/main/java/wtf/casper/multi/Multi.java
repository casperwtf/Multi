package wtf.casper.multi;

import lombok.Getter;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.amethyst.core.utils.ServiceUtil;
import wtf.casper.amethyst.paper.AmethystPlugin;
import wtf.casper.amethyst.paper.utils.BungeeUtil;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class Multi extends AmethystPlugin {

    private final List<Module> modules = new ArrayList<>();
    public static boolean DEBUG = false;

    @Override
    public void onLoad() {
        Inject.bind(Multi.class, this);

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
    public void onEnable() {
        for (Module module : modules) {
            module.enable();
        }

        registerCommands(getClassLoader());
        registerListeners(getClassLoader());

        BungeeUtil.registerOutChannel(this, "BungeeCord");
    }

    @Override
    public void onDisable() {
        for (Module module : modules) {
            module.disable();
        }
    }
}
