package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.collision.ClientCollisionTrackingPolicy;
import com.kingdomcomecombat.client.collision.ClientModelHurtboxCache;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Re-captures player parts after PAL's PlayerEntityModel RETURN injection. */
@Mixin(value = PlayerEntityModel.class, priority = 500)
public abstract class PlayerEntityModelPalHurtboxMixin {
    @Inject(
            method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V",
            at = @At("TAIL")
    )
    private void kingdomcomecombat$capturePalPlayerPose(
            PlayerEntityRenderState state,
            CallbackInfo ci
    ) {
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        MinecraftClient client = MinecraftClient.getInstance();
        boolean externalLocalFirstPerson = client.player != null
                && entityId == client.player.getId()
                && client.options.getPerspective().isFirstPerson()
                && com.kingdomcomecombat.client.compat.FirstPersonRenderCompat
                        .isExternalBodyRenderOrPreparing();
        if (!externalLocalFirstPerson
                && !ClientModelHurtboxCache.shouldCaptureRenderedPose(entityId)) {
            return;
        }
        if (client.world == null
                || !(client.world.getEntityById(entityId) instanceof LivingEntity entity)
                || !ClientCollisionTrackingPolicy.shouldCaptureHurtbox(entity)) {
            return;
        }

        BipedEntityModel<?> model = (BipedEntityModel<?>) (Object) this;
        ClientModelHurtboxCache.update(
                entityId,
                model.head,
                model.body,
                model.rightArm,
                model.leftArm,
                model.rightLeg,
                model.leftLeg,
                state.bodyYaw
        );
    }
}
