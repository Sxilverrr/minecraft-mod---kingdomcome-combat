package com.kingdomcomecombat.client.render;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Identifies deferred render commands submitted by the hidden collision pass. */
public final class CollisionOnlyRenderContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> SUSPEND_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final Set<Object> COMMANDS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private CollisionOnlyRenderContext() {
    }

    public static void begin() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void end() {
        int depth = DEPTH.get() - 1;
        if (depth <= 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth);
        }
    }

    public static boolean isActive() {
        return DEPTH.get() > 0 && SUSPEND_DEPTH.get() == 0;
    }

    /** Temporarily lets a nested visible renderer submit normal commands. */
    public static void suspend() {
        SUSPEND_DEPTH.set(SUSPEND_DEPTH.get() + 1);
    }

    public static void resume() {
        int depth = SUSPEND_DEPTH.get() - 1;
        if (depth <= 0) {
            SUSPEND_DEPTH.remove();
        } else {
            SUSPEND_DEPTH.set(depth);
        }
    }

    public static void markCommand(Object command) {
        if (isActive()) {
            COMMANDS.add(command);
        }
    }

    public static boolean isMarked(Object command) {
        return COMMANDS.contains(command);
    }

    public static void finishCommand(Object command) {
        COMMANDS.remove(command);
    }
}
