package com.overgrid.disastres.sound;

import com.overgrid.disastres.DisastresNaturais;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Sons do mod. Os arquivos .ogg ficam em assets/disastres_naturais/sounds/ e são
 * referenciados por sounds.json. Os nomes aqui precisam bater com as chaves desse JSON.
 */
public final class DisasterSounds {
    private DisasterSounds() {}

    public static final SoundEvent WIND_GUST = register("wind_gust");
    public static final SoundEvent WIND_HOWL = register("wind_howl");
    public static final SoundEvent TORNADO_LOOP = register("tornado_loop");
    public static final SoundEvent EARTHQUAKE_RUMBLE = register("earthquake_rumble");
    public static final SoundEvent DEBRIS_CRASH = register("debris_crash");
    public static final SoundEvent THUNDER_FAR = register("thunder_far");

    private static SoundEvent register(String name) {
        Identifier id = DisastresNaturais.id(name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void register() {
        // o ato de referenciar as constantes já registra os sons
    }
}
