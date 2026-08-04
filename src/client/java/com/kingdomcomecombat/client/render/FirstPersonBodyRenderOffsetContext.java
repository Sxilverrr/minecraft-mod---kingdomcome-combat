package com.kingdomcomecombat.client.render;

import net.minecraft.util.math.Vec3d;

public final class FirstPersonBodyRenderOffsetContext {
    private static final ThreadLocal<Entry> ACTIVE = new ThreadLocal<>();

    private FirstPersonBodyRenderOffsetContext() {
    }

    public static void begin(int entityId, Vec3d cameraRelativeOffset) {
        ACTIVE.set(new Entry(entityId, cameraRelativeOffset));
    }

    public static Vec3d offsetFor(int entityId) {
        Entry entry = ACTIVE.get();
        return entry != null && entry.entityId() == entityId ? entry.offset() : Vec3d.ZERO;
    }

    public static boolean isRenderingLocalFirstPersonBody() {
        Entry entry = ACTIVE.get();
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        return entry != null
                && client.player != null
                && entry.entityId() == client.player.getId()
                && client.options.getPerspective().isFirstPerson()
                && !(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen);
    }

    public static void end() {
        ACTIVE.remove();
    }

    private record Entry(int entityId, Vec3d offset) {
    }
}
