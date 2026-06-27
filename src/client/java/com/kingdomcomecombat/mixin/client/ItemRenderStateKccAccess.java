package com.kingdomcomecombat.mixin.client;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.class)
public interface ItemRenderStateKccAccess {
    @Accessor("displayContext")
    ItemDisplayContext kingdomcomecombat$getDisplayContext();
}
