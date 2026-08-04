package com.kingdomcomecombat.client.render;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/** Associates deferred 1.21.11 item render states with their source stacks. */
public final class ItemRenderStackContext12111 {
    private static final Map<ItemRenderState, ItemStack> STACKS = new WeakHashMap<>();

    private ItemRenderStackContext12111() {
    }

    public static synchronized void set(ItemRenderState state, ItemStack stack) {
        STACKS.put(state, stack.copy());
    }

    public static synchronized ItemStack get(ItemRenderState state) {
        return STACKS.getOrDefault(state, ItemStack.EMPTY);
    }
}
