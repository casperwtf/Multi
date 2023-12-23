package wtf.casper.multi.modules.base;

import com.google.auto.service.AutoService;
import wtf.casper.amethyst.core.inject.Inject;
import wtf.casper.amethyst.libs.boostedyaml.YamlDocument;
import wtf.casper.amethyst.libs.storageapi.Credentials;
import wtf.casper.amethyst.libs.storageapi.FieldStorage;
import wtf.casper.amethyst.libs.storageapi.StorageType;
import wtf.casper.amethyst.libs.storageapi.impl.direct.fstorage.DirectJsonFStorage;
import wtf.casper.amethyst.libs.storageapi.impl.direct.fstorage.DirectMongoFStorage;
import wtf.casper.multi.Multi;
import wtf.casper.multi.Module;

import java.io.File;
import java.util.UUID;

//@AutoService(Module.class)
public class UserManager implements Module {

    private final Multi plugin = Inject.get(Multi.class);
    private FieldStorage<UUID, HCUser> userStorage;
    private YamlDocument userModuleConfig;

    @Override
    public void load() {
        Inject.bind(UserManager.class, this);
    }

    @Override
    public void enable() {
        userModuleConfig = plugin.getYamlDocumentVersioned("user-module.yml");
        StorageType type = StorageType.valueOf(userModuleConfig.getString("storage.type"));
        switch (type) {
            case MONGODB -> {
                userStorage = new DirectMongoFStorage<>(
                        UUID.class,
                        HCUser.class,
                        Credentials.from(userModuleConfig, "storage"),
                        HCUser::new
                );
            }
            case JSON -> {
                userStorage = new DirectJsonFStorage<>(
                        UUID.class,
                        HCUser.class,
                        new File(plugin.getDataFolder(), "users.json"),
                        HCUser::new
                );
            }
            default ->
                    throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    @Override
    public void disable() {

    }
}
