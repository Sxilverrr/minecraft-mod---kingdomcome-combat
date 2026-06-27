package com.kingdomcomecombat.ai;

public class BeastCombatAiState {
    public BeastCombatAiProfile.Mode mode;
    public Phase phase = Phase.PREPARE;
    public int attackCooldownTicks = 0;
    public int phaseTicks = 0;
    public int circleDirection = 1;
    public double circleSpeed = 0.1;
    public boolean pursuing = false;
    public int chainAttacks = 0;
    public boolean hadActiveAttack = false;

    public void tick() {
        if (attackCooldownTicks > 0) {
            attackCooldownTicks--;
        }
        if (phaseTicks > 0) {
            phaseTicks--;
        }
    }

    public void resetForMode(BeastCombatAiProfile profile) {
        if (mode == profile.mode()) {
            return;
        }
        mode = profile.mode();
        phase = profile.mode() == BeastCombatAiProfile.Mode.CIRCLE_LUNGE
                ? Phase.CIRCLING
                : Phase.HOLDING;
        attackCooldownTicks = 0;
        phaseTicks = 0;
        pursuing = false;
        chainAttacks = 0;
        hadActiveAttack = false;
    }

    public enum Phase {
        PREPARE,
        CIRCLING,
        HOLDING,
        RUSHING,
        PURSUING
    }
}
