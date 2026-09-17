package dev.kirill.hbk.network;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.world.StrangeChestManager;
import dev.kirill.hbk.entity.FlyingCarpetEntity;
import dev.kirill.hbk.item.MemberWeaponItem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ModNetworking {
	private ModNetworking() {
	}

	public record OpenStrangeChestPayload(BlockPos pos) implements CustomPacketPayload {
		public static final Type<OpenStrangeChestPayload> TYPE = new Type<>(HbkMod.id("open_strange_chest"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenStrangeChestPayload> CODEC = StreamCodec.of(
				(buffer, payload) -> buffer.writeBlockPos(payload.pos),
				buffer -> new OpenStrangeChestPayload(buffer.readBlockPos())
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record FireAttackingMemberPayload() implements CustomPacketPayload {
		public static final FireAttackingMemberPayload INSTANCE = new FireAttackingMemberPayload();
		public static final Type<FireAttackingMemberPayload> TYPE = new Type<>(HbkMod.id("fire_attacking_member"));
		public static final StreamCodec<RegistryFriendlyByteBuf, FireAttackingMemberPayload> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record AnswerStrangeChestPayload(BlockPos pos, boolean thief) implements CustomPacketPayload {
		public static final Type<AnswerStrangeChestPayload> TYPE = new Type<>(HbkMod.id("answer_strange_chest"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AnswerStrangeChestPayload> CODEC = StreamCodec.of(
				(buffer, payload) -> {
					buffer.writeBlockPos(payload.pos);
					buffer.writeBoolean(payload.thief);
				},
				buffer -> new AnswerStrangeChestPayload(buffer.readBlockPos(), buffer.readBoolean())
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record CarpetInputPayload(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sprint)
			implements CustomPacketPayload {
		public static final Type<CarpetInputPayload> TYPE = new Type<>(HbkMod.id("carpet_input"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CarpetInputPayload> CODEC = StreamCodec.of(
				(buffer, payload) -> {
					buffer.writeBoolean(payload.forward);
					buffer.writeBoolean(payload.backward);
					buffer.writeBoolean(payload.left);
					buffer.writeBoolean(payload.right);
					buffer.writeBoolean(payload.jump);
					buffer.writeBoolean(payload.sprint);
				},
				buffer -> new CarpetInputPayload(
						buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
						buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean())
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(OpenStrangeChestPayload.TYPE, OpenStrangeChestPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AnswerStrangeChestPayload.TYPE, AnswerStrangeChestPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CarpetInputPayload.TYPE, CarpetInputPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(FireAttackingMemberPayload.TYPE, FireAttackingMemberPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(AnswerStrangeChestPayload.TYPE, (payload, context) ->
				context.server().execute(() -> StrangeChestManager.answer(context.player(), payload.pos(), payload.thief()))
		);
		ServerPlayNetworking.registerGlobalReceiver(CarpetInputPayload.TYPE, (payload, context) ->
				context.server().execute(() -> {
					if (context.player().getVehicle() instanceof FlyingCarpetEntity carpet) {
						carpet.setControls(context.player(), payload.forward(), payload.backward(), payload.left(), payload.right(), payload.jump(), payload.sprint());
					}
				})
		);
		ServerPlayNetworking.registerGlobalReceiver(FireAttackingMemberPayload.TYPE, (payload, context) ->
				context.server().execute(() -> {
					if (dev.kirill.hbk.registry.ModItems.usesSpecialLeftClick(context.player().getMainHandItem())
							&& context.player().getMainHandItem().getItem() instanceof MemberWeaponItem weapon) {
						weapon.fire(context.player());
					}
				})
		);
	}
}
