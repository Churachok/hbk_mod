package dev.kirill.hbk.network;

import dev.kirill.hbk.HbkMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Only the rescued player receives the stag activation; no vanilla totem entity event is sent. */
public record JagermeisterRescuePayload() implements CustomPacketPayload {
	public static final JagermeisterRescuePayload INSTANCE = new JagermeisterRescuePayload();
	public static final Type<JagermeisterRescuePayload> TYPE = new Type<>(HbkMod.id("jagermeister_rescue"));
	public static final StreamCodec<RegistryFriendlyByteBuf, JagermeisterRescuePayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	public static void sendTo(ServerPlayer player) {
		if (player.connection != null && ServerPlayNetworking.canSend(player, TYPE)) {
			ServerPlayNetworking.send(player, INSTANCE);
		}
	}
}
