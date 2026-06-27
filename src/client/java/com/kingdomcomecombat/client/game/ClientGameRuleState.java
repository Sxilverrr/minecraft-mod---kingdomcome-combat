package com.kingdomcomecombat.client.game;

public final class ClientGameRuleState {
    private static boolean hardcoreMode;

    private ClientGameRuleState() {
    }

    public static boolean hardcoreMode() {
        return hardcoreMode;
    }

    public static void setHardcoreMode(boolean enabled) {
        hardcoreMode = enabled;
    }
}
