package dev.kirill.hbk.registry;

import com.mojang.serialization.Codec;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.player.KonataDailyClaim;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModAttachments {
	public static final AttachmentType<Boolean> ROFL_SAFE_FALL = AttachmentRegistry.<Boolean>builder()
			.persistent(com.mojang.serialization.Codec.BOOL)
			.buildAndRegister(HbkMod.id("rofl_safe_fall"));
	public static final AttachmentType<KonataDailyClaim> KONATA_DAILY_CLAIM =
			AttachmentRegistry.<KonataDailyClaim>builder()
					.persistent(KonataDailyClaim.CODEC)
					.copyOnDeath()
					.initializer(() -> KonataDailyClaim.EMPTY)
					.buildAndRegister(HbkMod.id("konata_daily_claim"));
	// Neither part is copied on death: only the appearance is sent to the owner and tracking players.
	public static final AttachmentType<Integer> FURRY_COLOR = AttachmentRegistry.<Integer>builder()
			.persistent(Codec.intRange(-1, 15))
			.initializer(() -> -1)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all())
			.buildAndRegister(HbkMod.id("furry_color"));
	public static final AttachmentType<Long> FURRY_EXPIRES_AT = AttachmentRegistry.<Long>builder()
			.persistent(Codec.LONG)
			.initializer(() -> 0L)
			.buildAndRegister(HbkMod.id("furry_expires_at"));

	private ModAttachments() {
	}

	public static void register() {
		HbkMod.LOGGER.info("Registered hbk player attachments");
	}
}
