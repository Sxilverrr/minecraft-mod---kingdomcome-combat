package com.kingdomcomecombat.client.mixin;

import net.minecraft.item.ItemStack;

public interface EntityRenderStateKccAccess {
    void kingdomcomecombat$setEntityId(int entityId);

    int kingdomcomecombat$getEntityId();

    void kingdomcomecombat$setHandStacks(ItemStack mainHandStack, ItemStack offHandStack);

    ItemStack kingdomcomecombat$getMainHandStack();

    ItemStack kingdomcomecombat$getOffHandStack();

    void kingdomcomecombat$setArmorStacks(ItemStack headStack, ItemStack chestStack);

    ItemStack kingdomcomecombat$getHeadStack();

    ItemStack kingdomcomecombat$getChestStack();

    void kingdomcomecombat$setBodyBloodPercent(double percent);

    double kingdomcomecombat$getBodyBloodPercent();

    void kingdomcomecombat$setDragonArmorBroken(boolean broken);

    boolean kingdomcomecombat$isDragonArmorBroken();
}
