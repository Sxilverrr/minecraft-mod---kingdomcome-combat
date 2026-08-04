package com.kingdomcomecombat.client.render;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

/** Legacy entity renderers do not expose a biped render state for illagers. */
public final class LegacyIllagerArmorFeatureRenderer<T extends IllagerEntity>
        extends FeatureRenderer<T, IllagerEntityModel<T>> {
    private static final float ILLAGER_HELMET_Y_OFFSET = -1.15F;

    private final BipedEntityModel<T> innerModel;
    private final BipedEntityModel<T> outerModel;

    public LegacyIllagerArmorFeatureRenderer(
            FeatureRendererContext<T, IllagerEntityModel<T>> context,
            BipedEntityModel<T> innerModel,
            BipedEntityModel<T> outerModel
    ) {
        super(context);
        this.innerModel = innerModel;
        this.outerModel = outerModel;
    }

    @Override
    public void render(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            T entity,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float animationProgress,
            float headYaw,
            float headPitch
    ) {
        // 1.21.7+ copies IllagerEntityModel#getRootPart() onto the armor model's
        // root. Legacy BipedEntityModel has no exposed root, so apply exactly the
        // same final root transform to the render matrix instead.
        matrices.push();
        getContextModel().getPart().rotate(matrices);
        renderSlot(matrices, vertexConsumers, light, entity, EquipmentSlot.HEAD);
        renderSlot(matrices, vertexConsumers, light, entity, EquipmentSlot.CHEST);
        matrices.pop();
    }

    private void renderSlot(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            T entity,
            EquipmentSlot slot
    ) {
        ItemStack stack = entity.getEquippedStack(slot);
        if (!(stack.getItem() instanceof ArmorItem armor) || armor.getSlotType() != slot) {
            return;
        }

        boolean inner = slot == EquipmentSlot.LEGS;
        BipedEntityModel<T> model = inner ? innerModel : outerModel;

        ArmorMaterial material = armor.getMaterial().value();
        int dyedColor = stack.isIn(ItemTags.DYEABLE)
                ? ColorHelper.Argb.fullAlpha(DyedColorComponent.getColor(stack, 0xA06540))
                : 0xFFFFFFFF;
        for (ArmorMaterial.Layer layer : material.layers()) {
            int color = layer.isDyeable() ? dyedColor : 0xFFFFFFFF;
            Identifier texture = layer.getTexture(inner);
            if (BloodiedTextureCache.shouldUseEquipmentTexture(stack)) {
                texture = BloodiedTextureCache.getEquipmentTexture(texture, stack);
            }
            // Use vanilla's combined armor consumer so loader-specific buffer
            // implementations can pair the base layer and enchantment glint.
            // Rendering the glint as a second, independent model pass crashes
            // NeoForge when its buffer source has already advanced layers.
            VertexConsumer vertices = ItemRenderer.getArmorGlintConsumer(
                    vertexConsumers,
                    RenderLayer.getArmorCutoutNoCull(texture),
                    stack.hasGlint()
            );
            renderArmorParts(model, slot, matrices, vertices, light, color);
        }
    }

    private void renderArmorParts(
            BipedEntityModel<T> armorModel,
            EquipmentSlot slot,
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int color
    ) {
        IllagerEntityModel<T> illagerModel = getContextModel();
        ModelPart root = illagerModel.getPart();
        if (slot == EquipmentSlot.HEAD) {
            ModelPart head = illagerModel.getHead();
            renderPartAt(armorModel.head, head, ILLAGER_HELMET_Y_OFFSET, matrices, vertices, light, color);
            // Illager hats are children of the head, while biped armor hats are
            // root-level siblings. Both armor layers must therefore use the
            // head transform directly instead of IllagerEntityModel#getHat().
            renderPartAt(armorModel.hat, head, ILLAGER_HELMET_Y_OFFSET, matrices, vertices, light, color);
        } else if (slot == EquipmentSlot.CHEST) {
            renderPartAt(armorModel.body, root.getChild("body"), 0.0F, matrices, vertices, light, color);
            renderPartAt(armorModel.rightArm, root.getChild("right_arm"), 0.0F, matrices, vertices, light, color);
            renderPartAt(armorModel.leftArm, root.getChild("left_arm"), 0.0F, matrices, vertices, light, color);
        }
    }

    private static void renderPartAt(
            ModelPart armorPart,
            ModelPart illagerPart,
            float yOffsetPixels,
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int color
    ) {
        ModelTransform savedTransform = armorPart.getTransform();
        boolean savedVisible = armorPart.visible;
        matrices.push();
        try {
            if (yOffsetPixels != 0.0F) {
                matrices.translate(0.0F, yOffsetPixels / 16.0F, 0.0F);
            }
            illagerPart.rotate(matrices);
            armorPart.setTransform(ModelTransform.NONE);
            armorPart.visible = true;
            armorPart.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV, color);
        } finally {
            armorPart.setTransform(savedTransform);
            armorPart.visible = savedVisible;
            matrices.pop();
        }
    }
}
