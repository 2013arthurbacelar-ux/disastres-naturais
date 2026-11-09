package com.overgrid.disastres;

import com.overgrid.disastres.disaster.DisasterManager;
import com.overgrid.disastres.sound.DisasterSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;

public class DisastresNaturais implements ModInitializer {
    public static final String MOD_ID = "disastres_naturais";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        DisasterSounds.register();
        DisasterNetworking.registerPayloads();
        com.overgrid.disastres.item.ModItems.register();

        ServerTickEvents.END_WORLD_TICK.register(world -> DisasterManager.get(world).tick());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                DisasterCommands.register(dispatcher));
    }
}
