package com.kingdomcomecombat.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.lang.reflect.Proxy;

/** Vertex output that executes model rendering while discarding every vertex. */
public final class CollisionOnlyVertexConsumers {
    private static final VertexConsumer DISCARD = createDiscard();

    // ItemRenderer combines the base and glint consumers and explicitly
    // rejects identical delegates. Return a separate sink for every buffer
    // request so collision-only rendering remains valid for enchanted items.
    private static final VertexConsumerProvider PROVIDER = layer -> createDiscard();

    private CollisionOnlyVertexConsumers() {
    }

    public static VertexConsumerProvider provider() {
        return PROVIDER;
    }

    public static VertexConsumer consumer() {
        return DISCARD;
    }

    private static VertexConsumer createDiscard() {
        return (VertexConsumer) Proxy.newProxyInstance(
                VertexConsumer.class.getClassLoader(),
                new Class<?>[]{VertexConsumer.class},
                (proxy, method, args) -> {
                    Class<?> type = method.getReturnType();
                    if (type.isInstance(proxy)) return proxy;
                    if (type == boolean.class) return false;
                    if (type == byte.class) return (byte) 0;
                    if (type == short.class) return (short) 0;
                    if (type == int.class) return 0;
                    if (type == long.class) return 0L;
                    if (type == float.class) return 0.0F;
                    if (type == double.class) return 0.0D;
                    if (type == char.class) return (char) 0;
                    return null;
                }
        );
    }
}
