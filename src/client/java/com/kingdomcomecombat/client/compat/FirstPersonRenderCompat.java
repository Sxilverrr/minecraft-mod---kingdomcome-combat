package com.kingdomcomecombat.client.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class FirstPersonRenderCompat {
    private static final String FIRST_PERSON_MOD_ID = "firstperson";
    private static final String FIRST_PERSON_API_CLASS = "dev.tr7zw.firstperson.api.FirstPersonAPI";
    private static final String IRIS_API_CLASS = "net.irisshaders.iris.api.v0.IrisApi";
    private static final ThreadLocal<Integer> EXTERNAL_BODY_RENDER_DEPTH =
            ThreadLocal.withInitial(() -> 0);
    private static final Method IS_RENDERING_PLAYER = findApiMethod("isRenderingPlayer");
    private static final Method IS_ENABLED = findApiMethod("isEnabled");

    private FirstPersonRenderCompat() {
    }

    public static boolean beginLivingEntityRender() {
        if (!invokeBoolean(IS_RENDERING_PLAYER)) {
            return false;
        }
        EXTERNAL_BODY_RENDER_DEPTH.set(EXTERNAL_BODY_RENDER_DEPTH.get() + 1);
        return true;
    }

    public static void endLivingEntityRender(boolean entered) {
        if (!entered) {
            return;
        }
        int depth = EXTERNAL_BODY_RENDER_DEPTH.get() - 1;
        if (depth <= 0) {
            EXTERNAL_BODY_RENDER_DEPTH.remove();
        } else {
            EXTERNAL_BODY_RENDER_DEPTH.set(depth);
        }
    }

    public static boolean isExternalBodyRender() {
        return EXTERNAL_BODY_RENDER_DEPTH.get() > 0;
    }

    public static boolean isExternalBodyRenderOrPreparing() {
        return isExternalBodyRender() || invokeBoolean(IS_RENDERING_PLAYER);
    }

    public static boolean shouldSuppressPalFirstPersonRenderer() {
        return invokeBoolean(IS_ENABLED);
    }

    public static boolean isFirstPersonModelLoaded() {
        // Merely being present is not enough to hand first-person rendering over
        // to the external mod. It may be disabled in its own config, or a newer
        // Minecraft build may load the mod while exposing an incompatible API.
        // In both cases KCC must keep its own body renderer as a safe fallback.
        return FabricLoader.getInstance().isModLoaded(FIRST_PERSON_MOD_ID)
                && IS_ENABLED != null
                && invokeBoolean(IS_ENABLED);
    }

    public static boolean isRenderingShadowPass() {
        Object irisApi = IrisCompat.IRIS_API;
        Method shadowPassMethod = IrisCompat.IS_RENDERING_SHADOW_PASS;
        if (irisApi == null || shadowPassMethod == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(shadowPassMethod.invoke(irisApi));
        } catch (IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            return false;
        }
    }

    private static Object findIrisApi() {
        try {
            return Class.forName(IRIS_API_CLASS).getMethod("getInstance").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | LinkageError ignored) {
            return null;
        }
    }

    private static Method findIrisMethod(Object irisApi) {
        if (irisApi == null) {
            return null;
        }
        try {
            return irisApi.getClass().getMethod("isRenderingShadowPass");
        } catch (NoSuchMethodException | LinkageError ignored) {
            return null;
        }
    }

    private static Method findApiMethod(String name) {
        if (!FabricLoader.getInstance().isModLoaded(FIRST_PERSON_MOD_ID)) {
            return null;
        }
        try {
            return Class.forName(FIRST_PERSON_API_CLASS).getMethod(name);
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError ignored) {
            return null;
        }
    }

    private static boolean invokeBoolean(Method method) {
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            return false;
        }
    }

    /** Defers Iris class initialization until an actual world render pass. */
    private static final class IrisCompat {
        private static final Object IRIS_API = findIrisApi();
        private static final Method IS_RENDERING_SHADOW_PASS = findIrisMethod(IRIS_API);
    }
}
