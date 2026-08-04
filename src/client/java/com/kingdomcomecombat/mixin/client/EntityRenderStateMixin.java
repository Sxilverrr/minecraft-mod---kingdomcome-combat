package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.equipment.BloodiedEquipment;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityRenderStateKccAccess {
    private boolean kingdomcomecombat$dragonArmorBroken;
    @Unique
    private int kingdomcomecombat$entityId = -1;
    @Unique
    private ItemStack kingdomcomecombat$mainHandStack = ItemStack.EMPTY;
    @Unique
    private ItemStack kingdomcomecombat$offHandStack = ItemStack.EMPTY;
    @Unique
    private ItemStack kingdomcomecombat$headStack = ItemStack.EMPTY;
    @Unique
    private ItemStack kingdomcomecombat$chestStack = ItemStack.EMPTY;
    @Unique
    private double kingdomcomecombat$bodyBloodPercent = 0.0;

    @Override
    public void kingdomcomecombat$setEntityId(int entityId) {
        kingdomcomecombat$entityId = entityId;
    }

    @Override
    public int kingdomcomecombat$getEntityId() {
        return kingdomcomecombat$entityId;
    }

    @Override
    public void kingdomcomecombat$setHandStacks(ItemStack mainHandStack, ItemStack offHandStack) {
        kingdomcomecombat$mainHandStack = mainHandStack;
        kingdomcomecombat$offHandStack = offHandStack;
    }

    @Override
    public ItemStack kingdomcomecombat$getMainHandStack() {
        return kingdomcomecombat$mainHandStack;
    }

    @Override
    public ItemStack kingdomcomecombat$getOffHandStack() {
        return kingdomcomecombat$offHandStack;
    }

    @Override
    public void kingdomcomecombat$setArmorStacks(ItemStack headStack, ItemStack chestStack) {
        kingdomcomecombat$headStack = headStack;
        kingdomcomecombat$chestStack = chestStack;
    }

    @Override
    public ItemStack kingdomcomecombat$getHeadStack() {
        return kingdomcomecombat$headStack;
    }

    @Override
    public ItemStack kingdomcomecombat$getChestStack() {
        return kingdomcomecombat$chestStack;
    }

    @Override
    public void kingdomcomecombat$setBodyBloodPercent(double percent) {
        kingdomcomecombat$bodyBloodPercent = Math.max(0.0, Math.min(BloodiedEquipment.MAX_BLOOD_PERCENT, percent));
    }

    @Override
    public double kingdomcomecombat$getBodyBloodPercent() {
        return kingdomcomecombat$bodyBloodPercent;
    }

    @Override
    public void kingdomcomecombat$setDragonArmorBroken(boolean broken) {
        kingdomcomecombat$dragonArmorBroken = broken;
    }

    @Override
    public boolean kingdomcomecombat$isDragonArmorBroken() {
        return kingdomcomecombat$dragonArmorBroken;
    }
}
