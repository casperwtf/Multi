package wtf.casper.multi.packets.worldsync;

import lombok.Data;
import wtf.casper.amethyst.core.mq.Message;

@Data
public class GlobalSetGamerulePacket implements Message {
    private final String world;
    private final String gamerule;
    private final String value;
}