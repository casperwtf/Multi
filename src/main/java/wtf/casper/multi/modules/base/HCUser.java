package wtf.casper.multi.modules.base;

import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import wtf.casper.amethyst.libs.storageapi.id.Id;

import javax.annotation.Nullable;
import java.util.*;

@Getter
@Setter
public class HCUser {

    public HCUser(UUID id) {
        this.id = id;
    }

    @Id
    private final UUID id;
    @Nullable
    private String name;
    private double balance = 0;
    private Map<String, String> cachedPlaceholders = new HashMap<>();

    @Nullable
    public String getName(@Nullable String defaultName) {
        String name = Optional.ofNullable(this.name).orElse(defaultName);
        if (this.name == null) {
            this.name = name;
        }
        return name;
    }

    public void parsePlaceholders(List<String> placeholders) {
        Player player = Bukkit.getPlayer(id);
        for (String placeholder : placeholders) {
            String s = PlaceholderAPI.setPlaceholders(player, placeholder);
            cachedPlaceholders.put(placeholder, s);
        }
    }
}
