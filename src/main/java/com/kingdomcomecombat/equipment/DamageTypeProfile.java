package com.kingdomcomecombat.equipment;

public record DamageTypeProfile(
        double thrust,
        double strike,
        double slash
) {
    public DamageTypeProfile {
        thrust = Math.max(0.0, thrust);
        strike = Math.max(0.0, strike);
        slash = Math.max(0.0, slash);
    }

    public static DamageTypeProfile even(double value) {
        return new DamageTypeProfile(value, value, value);
    }

    public double total() {
        return thrust + strike + slash;
    }

    public double weightedAverage(DamageTypeProfile weights) {
        double totalWeight = weights.total();
        if (totalWeight <= 0.000001) {
            return 0.0;
        }

        return (thrust * weights.thrust()
                + strike * weights.strike()
                + slash * weights.slash()) / totalWeight;
    }

    public DamageTypeProfile multiply(DamageTypeProfile other) {
        return new DamageTypeProfile(
                thrust * other.thrust(),
                strike * other.strike(),
                slash * other.slash()
        );
    }

    public DamageTypeProfile add(DamageTypeProfile other) {
        return new DamageTypeProfile(
                thrust + other.thrust(),
                strike + other.strike(),
                slash + other.slash()
        );
    }
}
