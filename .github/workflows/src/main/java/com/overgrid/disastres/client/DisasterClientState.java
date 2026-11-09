package com.overgrid.disastres.client;

import com.overgrid.disastres.DisasterNetworking;
import com.overgrid.disastres.disaster.DisasterPreset;
import com.overgrid.disastres.disaster.DisasterType;
import com.overgrid.disastres.sound.DisasterSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Lado do cliente: recebe os desastres e toca os sons em loop enquanto eles
 * estiverem ativos, com volume que aumenta conforme o jogador se aproxima.
 */
public final class DisasterClientState {
    private DisasterClientState() {}

    private static final List<ClientDisaster> ACTIVE = new ArrayList<>();
    private static final Random RANDOM = Random.create();
    /** Distância, em blocos, a partir da qual o som do desastre fica audível. */
    private static final double HEARING_RANGE = 96.0;

    private static final class ClientDisaster {
        final DisasterType type;
        final Vec3d center;
        final float intensity;
        int ticksLeft;
        int nextThunderTick;
        int age;

        ClientDisaster(DisasterType type, Vec3d center, float intensity, int duration) {
            this.type = type;
            this.center = center;
            this.intensity = intensity;
            this.ticksLeft = duration;
            this.nextThunderTick = 0;
            this.age = 0;
        }
    }

    public static void onDisasterStart(DisasterNetworking.DisasterStartPayload p) {
        ACTIVE.add(new ClientDisaster(DisasterPreset.get(p.typeId()).mechanic(), p.center(), p.intensity(), p.durationTicks()));
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            ACTIVE.clear();
            return;
        }
        Vec3d playerPos = client.player.getEntityPos();

        ACTIVE.removeIf(d -> {
            d.ticksLeft--;
            d.age++;
            double dist = playerPos.distanceTo(d.center);
            if (dist > HEARING_RANGE) return d.ticksLeft <= 0;

            float volume = (float) Math.max(0.0, 1.0 - dist / HEARING_RANGE) * Math.min(1.0f, d.intensity);

            // vento contínuo: a cada ~20 ticks toca uma rajada, com variação
            if (d.age % 20 == 0) {
                play(client, pickGust(d.type), volume * 0.8f, 0.8f + RANDOM.nextFloat() * 0.4f);
            }
            // loops específicos de cada desastre
            switch (d.type) {
                case TORNADO -> {
                    if (d.age % 60 == 0) play(client, DisasterSounds.TORNADO_LOOP, volume, 1.0f);
                    if (d.age % 90 == 0) play(client, DisasterSounds.WIND_HOWL, volume * 0.6f, 0.9f);
                }
                case EARTHQUAKE -> {
                    if (d.age % 40 == 0) play(client, DisasterSounds.EARTHQUAKE_RUMBLE, volume, 0.8f + RANDOM.nextFloat() * 0.3f);
                    if (RANDOM.nextFloat() < 0.04f) play(client, DisasterSounds.DEBRIS_CRASH, volume * 0.7f, 0.9f);
                }
                case WIND_STORM -> {
                    if (d.age % 70 == 0) play(client, DisasterSounds.WIND_HOWL, volume * 0.7f, 1.0f);
                    if (d.age >= d.nextThunderTick && RANDOM.nextFloat() < 0.02f) {
                        play(client, DisasterSounds.THUNDER_FAR, volume * 0.9f, 1.0f);
                        d.nextThunderTick = d.age + 200;
                    }
                }
                case TSUNAMI -> {
                    if (d.age % 50 == 0) play(client, DisasterSounds.WIND_HOWL, volume, 0.6f);
                    if (d.age % 30 == 0) play(client, DisasterSounds.DEBRIS_CRASH, volume * 0.8f, 0.7f);
                }
                case RADIATION -> {
                    if (d.age % 80 == 0) play(client, DisasterSounds.WIND_HOWL, volume * 0.5f, 0.5f);
                }
                case NUCLEAR -> {
                    if (d.age == 1) play(client, DisasterSounds.THUNDER_FAR, volume, 0.5f);
                    if (d.age % 40 == 0) play(client, DisasterSounds.EARTHQUAKE_RUMBLE, volume, 0.6f);
                }
                case BLACK_HOLE -> {
                    if (d.age % 60 == 0) play(client, DisasterSounds.TORNADO_LOOP, volume, 0.6f);
                    if (d.age % 90 == 0) play(client, DisasterSounds.WIND_HOWL, volume * 0.6f, 0.5f);
                }
            }
            return d.ticksLeft <= 0;
        });
    }

    private static net.minecraft.sound.SoundEvent pickGust(DisasterType type) {
        return type == DisasterType.EARTHQUAKE ? DisasterSounds.DEBRIS_CRASH : DisasterSounds.WIND_GUST;
    }

    private static void play(MinecraftClient client, net.minecraft.sound.SoundEvent event, float volume, float pitch) {
        if (volume <= 0.01f) return;
        client.getSoundManager().play(PositionedSoundInstance.ambient(event, pitch, volume));
    }
}
