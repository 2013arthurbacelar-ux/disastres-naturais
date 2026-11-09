package com.overgrid.disastres;

import com.overgrid.disastres.client.DisasterClientState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class DisastresNaturaisClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DisasterNetworking.registerClientReceivers();
        ClientTickEvents.END_CLIENT_TICK.register(client -> DisasterClientState.tick(client));
    }
}
