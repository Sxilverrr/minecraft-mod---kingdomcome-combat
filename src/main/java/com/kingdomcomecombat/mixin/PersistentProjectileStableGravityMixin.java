package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.projectile.StableProjectileAccess;
import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileStableGravityMixin extends ProjectileEntity implements StableProjectileAccess {
    @Unique
    private static final Identifier KINGDOMCOMECOMBAT_STABLE_ID =
            Identifier.of(KingdomComeCombat.MOD_ID, "stable");
    @Unique
    private static final TrackedData<Integer> KINGDOMCOMECOMBAT_STABLE_LEVEL =
            DataTracker.registerData(PersistentProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private PersistentProjectileStableGravityMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void kingdomcomecombat$setStableLevel(int level) {
        getDataTracker().set(KINGDOMCOMECOMBAT_STABLE_LEVEL, Math.max(0, level));
    }

    @Override
    public int kingdomcomecombat$getStableLevel() {
        return getDataTracker().get(KINGDOMCOMECOMBAT_STABLE_LEVEL);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void kingdomcomecombat$trackStableLevel(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(KINGDOMCOMECOMBAT_STABLE_LEVEL, 0);
    }

    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true)
    private void kingdomcomecombat$reduceStableGravity(CallbackInfoReturnable<Double> cir) {
        int stableLevel = Math.max(
                kingdomcomecombat$getStableLevel(),
                kingdomcomecombat$stableLevelFromWeapon()
        );
        if (stableLevel <= 0) {
            return;
        }

        double multiplier = Math.max(0.35, 1.0 - 0.15 * stableLevel);
        cir.setReturnValue(cir.getReturnValueD() * multiplier);
    }

    @Unique
    private int kingdomcomecombat$stableLevelFromWeapon() {
        PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;
        ItemStack weaponStack = projectile.getWeaponStack();
        if (weaponStack == null) {
            return 0;
        }

        ItemEnchantmentsComponent enchantments = weaponStack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            return 0;
        }

        int level = 0;
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(KINGDOMCOMECOMBAT_STABLE_ID)) {
                level = Math.max(level, entry.getIntValue());
            }
        }
        return level;
    }
}
