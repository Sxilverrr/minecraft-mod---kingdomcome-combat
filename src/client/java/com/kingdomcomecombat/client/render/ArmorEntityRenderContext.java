package com.kingdomcomecombat.client.render;

public final class ArmorEntityRenderContext {
    private static final ThreadLocal<Integer> ENTITY_ID = new ThreadLocal<>();

    private ArmorEntityRenderContext() {
    }

    public static void begin(int entityId) {
        ENTITY_ID.set(entityId);
    }

    public static void end() {
        ENTITY_ID.remove();
    }

    public static boolean isRendering(int entityId) {
        Integer current = ENTITY_ID.get();
        return current != null && current == entityId;
    }
}
