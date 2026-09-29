package dev.kirill.hbk.client;

import dev.kirill.hbk.client.mixin.ChatComponentAccessor;
import dev.kirill.hbk.entity.KirillSecondEntity;
import dev.kirill.hbk.network.ModNetworking;
import dev.kirill.hbk.registry.ModSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class UnknownEncounterClient {
	private static final int TICKS_PER_CHARACTER = 3;
	private static final double CAMERA_TARGET_VERTICAL_OFFSET = -0.3;
	private static boolean active;
	private static boolean musicActive;
	private static int unknownEntityId = -1;
	private static UUID unknownEntityUuid;
	private static int pendingRemovalEntityId = -1;
	private static UUID pendingRemovalEntityUuid;
	private static String typingText;
	private static int typingTicks;
	private static int visibleCharacters;
	private static GuiMessage typingMessage;
	private static UnknownMusicInstance activeMusic;

	private UnknownEncounterClient() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.UnknownEncounterPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					active = payload.active();
					if (payload.active()) {
						unknownEntityId = payload.entityId();
						unknownEntityUuid = null;
						typingText = null;
						typingMessage = null;
					}
				})
		);
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.UnknownMusicPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					musicActive = payload.playing();
					if (musicActive) {
						startMusic(context.client());
					} else {
						stopMusic(context.client());
						scheduleUnknownRemoval(context.client(), unknownEntityId, unknownEntityUuid);
					}
				})
		);
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.UnknownRemovePayload.TYPE, (payload, context) ->
				context.client().execute(() -> scheduleUnknownRemoval(
						context.client(), payload.entityId(), payload.entityUuid()))
		);
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.UnknownTypingPayload.TYPE, (payload, context) ->
				context.client().execute(() -> startTyping(context.client(), payload.message()))
		);
		ClientTickEvents.END_CLIENT_TICK.register(UnknownEncounterClient::tick);
	}

	public static boolean isActive() {
		return active;
	}

	public static boolean isMusicActive() {
		return musicActive;
	}

	private static void tick(Minecraft client) {
		tickTyping(client);
		removePendingUnknown(client);
		if (client.player == null || client.level == null) {
			active = false;
			musicActive = false;
			if (client.getConnection() == null) {
				unknownEntityId = -1;
				unknownEntityUuid = null;
				pendingRemovalEntityId = -1;
				pendingRemovalEntityUuid = null;
			}
			stopMusic(client);
			return;
		}
		if (musicActive) {
			startMusic(client);
		} else {
			stopMusic(client);
		}
		if (!active) {
			return;
		}
		if (!client.player.isAlive()) {
			active = false;
			return;
		}
		Entity unknown = client.level.getEntity(unknownEntityId);
		if (unknown != null) {
			unknownEntityUuid = unknown.getUUID();
			client.player.lookAt(EntityAnchorArgument.Anchor.EYES,
					unknown.getEyePosition().add(0.0, CAMERA_TARGET_VERTICAL_OFFSET, 0.0));
		}
	}

	private static void startTyping(Minecraft client, String message) {
		typingText = message;
		typingTicks = 0;
		visibleCharacters = 0;
		ChatComponent chat = client.gui.hud.getChat();
		chat.addClientSystemMessage(unknownMessage(""));
		var messages = ((ChatComponentAccessor) chat).hbk$getAllMessages();
		typingMessage = messages.isEmpty() ? null : messages.getFirst();
	}

	private static void tickTyping(Minecraft client) {
		if (typingText == null) {
			return;
		}
		typingTicks++;
		int totalCharacters = typingText.codePointCount(0, typingText.length());
		int nextVisible = Math.min(totalCharacters, typingTicks / TICKS_PER_CHARACTER);
		if (nextVisible > visibleCharacters) {
			visibleCharacters = nextVisible;
			int endIndex = typingText.offsetByCodePoints(0, visibleCharacters);
			replaceTypingMessage(client, unknownMessage(typingText.substring(0, endIndex)));
		}
		if (visibleCharacters >= totalCharacters) {
			typingText = null;
			typingMessage = null;
		}
	}

	private static void replaceTypingMessage(Minecraft client, Component content) {
		if (typingMessage == null) {
			return;
		}
		ChatComponent chat = client.gui.hud.getChat();
		ChatComponentAccessor accessor = (ChatComponentAccessor) chat;
		var messages = accessor.hbk$getAllMessages();
		int index = messages.indexOf(typingMessage);
		if (index < 0) {
			return;
		}
		typingMessage = new GuiMessage(typingMessage.addedTime(), content, typingMessage.signature(),
				typingMessage.source(), typingMessage.tag());
		messages.set(index, typingMessage);
		accessor.hbk$refreshTrimmedMessages();
	}

	private static Component unknownMessage(String message) {
		return Component.literal("<???> " + message);
	}

	private static void startMusic(Minecraft client) {
		if (activeMusic != null && (activeMusic.isStopped()
				|| activeMusic.hasStarted() && !client.getSoundManager().isActive(activeMusic))) {
			activeMusic = null;
		}
		client.getMusicManager().stopPlaying();
		if (activeMusic == null && musicActive && client.level != null && client.player != null) {
			activeMusic = new UnknownMusicInstance(client);
			client.getSoundManager().play(activeMusic);
		}
	}

	private static void stopMusic(Minecraft client) {
		if (activeMusic != null) {
			client.getSoundManager().stop(activeMusic);
			activeMusic = null;
		}
	}

	private static void scheduleUnknownRemoval(Minecraft client, int entityId, UUID entityUuid) {
		pendingRemovalEntityId = entityId;
		pendingRemovalEntityUuid = entityUuid;
		removePendingUnknown(client);
	}

	private static void removePendingUnknown(Minecraft client) {
		if (client.level == null || pendingRemovalEntityId < 0 && pendingRemovalEntityUuid == null) {
			return;
		}

		Entity target = pendingRemovalEntityId < 0 ? null : client.level.getEntity(pendingRemovalEntityId);
		if (!(target instanceof KirillSecondEntity)
				|| pendingRemovalEntityUuid != null && !pendingRemovalEntityUuid.equals(target.getUUID())) {
			target = null;
		}
		if (target == null && pendingRemovalEntityUuid != null) {
			for (Entity candidate : client.level.entitiesForRendering()) {
				if (candidate instanceof KirillSecondEntity
						&& pendingRemovalEntityUuid.equals(candidate.getUUID())) {
					target = candidate;
					break;
				}
			}
		}
		if (target == null) {
			return;
		}

		int removedId = target.getId();
		UUID removedUuid = target.getUUID();
		client.level.removeEntity(removedId, Entity.RemovalReason.DISCARDED);
		if (unknownEntityId == removedId || removedUuid.equals(unknownEntityUuid)) {
			unknownEntityId = -1;
			unknownEntityUuid = null;
		}
		pendingRemovalEntityId = -1;
		pendingRemovalEntityUuid = null;
	}

	private static final class UnknownMusicInstance extends AbstractTickableSoundInstance {
		private final Minecraft client;
		private boolean started;

		private UnknownMusicInstance(Minecraft client) {
			super(ModSounds.UNKNOWN_MUSIC, SoundSource.MUSIC, RandomSource.create());
			this.client = client;
			this.volume = 0.85f;
			this.pitch = 0.86f;
			this.looping = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.relative = true;
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		@Override
		public void tick() {
			this.started = true;
			if (!musicActive || this.client.level == null || this.client.player == null) {
				this.stop();
			}
		}

		private boolean hasStarted() {
			return this.started;
		}
	}
}
