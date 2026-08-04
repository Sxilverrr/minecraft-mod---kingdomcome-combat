package com.kingdomcomecombat.ai;

import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.ServerCombatControlState;
import com.kingdomcomecombat.equipment.MobCombatAttributes;
import com.kingdomcomecombat.equipment.MobCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.network.IncomingAttackWarningPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ConfiguredMobAttackTicker {
    private static final Map<UUID, State> STATES = new HashMap<>();
    private ConfiguredMobAttackTicker() {}

    public static boolean disablesVanillaAttack(MobEntity mob) {
        return MobCombatAttributesRegistry.get(mob)
                .map(MobCombatAttributes::attackBehavior)
                .map(behavior -> behavior.mode() != MobCombatAttributes.AttackBehavior.Mode.VANILLA)
                .orElse(false);
    }

    public static void tick(MobEntity mob) {
        MobCombatAttributes attributes = MobCombatAttributesRegistry.get(mob).orElse(null);
        if (attributes == null || attributes.attackBehavior().mode() == MobCombatAttributes.AttackBehavior.Mode.VANILLA) {
            STATES.remove(mob.getUuid());
            return;
        }
        if (!mob.isAlive() || mob.isRemoved()) {
            STATES.remove(mob.getUuid());
            return;
        }
        State state = STATES.computeIfAbsent(mob.getUuid(), ignored -> new State());
        if (mob instanceof SpiderEntity && state.active <= 0 && mob.getVelocity().y > 0.0) {
            Vec3d velocity = mob.getVelocity();
            mob.setVelocity(velocity.x, 0.0, velocity.z);
            mob.velocityModified = true;
        }
        if (state.cooldown > 0) state.cooldown--;

        LivingEntity target = mob.getTarget();
        if ((target == null || !target.isAlive()) && mob instanceof IronGolemEntity golem) {
            target = acquireNearestHostile(golem);
        }
        if (target == null || !target.isAlive()) {
            state.cancel();
            return;
        }
        MobCombatAttributes.AttackBehavior behavior = attributes.attackBehavior();
        if (state.windup > 0) {
            mob.getNavigation().stop();
            face(mob, target);
            if (!state.warningSent && state.windup <= 8) {
                sendWarning(mob, target, state.defenseTier);
                state.warningSent = true;
            }
            if (--state.windup == 0) beginMovement(mob, target, behavior, state);
            return;
        }
        if (state.active > 0) {
            mob.setAttacking(true);
            face(mob, target);
            if (state.chargeVelocity.lengthSquared() > 0.0001) {
                mob.setVelocity(state.chargeVelocity.x,
                        state.airborneLunge ? state.chargeVelocity.y : mob.getVelocity().y,
                        state.chargeVelocity.z);
                mob.velocityModified = true;
            }
            state.active--;
            if (!state.hitTargets.contains(target.getUuid())
                    && mob.getBoundingBox().expand(0.18).intersects(target.getBoundingBox())) {
                float damage = (float) Math.max(1.0, mob.getAttributeValue(EntityAttributes.ATTACK_DAMAGE));
                target.damage((ServerWorld) mob.getWorld(), mob.getDamageSources().mobAttack(mob), damage);
            }
            if (state.active == 0) {
                mob.setAttacking(false);
                state.cooldown = behavior.cooldownTicks();
                state.hitTargets.clear();
                state.warningSent = false;
            }
            return;
        }
        if (mob instanceof IronGolemEntity && mob.distanceTo(target) > behavior.startDistance()) {
            // Its configured attack replaces the vanilla melee goal, so that goal can no longer
            // supply path finding. Keep pursuing until the custom lunge can begin.
            mob.getNavigation().startMovingTo(target, 1.0);
            return;
        }
        if (state.cooldown > 0
                || com.kingdomcomecombat.combat.ServerCombatState.getAttack(target.getUuid()) != null) return;

        state.windup = Math.max(1, behavior.windupTicks());
        state.selectedMode = behavior.mode() == MobCombatAttributes.AttackBehavior.Mode.MIXED
                ? (mob.getRandom().nextBoolean()
                        ? MobCombatAttributes.AttackBehavior.Mode.LUNGE
                        : MobCombatAttributes.AttackBehavior.Mode.JUMP)
                : behavior.mode();
        state.defenseTier = state.selectedMode == MobCombatAttributes.AttackBehavior.Mode.JUMP
                ? behavior.jumpDefenseTier() : behavior.lungeDefenseTier();
        mob.getNavigation().stop();
        mob.setVelocity(0.0, mob.getVelocity().y, 0.0);
        face(mob, target);
        state.warningSent = false;
        if (state.windup <= 8) {
            sendWarning(mob, target, state.defenseTier);
            state.warningSent = true;
        }
    }

    private static LivingEntity acquireNearestHostile(IronGolemEntity golem) {
        if (!(golem.getWorld() instanceof ServerWorld world) || world.getTime() % 10L != 0L) {
            return null;
        }
        HostileEntity nearest = world.getEntitiesByClass(
                        HostileEntity.class,
                        golem.getBoundingBox().expand(16.0),
                        hostile -> hostile.isAlive()
                                && !(hostile instanceof CreeperEntity)
                                && golem.canTarget(hostile)
                ).stream()
                .min(Comparator.comparingDouble(golem::squaredDistanceTo))
                .orElse(null);
        if (nearest != null) {
            golem.setTarget(nearest);
        }
        return nearest;
    }

    private static void beginMovement(MobEntity mob, LivingEntity target,
                                      MobCombatAttributes.AttackBehavior behavior, State state) {
        mob.setAttacking(true);
        if (mob instanceof IronGolemEntity && mob.getWorld() instanceof ServerWorld world) {
            // Vanilla iron golem renderers start their arm-swing timer from entity status 4.
            world.sendEntityStatus(mob, (byte) 4);
        }
        Vec3d delta = target.getBoundingBox().getCenter().subtract(mob.getBoundingBox().getCenter());
        double speed = Math.max(behavior.speed(), 3.0 / Math.max(1, behavior.activeTicks()));
        if (behavior.airborneLunge() && delta.lengthSquared() > 0.0001) {
            Vec3d velocity = delta.normalize().multiply(speed);
            mob.setVelocity(velocity);
            mob.velocityModified = true;
            state.chargeVelocity = velocity;
            state.airborneLunge = true;
            state.active = behavior.activeTicks();
            state.hitTargets.clear();
            return;
        }
        Vec3d horizontal = new Vec3d(delta.x, 0.0, delta.z);
        if (horizontal.lengthSquared() < 0.0001) horizontal = Vec3d.fromPolar(0.0F, mob.getYaw());
        Vec3d forward = horizontal.normalize().multiply(speed);
        double y = state.selectedMode == MobCombatAttributes.AttackBehavior.Mode.JUMP
                ? behavior.jumpVelocity() : mob.getVelocity().y;
        mob.setVelocity(forward.x, y, forward.z);
        mob.velocityModified = true;
        state.chargeVelocity = new Vec3d(forward.x, 0.0, forward.z);
        state.airborneLunge = false;
        state.active = behavior.activeTicks();
        state.hitTargets.clear();
    }

    private static void sendWarning(MobEntity mob, LivingEntity target,
                                    MobCombatAttributes.MeleeDefenseTier defenseTier) {
        if (!(target instanceof ServerPlayerEntity player)) return;
        boolean hasShield = EquipmentCombatAttributesRegistry.isShield(player.getMainHandStack())
                || EquipmentCombatAttributesRegistry.isShield(player.getOffHandStack());
        boolean shieldRequired = defenseTier == MobCombatAttributes.MeleeDefenseTier.SHIELD_BLOCKABLE
                || defenseTier == MobCombatAttributes.MeleeDefenseTier.SHIELD_PERFECT_BLOCK_ONLY;
        int type = defenseTier == MobCombatAttributes.MeleeDefenseTier.UNBLOCKABLE
                || (shieldRequired && !hasShield)
                ? IncomingAttackWarningPayload.UNBLOCKABLE
                : IncomingAttackWarningPayload.DIRECTION_FREE_BLOCK;
        ServerPlayNetworking.send(player, new IncomingAttackWarningPayload(
                mob.getId(), CombatDirection.UP.ordinal(), type));
    }

    private static void face(MobEntity mob, LivingEntity target) {
        if (target instanceof net.minecraft.entity.player.PlayerEntity
                && ServerCombatControlState.isDodging(target)) {
            return;
        }
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        mob.setYaw(yaw); mob.setBodyYaw(yaw); mob.setHeadYaw(yaw);
    }

    public static void interruptCharge(MobEntity mob) {
        State state = STATES.get(mob.getUuid());
        if (state == null) return;
        state.active = 0;
        state.chargeVelocity = Vec3d.ZERO;
        mob.setAttacking(false);
    }

    public static MobCombatAttributes.MeleeDefenseTier currentDefenseTier(
            LivingEntity attacker, MobCombatAttributes.MeleeDefenseTier fallback) {
        State state = STATES.get(attacker.getUuid());
        return state == null || state.defenseTier == null ? fallback : state.defenseTier;
    }

    public static boolean currentAttackIgnoresBlockDirection(LivingEntity attacker) {
        State state = STATES.get(attacker.getUuid());
        return state != null && (state.selectedMode == MobCombatAttributes.AttackBehavior.Mode.LUNGE
                || state.selectedMode == MobCombatAttributes.AttackBehavior.Mode.JUMP);
    }

    public static boolean claimChargeHit(MobEntity attacker, LivingEntity target) {
        State state = STATES.get(attacker.getUuid());
        return state != null && state.active > 0 && state.hitTargets.add(target.getUuid());
    }

    private static final class State {
        int windup, active, cooldown;
        boolean warningSent;
        boolean airborneLunge;
        MobCombatAttributes.AttackBehavior.Mode selectedMode = MobCombatAttributes.AttackBehavior.Mode.LUNGE;
        MobCombatAttributes.MeleeDefenseTier defenseTier;
        final Set<UUID> hitTargets = new HashSet<>();
        Vec3d chargeVelocity = Vec3d.ZERO;
        void cancel() {
            windup = 0; active = 0; hitTargets.clear(); warningSent = false;
            chargeVelocity = Vec3d.ZERO; airborneLunge = false;
        }
    }
}
