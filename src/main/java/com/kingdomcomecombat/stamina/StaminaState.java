package com.kingdomcomecombat.stamina;

public class StaminaState {
    private double current;
    private double max;
    private int regenDelayTicks;

    public StaminaState(double max) {
        this.max = sanitizeMax(max);
        this.current = this.max;
        this.regenDelayTicks = 0;
    }

    public double current() {
        return current;
    }

    public double max() {
        return max;
    }

    public boolean hasAtLeast(double amount) {
        return current >= amount;
    }

    public boolean consume(double amount) {
        amount = Math.max(0.0, amount);
        if (current < amount) {
            return false;
        }

        current -= amount;
        regenDelayTicks = StaminaConfig.DEFAULT_REGEN_DELAY_TICKS;
        return true;
    }

    public void damage(double amount) {
        current = clamp(current - Math.max(0.0, amount), 0.0, max);
        regenDelayTicks = StaminaConfig.DEFAULT_REGEN_DELAY_TICKS;
    }

    public void damage(double amount, int regenDelayTicks) {
        current = clamp(current - Math.max(0.0, amount), 0.0, max);
        this.regenDelayTicks = Math.max(this.regenDelayTicks, Math.max(0, regenDelayTicks));
    }

    public void restore(double amount) {
        current = clamp(current + Math.max(0.0, amount), 0.0, max);
    }

    public void setCurrent(double value) {
        current = clamp(value, 0.0, max);
    }

    public void tick(double resolvedMax, double regenPerTick) {
        max = sanitizeMax(resolvedMax);
        current = clamp(current, 0.0, max);

        if (regenDelayTicks > 0) {
            regenDelayTicks--;
            return;
        }

        restore(regenPerTick);
    }

    public boolean isIdleAtMaximum(double resolvedMax) {
        double sanitized = sanitizeMax(resolvedMax);
        return regenDelayTicks <= 0
                && Math.abs(max - sanitized) <= 0.0001
                && current >= sanitized - 0.0001;
    }

    private static double sanitizeMax(double value) {
        return Math.max(1.0, value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
