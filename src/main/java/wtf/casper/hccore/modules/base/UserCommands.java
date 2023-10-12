package wtf.casper.hccore.modules.base;

import com.google.auto.service.AutoService;
import org.bukkit.command.CommandSender;
import wtf.casper.amethyst.libs.cloud.commandframework.annotations.Argument;
import wtf.casper.amethyst.libs.cloud.commandframework.annotations.CommandDescription;
import wtf.casper.amethyst.libs.cloud.commandframework.annotations.CommandMethod;
import wtf.casper.amethyst.paper.command.CloudCommandProvider;

@AutoService(CloudCommandProvider.class)
public class UserCommands implements CloudCommandProvider {

    @CommandDescription("Test cloud command using @CommandMethod")
    @CommandMethod("test <test>")
    public void test(CommandSender sender, @Argument("test") String test) {
        System.out.println(test);
    }

}
