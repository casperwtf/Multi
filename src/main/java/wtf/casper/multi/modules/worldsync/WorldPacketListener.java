package wtf.casper.multi.modules.worldsync;

import wtf.casper.amethyst.libs.packetevents.api.event.simple.PacketLoginReceiveEvent;
import wtf.casper.amethyst.libs.packetevents.api.event.simple.PacketLoginSendEvent;
import wtf.casper.amethyst.libs.packetevents.api.event.simple.PacketPlaySendEvent;
import wtf.casper.amethyst.libs.packetevents.api.event.simple.PacketStatusReceiveEvent;
import wtf.casper.amethyst.paper.utils.PacketListener;

public class WorldPacketListener extends PacketListener {

    public WorldPacketListener() {
        register();
    }

    @Override
    public void onPacketLoginReceive(PacketLoginReceiveEvent event) {

    }

    @Override
    public void onPacketLoginSend(PacketLoginSendEvent event) {

    }

    @Override
    public void onPacketStatusReceive(PacketStatusReceiveEvent event) {

    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {

    }
}
