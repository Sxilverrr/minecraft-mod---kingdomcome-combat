package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.client.render.BloodiedTextureCache;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererBloodiedTextureMixin {
    @ModifyVariable(
            method = "getRenderLayer",
            at = @At("STORE"),
            ordinal = 0
    )
    private Identifier kingdomcomecombat$useBloodiedBodyTexture(
            Identifier texture,
            LivingEntityRenderState state,
            boolean showBody,
            boolean translucent,
            boolean showOutline
    ) {
        double blood = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getBodyBloodPercent();
        if (((EntityRenderStateKccAccess) state).kingdomcomecombat$isDragonArmorBroken()) {
            texture = BloodiedTextureCache.getDragonCrackedTexture(texture);
        }
        boolean player = state instanceof PlayerEntityRenderState;
        if (!CombatClientConfig.renderBlood() || blood <= 0.0) {
            return texture;
        }
        if (player && (!CombatClientConfig.renderPlayerBlood()
                || FirstPersonRenderCompat.isExternalBodyRender())) {
            return texture;
        }
        return BloodiedTextureCache.getBloodiedTexture(texture, blood);
    }
}
