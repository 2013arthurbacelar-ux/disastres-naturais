package com.overgrid.disastres.client;

import net.minecraft.client.MinecraftClient;

/** Ponte client-only: abre o menu da varinha. Só é chamada quando o código roda no cliente. */
public final class DisasterWandClient {
    private DisasterWandClient() {}

    public static void openMenu() {
        MinecraftClient.getInstance().setScreen(new DisasterWandScreen());
    }
}
