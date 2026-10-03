package dev.kirill.hbk.registry;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.player.KonataDailyClaim;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {
	public static final AttachmentType<KonataDailyClaim> KONATA_DAILY_CLAIM =
			AttachmentRegistry.<KonataDailyClaim>builder()
					.persistent(KonataDailyClaim.CODEC)
					.copyOnDeath()
					.initializer(() -> KonataDailyClaim.EMPTY)
					.buildAndRegister(HbkMod.id("konata_daily_claim"));

	private ModAttachments() {
	}

	public static void register() {
		HbkMod.LOGGER.info("Registered hbk player attachments");
	}
}
