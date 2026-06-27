package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.render.BloodiedTextureCache;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.equipment.BloodiedEntityAccess;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntityRenderer.class)
public class LegacyLivingEntityRendererBloodiedTextureMixin {
    @ModifyVariable(method = "getRenderLayer", at = @At("STORE"), ordinal = 0)
    private Identifier kingdomcomecombat$useBloodiedBodyTexture(
            Identifier texture,
            LivingEntity entity,
            boolean showBody,
            boolean translucent,
            boolean showOutline
    ) {
        double blood = entity instanceof BloodiedEntityAccess access
                ? access.kingdomcomecombat$getBodyBloodPercent() : 0.0;
        if (!CombatClientConfig.renderBlood() || blood <= 0.0) {
            return texture;
        }
        if (entity instanceof PlayerEntity && (!CombatClientConfig.renderPlayerBlood()
                || FirstPersonRenderCompat.isExternalBodyRender())) {
            return texture;
        }
        return BloodiedTextureCache.getBloodiedTexture(texture, blood);
    }
}
