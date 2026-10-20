package dev.kirill.hbk.client.mixin;

import net.minecraft.client.Camera;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraInvoker {
	@Invoker("createProjectionMatrixForCulling")
	Matrix4f hbk$createProjectionMatrixForCulling();
}
