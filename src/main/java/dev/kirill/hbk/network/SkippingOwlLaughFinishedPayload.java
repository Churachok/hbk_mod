package dev.kirill.hbk.network;

import dev.kirill.hbk.HbkMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** A completion acknowledgement cannot shorten the authoritative minimum scene duration. */
public record SkippingOwlLaughFinishedPayload(UUID encounterId) implements CustomPacketPayload {
	public static final Type<SkippingOwlLaughFinishedPayload> TYPE = new Type<>(HbkMod.id("skipping_owl_laugh_finished"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SkippingOwlLaughFinishedPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> buffer.writeUUID(payload.encounterId()),
			buffer -> new SkippingOwlLaughFinishedPayload(buffer.readUUID())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
