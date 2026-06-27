package com.kingdomcomecombat.client.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Bridges PAL versions before and after the first-person-pass accessors were added. */
public final class PalAnimationStateCompat {
    private static Method firstPersonPassGetter;
    private static Method firstPersonPassSetter;
    private static Class<?> resolvedClass;

    private PalAnimationStateCompat() {
    }

    public static boolean isFirstPersonPass(Object state) {
        resolve(state);
        if (firstPersonPassGetter == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(firstPersonPassGetter.invoke(state));
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }

    public static void setFirstPersonPass(Object state, boolean value) {
        resolve(state);
        if (firstPersonPassSetter == null) {
            return;
        }
        try {
            firstPersonPassSetter.invoke(state, value);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            // Older PAL versions do not expose this state; rendering remains PAL-owned.
        }
    }

    public static boolean hasFirstPersonPassAccessors(Object state) {
        resolve(state);
        return firstPersonPassGetter != null && firstPersonPassSetter != null;
    }

    private static void resolve(Object state) {
        if (state == null || resolvedClass == state.getClass()) {
            return;
        }
        resolvedClass = state.getClass();
        firstPersonPassGetter = method(resolvedClass, "playerAnimLib$isFirstPersonPass");
        firstPersonPassSetter = method(resolvedClass, "playerAnimLib$setFirstPersonPass", boolean.class);
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
