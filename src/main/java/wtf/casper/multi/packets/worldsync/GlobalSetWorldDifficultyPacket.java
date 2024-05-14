package wtf.casper.multi.packets.worldsync;

import lombok.Data;
import wtf.casper.amethyst.core.mq.Message;

@Data
public class GlobalSetWorldDifficultyPacket implements Message {
    private final String world;
    private final int difficulty;
}
