package wtf.casper.multi.packets.worldsync;

import lombok.Data;
import wtf.casper.amethyst.core.mq.Message;

@Data
public class GlobalSetTimePacket implements Message {
    private final String world;
    private final long time;
}
