package com.kingdomcomecombat.client.ui;

import com.kingdomcomecombat.network.LearnSkillBookPayload;
import com.kingdomcomecombat.network.OpenSkillBookPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class SkillBookClientState {
    private static String comboId = "";
    private static String passiveId = "";
    private static boolean learned;

    private SkillBookClientState() {
    }

    public static void set(OpenSkillBookPayload payload) {
        comboId = payload.comboId();
        passiveId = payload.passiveId();
        learned = payload.learned();
    }

    public static void clear() {
        comboId = "";
        passiveId = "";
        learned = false;
    }

    public static void learnIfSecondPage(int pageIndex) {
        if (pageIndex < 1 || learned || (comboId.isBlank() && passiveId.isBlank())) {
            return;
        }

        learned = true;
        ClientPlayNetworking.send(new LearnSkillBookPayload(comboId, passiveId));
    }
}
