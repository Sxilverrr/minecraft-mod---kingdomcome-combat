package com.kingdomcomecombat.combat;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ActiveServerAttack {
    public final CombatDirection direction;

    /**
     * 攻击开始瞬间的朝向。
     */
    public final float startYaw;

    /**
     * 客户端锁定目标。
     * -1 表示没有锁定目标。
     */
    public final int targetEntityId;
    public final boolean lockedLunge;
    public boolean movementKeyLunge;
    public int lungeForwardInput = 1;
    public int lungeSideInput = 0;
    public int lungeProgressTicks = 0;
    public double accumulatedLungeSpeed = 0.0;
    public double mobLungeStartX = Double.NaN;
    public double mobLungeStartZ = Double.NaN;
    public final int attackTotalTicks;
    public final float animationSpeedMultiplier;
    public final ComboMoveConfig comboMove;
    public final AttackMoveConfig moveConfig;
    public final long startWorldTick;
    public final boolean perfectCounterSlow;
    public final double startupSlowdown;

    public int ageTicks = 0;
    private int realAgeTicks = 0;
    private double elapsedTicks = 0.0;
    public boolean connectedOrNormalBlocked = false;
    public boolean perfectBlocked = false;
    public boolean directHitTriggered = false;
    public boolean weaponClashTriggered = false;
    public int delayedLethalTargetEntityId = -1;
    public float delayedLethalDamage = 0.0F;
    public long clientAttackInstanceId = 0L;

    public final Set<UUID> hitTargets = new HashSet<>();
    public final Set<UUID> bloodiedTargets = new HashSet<>();
    public final Map<UUID, Double> targetArmorReductions = new HashMap<>();
    public final Set<Integer> triggeredComboChainEvents = new HashSet<>();

    public ActiveServerAttack(
            CombatDirection direction,
            float startYaw,
            int targetEntityId,
            int attackTotalTicks,
            float animationSpeedMultiplier
    ) {
        this(direction, startYaw, targetEntityId, targetEntityId >= 0, attackTotalTicks, animationSpeedMultiplier, null);
    }

    public ActiveServerAttack(
            CombatDirection direction,
            float startYaw,
            int targetEntityId,
            boolean lockedLunge,
            int attackTotalTicks,
            float animationSpeedMultiplier
    ) {
        this(direction, startYaw, targetEntityId, lockedLunge, attackTotalTicks, animationSpeedMultiplier, null);
    }

    public ActiveServerAttack(
            CombatDirection direction,
            float startYaw,
            int targetEntityId,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove
    ) {
        this(direction, startYaw, targetEntityId, targetEntityId >= 0, attackTotalTicks, animationSpeedMultiplier, comboMove, null, 0L);
    }

    public ActiveServerAttack(
            CombatDirection direction,
            float startYaw,
            int targetEntityId,
            boolean lockedLunge,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove
    ) {
        this(direction, startYaw, targetEntityId, lockedLunge, attackTotalTicks, animationSpeedMultiplier, comboMove, null, 0L);
    }

    public ActiveServerAttack(
            CombatDirection direction,
            float startYaw,
            int targetEntityId,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            long startWorldTick
    ) {
        this(direction, startYaw, targetEntityId, targetEntityId >= 0, attackTotalTicks, animationSpeedMultiplier, comboMove, null, startWorldTick);
    }

    public ActiveServerAttack(
            CombatDirection direction,
            float startYaw,
            int targetEntityId,
            boolean lockedLunge,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            long startWorldTick
    ) {
        this(direction, startYaw, targetEntityId, lockedLunge, attackTotalTicks, animationSpeedMultiplier, comboMove, null, startWorldTick);
    }

    public ActiveServerAttack(
            CombatDirection direction,
            float startYaw,
            int targetEntityId,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            AttackMoveConfig moveConfig,
            long startWorldTick
    ) {
        this(
                direction,
                startYaw,
                targetEntityId,
                targetEntityId >= 0,
                attackTotalTicks,
                animationSpeedMultiplier,
                comboMove,
                moveConfig,
                startWorldTick,
                false
        );
    }

    public ActiveServerAttack(
            CombatDirection direction,
            float startYaw,
            int targetEntityId,
            boolean lockedLunge,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            AttackMoveConfig moveConfig,
            long startWorldTick
    ) {
        this(
                direction,
                startYaw,
                targetEntityId,
                lockedLunge,
                attackTotalTicks,
                animationSpeedMultiplier,
                comboMove,
                moveConfig,
                startWorldTick,
                false
        );
    }

    public ActiveServerAttack(
            CombatDirection direction,
            float startYaw,
            int targetEntityId,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            AttackMoveConfig moveConfig,
            long startWorldTick,
            boolean perfectCounterSlow
    ) {
        this(
                direction,
                startYaw,
                targetEntityId,
                targetEntityId >= 0,
                attackTotalTicks,
                animationSpeedMultiplier,
                comboMove,
                moveConfig,
                startWorldTick,
                perfectCounterSlow,
                0.0
        );
    }

    public ActiveServerAttack(
            CombatDirection direction,
            float startYaw,
            int targetEntityId,
            boolean lockedLunge,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            AttackMoveConfig moveConfig,
            long startWorldTick,
            boolean perfectCounterSlow
    ) {
        this(
                direction,
                startYaw,
                targetEntityId,
                lockedLunge,
                attackTotalTicks,
                animationSpeedMultiplier,
                comboMove,
                moveConfig,
                startWorldTick,
                perfectCounterSlow,
                0.0
        );
    }

    public ActiveServerAttack(
            CombatDirection direction,
            float startYaw,
            int targetEntityId,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            AttackMoveConfig moveConfig,
            long startWorldTick,
            boolean perfectCounterSlow,
            double startupSlowdown
    ) {
        this(
                direction,
                startYaw,
                targetEntityId,
                targetEntityId >= 0,
                attackTotalTicks,
                animationSpeedMultiplier,
                comboMove,
                moveConfig,
                startWorldTick,
                perfectCounterSlow,
                startupSlowdown
        );
    }

    public ActiveServerAttack(
            CombatDirection direction,
            float startYaw,
            int targetEntityId,
            boolean lockedLunge,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            AttackMoveConfig moveConfig,
            long startWorldTick,
            boolean perfectCounterSlow,
            double startupSlowdown
    ) {
        this.direction = direction;
        this.startYaw = startYaw;
        this.targetEntityId = targetEntityId;
        this.lockedLunge = lockedLunge;
        this.attackTotalTicks = CombatTiming.sanitizeAttackTotalTicks(attackTotalTicks);
        this.animationSpeedMultiplier = Math.max(0.05F, animationSpeedMultiplier);
        this.comboMove = comboMove;
        this.moveConfig = moveConfig;
        this.startWorldTick = startWorldTick;
        this.perfectCounterSlow = perfectCounterSlow;
        this.startupSlowdown = Math.max(0.0, Math.min(0.95, startupSlowdown));
    }

    public void setLungeInput(boolean movementKeyPressed, int forwardInput, int sideInput) {
        movementKeyLunge = movementKeyPressed;
        lungeForwardInput = Math.max(0, Math.min(1, forwardInput));
        lungeSideInput = Math.max(-1, Math.min(1, sideInput));
    }

    public AttackMoveConfig moveConfig() {
        return moveConfig != null ? moveConfig : AttackMoveConfigs.get(direction);
    }

    public boolean isActiveFrame() {
        return CombatTiming.isAttackActiveFrame(ageTicks, attackTotalTicks);
    }

    public boolean canMove() {
        if (comboMove != null) {
            return false;
        }

        return ageTicks >= CombatTiming.getAttackTransitionStartTick(
                attackTotalTicks,
                transitionTicks()
        );
    }

    public boolean isFinished() {
        return CombatTiming.isAttackFinished(ageTicks, attackTotalTicks);
    }

    public int transitionTicks() {
        int rawTransitionTicks = moveConfig != null
                ? moveConfig.transitionTicks()
                : CombatTiming.LIGHT_ATTACK_TRANSITION_TICKS;
        return CombatTiming.scaleTicksForAttackSpeed(rawTransitionTicks, animationSpeedMultiplier);
    }

    public double getForwardSpeed() {
        if (comboMove != null && !comboMove.lunges()) {
            return 0.0;
        }

        return CombatTiming.getAttackForwardSpeed(ageTicks, attackTotalTicks);
    }

    public float getAnimationElapsedSeconds() {
        return (float) (elapsedTicks / 20.0) * animationSpeedMultiplier;
    }

    public void tick() {
        elapsedTicks += currentTickScale();
        realAgeTicks++;
        ageTicks = Math.max(ageTicks, (int) Math.floor(elapsedTicks));
    }

    public double currentTickScale() {
        double scale = 1.0;
        if (perfectCounterSlow && realAgeTicks < CombatControlConfig.PERFECT_COUNTER_SLOW_TICKS) {
            scale = Math.min(scale, CombatControlConfig.PERFECT_COUNTER_ATTACK_SPEED_SCALE);
        }

        if (startupSlowdown > 0.0 && realAgeTicks < CombatControlConfig.PERFECT_COUNTER_SLOW_TICKS) {
            scale = Math.min(scale, 1.0 - startupSlowdown);
        }

        return scale;
    }
}
