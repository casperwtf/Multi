package wtf.casper.hccore.modules.base;

import com.google.auto.service.AutoService;
import lombok.extern.java.Log;
import org.bukkit.command.CommandSender;
import wtf.casper.amethyst.libs.cloud.commandframework.annotations.Argument;
import wtf.casper.amethyst.libs.cloud.commandframework.annotations.CommandDescription;
import wtf.casper.amethyst.libs.cloud.commandframework.annotations.CommandMethod;
import wtf.casper.amethyst.libs.cloud.commandframework.annotations.CommandPermission;
import wtf.casper.amethyst.paper.command.CloudCommand;

@AutoService(CloudCommand.class) @Log
public class UserCommands implements CloudCommand {

    @CommandDescription("Test cloud command using @CommandMethod")
    @CommandMethod("test <test>")
    @CommandPermission("hccore.test")
    public void test(CommandSender sender, @Argument("test") String test) {
        log.info(test);
    }
}
