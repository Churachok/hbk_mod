package dev.kirill.hbk.network;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.world.SkippingOwlEncounter;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** An owner-only scene clock; repeated snapshots never restart the same encounter. */
public record SkippingOwlScenePayload(UUID encounterId, int owlEntityId, long startedAtGameTime,
		boolean active) implements CustomPacketPayload {
	public static final Type<SkippingOwlScenePayload> TYPE = new Type<>(HbkMod.id("skipping_owl_scene"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SkippingOwlScenePayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeUUID(payload.encounterId());
				buffer.writeVarInt(payload.owlEntityId());
				buffer.writeLong(payload.startedAtGameTime());
				buffer.writeBoolean(payload.active());
			},
			buffer -> new SkippingOwlScenePayload(buffer.readUUID(), buffer.readVarInt(), buffer.readLong(), buffer.readBoolean())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() { return TYPE; }

	public static void registerPayloads() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SkippingOwlLaughFinishedPayload.TYPE,
				SkippingOwlLaughFinishedPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SkippingOwlLaughFinishedPayload.TYPE, (payload, context) ->
				context.server().execute(() -> SkippingOwlEncounter.acknowledgeLaughFinished(
						context.player(), payload.encounterId())));
	}

	/** Clients without this payload capability must never leave a scene hanging. */
	public boolean sendTo(ServerPlayer player) {
		if (player.connection == null || !ServerPlayNetworking.canSend(player, TYPE)) return false;
		ServerPlayNetworking.send(player, this);
		return true;
	}
}
