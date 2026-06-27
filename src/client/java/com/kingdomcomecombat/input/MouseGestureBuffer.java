package com.kingdomcomecombat.client.input;

import com.kingdomcomecombat.combat.CombatDirection;

public class MouseGestureBuffer {
    private static final double STANCE_THRESHOLD = 8.0;
    private static final int COOLDOWN_TICKS = 5;

    private double accumulatedX = 0.0;
    private double accumulatedY = 0.0;
    private int cooldownTicks = 0;

    public void tick(double dx, double dy) {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        accumulatedX += dx;
        accumulatedY += dy;
    }

    public CombatDirection consumeDirectionIfReady() {
        if (cooldownTicks > 0) {
            return null;
        }

        double absX = Math.abs(accumulatedX);
        double absY = Math.abs(accumulatedY);

        if (absX < STANCE_THRESHOLD && absY < STANCE_THRESHOLD) {
            return null;
        }

        CombatDirection result;

        if (absX > absY) {
            result = accumulatedX > 0
                    ? CombatDirection.RIGHT
                    : CombatDirection.LEFT;
        } else {
            result = accumulatedY > 0
                    ? CombatDirection.DOWN
                    : CombatDirection.UP;
        }

        reset();
        cooldownTicks = COOLDOWN_TICKS;

        return result;
    }

    private void reset() {
        accumulatedX = 0.0;
        accumulatedY = 0.0;
    }
}