package com.kingdomcomecombat.compat;

import com.kingdomcomecombat.KingdomComeCombat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Reflection-only bridge for Legendary Survival Overhaul's NeoForge limb API. */
public final class LegendarySurvivalOverhaulCompat {
    private static volatile boolean reflectionFailureLogged;

    private LegendarySurvivalOverhaulCompat() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean applyKccLimbDamage(Object player, float damageValue) {
        String kccPart = FirstAidCompat.activePartName();
        if (kccPart == null) {
            return false;
        }

        try {
            Class<?> bodyDamageUtil = Class.forName(
                    "sfiomn.legendarysurvivaloverhaul.api.bodydamage.BodyDamageUtil");
            Class<?> bodyDamageInterface = Class.forName(
                    "sfiomn.legendarysurvivaloverhaul.api.bodydamage.IBodyDamageUtil");
            Class<? extends Enum> bodyPartEnum = (Class<? extends Enum>) Class.forName(
                    "sfiomn.legendarysurvivaloverhaul.api.bodydamage.BodyPartEnum").asSubclass(Enum.class);
            Object lsoPart = Enum.valueOf(bodyPartEnum, lsoPartName(kccPart));

            Field internalField = bodyDamageUtil.getField("internal");
            Object internal = internalField.get(null);
            if (internal == null) {
                return false;
            }
            Method hurtBodyPart = findHurtBodyPart(bodyDamageInterface, bodyPartEnum);
            hurtBodyPart.invoke(
                    internal,
                    player,
                    lsoPart,
                    damageValue * FirstAidCompat.activeDamageScale()
            );
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            logReflectionFailure(exception);
            return false;
        }
    }

    private static Method findHurtBodyPart(Class<?> api, Class<?> bodyPartEnum) throws NoSuchMethodException {
        for (Method method : api.getMethods()) {
            if (method.getName().equals("hurtBodyPart")
                    && method.getParameterCount() == 3
                    && method.getParameterTypes()[1] == bodyPartEnum) {
                return method;
            }
        }
        throw new NoSuchMethodException("LSO IBodyDamageUtil.hurtBodyPart");
    }

    private static String lsoPartName(String kccPart) {
        return "BODY".equals(kccPart) ? "CHEST" : kccPart;
    }

    private static void logReflectionFailure(Throwable exception) {
        if (!reflectionFailureLogged) {
            reflectionFailureLogged = true;
            KingdomComeCombat.LOGGER.warn(
                    "Legendary Survival Overhaul is present, but its limb damage API is incompatible.",
                    exception
            );
        }
    }
}
