package com.kingdomcomecombat.client.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CombatKeyBindings {
    public static final String CATEGORY = "category.kingdom_come_combat.combat";

    public static KeyBinding LOCK_ON_KEY;
    public static KeyBinding SOFT_LOCK_KEY;
    public static KeyBinding SKILL_SCREEN_KEY;
    public static KeyBinding DODGE_KEY;

    public static void register() {
        LOCK_ON_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kingdom_come_combat.lock_on",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                CATEGORY
        ));
        SOFT_LOCK_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kingdom_come_combat.soft_lock",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_TAB,
                CATEGORY
        ));
        SKILL_SCREEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kingdom_come_combat.skill_screen",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
        ));
        DODGE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kingdom_come_combat.dodge",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                CATEGORY
        ));
    }
}
