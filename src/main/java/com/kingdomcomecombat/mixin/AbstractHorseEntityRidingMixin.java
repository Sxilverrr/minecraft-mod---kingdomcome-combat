package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.riding.KccHorseRidingData;
import com.kingdomcomecombat.riding.ServerHorseControlState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorseEntity.class)
public abstract class AbstractHorseEntityRidingMixin extends AnimalEntity implements KccHorseRidingData {
    @Unique private static final String KCC_MAX_STAMINA_KEY = "kccHorseMaxStamina";
    @Unique private static final String KCC_STAMINA_KEY = "kccHorseStamina";
    @Unique private static final String KCC_SPEED_KEY = "kccHorseCurrentSpeed";
    @Unique private static final String KCC_EXHAUSTION_LOCK_KEY = "kccHorseExhaustionLockTicks";
    @Unique private static final double KCC_MIN_STAMINA = 60.0;
    @Unique private static final double KCC_MAX_STAMINA = 120.0;
    @Unique private static final double KCC_WALK_SPEED_FACTOR = 0.25;
    @Unique private static final double KCC_RUN_THRESHOLD = 0.60;
    @Unique private static final double KCC_SPEED_GAIN_PER_TAP = 0.33;
    @Unique private static final double KCC_MAX_SPRINT_SPEED_FACTOR = 1.20;
    @Unique private static final double KCC_DECAY_PER_TICK = 0.02 / 20.0;
    @Unique private static final double KCC_STAMINA_COST_PER_TICK = 2.0 / 20.0;
    @Unique private static final double KCC_STAMINA_REGEN_PER_TICK = 10.0 / 20.0;
    @Unique private static final double KCC_BRAKE_STAMINA_COST = 8.0;
    @Unique private static final double KCC_JUMP_STAMINA_COST = 10.0;
    @Unique private static final int KCC_EXHAUSTION_LOCK_TICKS = 5 * 20;

    @Unique private double kccMaxHorseStamina = 0.0;
    @Unique private double kccHorseStamina = 0.0;
    @Unique private double kccCurrentSpeed = 0.0;
    @Unique private boolean kccLastSprintPressed = false;
    @Unique private int kccBrakeDelayTicks = 0;
    @Unique private int kccPanicDelayTicks = 0;
    @Unique private int kccExhaustionLockTicks = 0;

    protected AbstractHorseEntityRidingMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow public abstract void setAngry(int ticks);

    @Inject(method = "initAttributes", at = @At("RETURN"))
    private void kingdomcomecombat$initHorseStamina(Random random, CallbackInfo ci) {
        kingdomcomecombat$ensureHorseStamina(random);
    }

    @Inject(method = "writeCustomData", at = @At("RETURN"))
    private void kingdomcomecombat$writeHorseRidingData(WriteView view, CallbackInfo ci) {
        kingdomcomecombat$ensureHorseStamina(this.random);
        view.putDouble(KCC_MAX_STAMINA_KEY, kccMaxHorseStamina);
        view.putDouble(KCC_STAMINA_KEY, kccHorseStamina);
        view.putDouble(KCC_SPEED_KEY, kccCurrentSpeed);
        view.putInt(KCC_EXHAUSTION_LOCK_KEY, kccExhaustionLockTicks);
    }

    @Inject(method = "readCustomData", at = @At("RETURN"))
    private void kingdomcomecombat$readHorseRidingData(ReadView view, CallbackInfo ci) {
        kccMaxHorseStamina = view.getDouble(KCC_MAX_STAMINA_KEY, 0.0);
        kccHorseStamina = view.getDouble(KCC_STAMINA_KEY, kccMaxHorseStamina);
        kccCurrentSpeed = Math.max(0.0, view.getDouble(KCC_SPEED_KEY, 0.0));
        kccExhaustionLockTicks = Math.max(0, view.getInt(KCC_EXHAUSTION_LOCK_KEY, 0));
        kingdomcomecombat$ensureHorseStamina(this.random);
    }

