package wtf.casper.multi.packets.worldsync;

import lombok.Data;
import wtf.casper.amethyst.core.mq.Message;

@Data
public class GlobalSetWeatherPacket implements Message {
    private final String world;
    private final WeatherState weatherState;
    private final int duration; // in ticks

    public enum WeatherState {
        CLEAR,
        RAIN,
        THUNDER
    }
}
