package com.kingdomcomecombat.ai;

import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.ComboMoveConfig;

public class HumanoidCombatAiState {
    public CombatDirection direction = CombatDirection.RIGHT;
    public int attackCooldownTicks = 0;
    public int decisionTicks = 0;
    public int forcedCounterAttackTicks = 0;
    public int handSwitchCooldownTicks = 0;
    public int dodgeCooldownTicks = 0;
    public int forwardStepChaseCooldownTicks = 0;
    public double defensivePressure = 0.0;
    public double perfectBlockPenalty = 0.0;
    public int attacksSincePerfectBlock = 0;
    public int forcedPerfectBlockTicks = 0;
    public int forcedCounterDelayTicks = 0;
    public int ticksSinceLastAttack = 0;
    public int followUpAttacksRemaining = 0;
    public int stanceSwitchLockTicks = 0;
    public CombatDirection forcedCounterDirection = null;
    public CombatDirection pendingPostAttackDirection = null;
    public int pendingPostAttackHandSwitchCooldownTicks = -1;
    public ComboMoveConfig plannedCombo = null;
    public int plannedComboIndex = 0;
    public boolean equipmentInitialized = false;
    public int visibilityCheckTicks = 0;
    public int visibilityTargetId = -1;
    public boolean cachedTargetVisible = false;
    public boolean stanceSynced = false;

    public void tickCooldowns() {
        ticksSinceLastAttack++;

        if (attackCooldownTicks > 0) {
            attackCooldownTicks--;
        }

        if (decisionTicks > 0) {
            decisionTicks--;
        }

        if (forcedCounterAttackTicks > 0) {
            forcedCounterAttackTicks--;
        }

        if (handSwitchCooldownTicks > 0) {
            handSwitchCooldownTicks--;
        }

        if (stanceSwitchLockTicks > 0) {
            stanceSwitchLockTicks--;
        }

        if (dodgeCooldownTicks > 0) {
            dodgeCooldownTicks--;
        }

        if (forwardStepChaseCooldownTicks > 0) {
            forwardStepChaseCooldownTicks--;
        }

        if (forcedPerfectBlockTicks > 0) {
            forcedPerfectBlockTicks--;
        }

        defensivePressure = Math.max(0.0, defensivePressure - 0.0025);
        perfectBlockPenalty = Math.max(0.0, perfectBlockPenalty - 0.0025);

        if (forcedCounterDelayTicks > 0) {
            forcedCounterDelayTicks--;
        }
    }
}