    @Inject(method = "setChildAttributes", at = @At("RETURN"))
    private void kingdomcomecombat$inheritHorseStamina(
            PassiveEntity otherParent,
            AbstractHorseEntity child,
            CallbackInfo ci
    ) {
        kingdomcomecombat$ensureHorseStamina(this.random);
        double other = KCC_MAX_STAMINA;
        if (otherParent instanceof AbstractHorseEntityRidingMixin otherMixin) {
            otherMixin.kingdomcomecombat$ensureHorseStamina(otherParent.getRandom());
            other = otherMixin.kccMaxHorseStamina;
        }

        if ((Object) child instanceof AbstractHorseEntityRidingMixin childMixin) {
            double inherited = (kccMaxHorseStamina + other) * 0.5
                    + (child.getRandom().nextDouble() - 0.5) * 12.0;
            childMixin.kccMaxHorseStamina = MathHelper.clamp(inherited, KCC_MIN_STAMINA, KCC_MAX_STAMINA);
            childMixin.kccHorseStamina = childMixin.kccMaxHorseStamina;
            childMixin.kccCurrentSpeed = 0.0;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void kingdomcomecombat$tickHorseRidingState(CallbackInfo ci) {
        kingdomcomecombat$ensureHorseStamina(this.random);
        boolean exhaustionLockedThisTick = kccExhaustionLockTicks > 0;
        if (exhaustionLockedThisTick) {
            kccExhaustionLockTicks--;
            kccCurrentSpeed = 0.0;
        }
        LivingEntity passenger = this.getControllingPassenger();
        if (!(passenger instanceof PlayerEntity player)) {
            kccLastSprintPressed = false;
            kccCurrentSpeed = Math.max(0.0, kccCurrentSpeed - KCC_DECAY_PER_TICK);
            return;
        }

        ServerHorseControlState.Input input = kingdomcomecombat$getHorseInput(player);
        boolean sprintTap = input.sprintPressed() && !kccLastSprintPressed;
        kccLastSprintPressed = input.sprintPressed();

        if (sprintTap
                && !exhaustionLockedThisTick
                && kccBrakeDelayTicks <= 0
                && kccPanicDelayTicks <= 0) {
            kccCurrentSpeed += KCC_SPEED_GAIN_PER_TAP;
            if (kccCurrentSpeed > 1.0) {
                kccPanicDelayTicks = 12;
                this.setAngry(24);
            }
        }

        if (input.jumpPressed() && !exhaustionLockedThisTick) {
            kingdomcomecombat$consumeHorseStamina(KCC_JUMP_STAMINA_COST);
            exhaustionLockedThisTick = kccExhaustionLockTicks > 0;
        }

        if (input.forward() < -0.25F && kccBrakeDelayTicks <= 0 && kccPanicDelayTicks <= 0) {
            kccBrakeDelayTicks = 8;
            kingdomcomecombat$consumeHorseStamina(KCC_BRAKE_STAMINA_COST);
            exhaustionLockedThisTick = kccExhaustionLockTicks > 0;
        }

        if (kccPanicDelayTicks > 0) {
            kccPanicDelayTicks--;
            this.setAngry(20);
            if (kccPanicDelayTicks == 0) {
                if (!this.getWorld().isClient()) {
                    passenger.stopRiding();
                    passenger.addVelocity(0.0, 0.42, 0.0);
                    passenger.velocityModified = true;
                }
                kccCurrentSpeed = 0.0;
            }
            return;
        }

        if (kccBrakeDelayTicks > 0) {
            kccBrakeDelayTicks--;
            if (kccBrakeDelayTicks == 0) {
                this.setAngry(22);
            }
            kccCurrentSpeed *= 0.72;
            if (kccCurrentSpeed < 0.03) {
                kccCurrentSpeed = 0.0;
            }
            return;
        }

        kccCurrentSpeed = Math.max(0.0, kccCurrentSpeed - KCC_DECAY_PER_TICK);
        boolean shouldMoveForward = input.forward() > 0.25F
                || Math.abs(input.sideways()) > 0.25F
                || kccCurrentSpeed > 0.001;
        boolean consumedStamina = false;
        if (shouldMoveForward && kccCurrentSpeed > KCC_RUN_THRESHOLD) {
            kccHorseStamina = Math.max(0.0, kccHorseStamina - KCC_STAMINA_COST_PER_TICK);
            consumedStamina = true;
            if (kccHorseStamina <= 0.0) {
                kccHorseStamina = 0.0;
                kccCurrentSpeed = 0.0;
                kccExhaustionLockTicks = KCC_EXHAUSTION_LOCK_TICKS;
            }
        }

        if (!consumedStamina) {
            kccHorseStamina = Math.min(kccMaxHorseStamina, kccHorseStamina + KCC_STAMINA_REGEN_PER_TICK);
        }
    }

    @Inject(method = "getControlledRotation", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$controlledHorseRotation(
            LivingEntity controllingPassenger,
            CallbackInfoReturnable<Vec2f> cir
    ) {
        if (controllingPassenger instanceof PlayerEntity) {
            cir.setReturnValue(kingdomcomecombat$getControlledRotation(controllingPassenger));
        }
    }

    @Inject(method = "getControlledMovementInput", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$controlledHorseInput(
            PlayerEntity controllingPlayer,
            Vec3d movementInput,
            CallbackInfoReturnable<Vec3d> cir
    ) {
        cir.setReturnValue(kingdomcomecombat$getControlledMovementInput(controllingPlayer));
    }

    @Inject(method = "getSaddledSpeed", at = @At("RETURN"), cancellable = true)
    private void kingdomcomecombat$controlledHorseSpeed(
            PlayerEntity controllingPlayer,
            CallbackInfoReturnable<Float> cir
    ) {
        cir.setReturnValue(kingdomcomecombat$modifySaddledSpeed(controllingPlayer, cir.getReturnValueF()));
    }

    @Override
    public Vec2f kingdomcomecombat$getControlledRotation(LivingEntity controllingPassenger) {
        PlayerEntity player = (PlayerEntity) controllingPassenger;
        ServerHorseControlState.Input input = kingdomcomecombat$getHorseInput(player);
        if (!this.getWorld().isClient()) this.setYaw(input.yaw());
        return new Vec2f(controllingPassenger.getPitch() * 0.5F, this.getYaw());
    }

    @Override
    public Vec3d kingdomcomecombat$getControlledMovementInput(PlayerEntity controllingPlayer) {
        ServerHorseControlState.Input input = kingdomcomecombat$getHorseInput(controllingPlayer);
        boolean shouldMoveForward = input.forward() > 0.1F
                || Math.abs(input.sideways()) > 0.1F || kccCurrentSpeed > 0.001;
        double forward = shouldMoveForward && kccPanicDelayTicks <= 0 && kccBrakeDelayTicks <= 0 ? 1.0 : 0.0;
        return new Vec3d(0.0, 0.0, forward);
    }

    @Override
    public float kingdomcomecombat$modifySaddledSpeed(PlayerEntity controllingPlayer, float vanillaSpeed) {
        double speedFactor;
        if (kccCurrentSpeed >= KCC_RUN_THRESHOLD) {
            double sprintProgress = (Math.min(1.0, kccCurrentSpeed) - KCC_RUN_THRESHOLD) / (1.0 - KCC_RUN_THRESHOLD);
            speedFactor = 1.0 + sprintProgress * (KCC_MAX_SPRINT_SPEED_FACTOR - 1.0);
        } else {
            speedFactor = KCC_WALK_SPEED_FACTOR
                    + (Math.max(0.0, kccCurrentSpeed) / KCC_RUN_THRESHOLD)
                    * (1.0 - KCC_WALK_SPEED_FACTOR);
        }

        if (kccBrakeDelayTicks > 0 || kccPanicDelayTicks > 0) {
            speedFactor *= 0.25;
        }

        ServerHorseControlState.Input input = kingdomcomecombat$getHorseInput(controllingPlayer);
        double turnAmount = Math.min(1.0, Math.abs(input.sideways()));
        if (turnAmount > 0.05 && kccCurrentSpeed > 0.001) {
            speedFactor *= 1.0 - turnAmount * 0.20;
        }

        return (float) (vanillaSpeed * speedFactor);
    }

    @Unique
    private void kingdomcomecombat$ensureHorseStamina(Random random) {
        boolean initializedMax = kccMaxHorseStamina > 0.0;
        if (kccMaxHorseStamina <= 0.0) {
            kccMaxHorseStamina = KCC_MIN_STAMINA + random.nextDouble() * (KCC_MAX_STAMINA - KCC_MIN_STAMINA);
        }
        if (!initializedMax && kccHorseStamina <= 0.0) {
            kccHorseStamina = kccMaxHorseStamina;
        }
        kccHorseStamina = MathHelper.clamp(kccHorseStamina, 0.0, kccMaxHorseStamina);
    }

    @Unique
    private ServerHorseControlState.Input kingdomcomecombat$getHorseInput(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return ServerHorseControlState.get(serverPlayer);
        }

        return new ServerHorseControlState.Input(
                kingdomcomecombat$clampInput(player.sidewaysSpeed),
                kingdomcomecombat$clampInput(player.forwardSpeed),
                player.isSprinting(),
                false,
                player.getYaw(),
                player.getWorld().getTime()
        );
    }

    @Unique
    private void kingdomcomecombat$consumeHorseStamina(double cost) {
        if (cost <= 0.0 || kccExhaustionLockTicks > 0) {
            return;
        }
        kccHorseStamina = Math.max(0.0, kccHorseStamina - cost);
        if (kccHorseStamina <= 0.0) {
            kccHorseStamina = 0.0;
            kccCurrentSpeed = 0.0;
            kccExhaustionLockTicks = KCC_EXHAUSTION_LOCK_TICKS;
        }
    }

    @Unique
    private static float kingdomcomecombat$clampInput(float value) {
        return Math.max(-1.0F, Math.min(1.0F, value));
    }

    @Override
    public double kingdomcomecombat$getHorseMaxStamina() {
        kingdomcomecombat$ensureHorseStamina(this.random);
        return kccMaxHorseStamina;
    }

    @Override
    public double kingdomcomecombat$getHorseStamina() {
        kingdomcomecombat$ensureHorseStamina(this.random);
        return kccHorseStamina;
    }

    @Override
    public double kingdomcomecombat$getHorseCurrentSpeed() {
        return MathHelper.clamp(kccCurrentSpeed, 0.0, 1.0);
    }
}
