package com.kingdomcomecombat.client.render;

import net.minecraft.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

public final class EquipmentOverlayRenderContext {
    private static final ThreadLocal<Deque<ItemStack>> STACKS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private EquipmentOverlayRenderContext() {
    }

    public static void push(ItemStack stack) {
        STACKS.get().push(stack);
    }

    public static void pop() {
        Deque<ItemStack> stacks = STACKS.get();
        if (!stacks.isEmpty()) {
            stacks.pop();
        }
    }

    public static ItemStack currentStack() {
        Deque<ItemStack> stacks = STACKS.get();
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.peek();
    }
}
