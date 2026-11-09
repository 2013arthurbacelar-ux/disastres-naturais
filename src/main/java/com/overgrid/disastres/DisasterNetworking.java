package com.overgrid.disastres;

import com.overgrid.disastres.client.DisasterClientState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.Vec3d;

/** Sincroniza eventos de desastre (início, intensidade, tipo) do servidor para os clientes. */
public final class DisasterNetworking {
    private DisasterNetworking() {}

    public record DisasterStartPayload(int typeId, double x, double y, double z, float intensity, int durationTicks)
            implements CustomPayload {
        public static final CustomPayload.Id<DisasterStartPayload> ID =
                new CustomPayload.Id<>(DisastresNaturais.id("disaster_start"));

        public static final PacketCodec<net.minecraft.network.RegistryByteBuf, DisasterStartPayload> CODEC =
                PacketCodecs.tuple(
                        PacketCodecs.VAR_INT, DisasterStartPayload::typeId,
                        PacketCodecs.DOUBLE, DisasterStartPayload::x,
                        PacketCodecs.DOUBLE, DisasterStartPayload::y,
                        PacketCodecs.DOUBLE, DisasterStartPayload::z,
                        PacketCodecs.FLOAT, DisasterStartPayload::intensity,
                        PacketCodecs.VAR_INT, DisasterStartPayload::durationTicks,
                        DisasterStartPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public Vec3d center() {
            return new Vec3d(x, y, z);
        }
    }

    /** Cliente -> servidor: o jogador escolheu um desastre no menu da varinha. */
    public record SelectDisasterPayload(int typeId) implements CustomPayload {
        public static final CustomPayload.Id<SelectDisasterPayload> ID =
                new CustomPayload.Id<>(DisastresNaturais.id("select_disaster"));

        public static final PacketCodec<net.minecraft.network.RegistryByteBuf, SelectDisasterPayload> CODEC =
                PacketCodecs.tuple(PacketCodecs.VAR_INT, SelectDisasterPayload::typeId, SelectDisasterPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(DisasterStartPayload.ID, DisasterStartPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectDisasterPayload.ID, SelectDisasterPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SelectDisasterPayload.ID,
                (payload, context) -> context.server().execute(() ->
                        com.overgrid.disastres.item.DisasterWand.setSelected(
                                context.player(), payload.typeId())));
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(DisasterStartPayload.ID,
                (payload, context) -> context.client().execute(() -> DisasterClientState.onDisasterStart(payload)));
    }

    public static void broadcast(net.minecraft.server.world.ServerWorld world, DisasterStartPayload payload) {
        for (var player : world.getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
