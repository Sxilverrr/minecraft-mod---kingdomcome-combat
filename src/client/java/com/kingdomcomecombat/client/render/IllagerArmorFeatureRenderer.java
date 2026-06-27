package com.kingdomcomecombat.client.render;

import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.client.mixin.IllagerModelPartsAccess;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.IllagerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class IllagerArmorFeatureRenderer
        extends FeatureRenderer<IllagerEntityRenderState, IllagerEntityModel<IllagerEntityRenderState>> {
    private static final float ILLAGER_HELMET_Y_OFFSET = -1.15F;

    private final BipedEntityModel<BipedEntityRenderState> armorModel;
    private final BipedEntityModel<BipedEntityRenderState> headPhysicsModel;
    private final BipedEntityModel<BipedEntityRenderState> chestPhysicsModel;
    private final EquipmentRenderer equipmentRenderer;

    public IllagerArmorFeatureRenderer(
            FeatureRendererContext<IllagerEntityRenderState, IllagerEntityModel<IllagerEntityRenderState>> context,
            BipedEntityModel<BipedEntityRenderState> armorModel,
            BipedEntityModel<BipedEntityRenderState> headPhysicsModel,
            BipedEntityModel<BipedEntityRenderState> chestPhysicsModel,
            EquipmentRenderer equipmentRenderer
    ) {
        super(context);
        this.armorModel = armorModel;
        this.headPhysicsModel = headPhysicsModel;
        this.chestPhysicsModel = chestPhysicsModel;
        this.equipmentRenderer = equipmentRenderer;
    }

    @Override
    public void render(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            IllagerEntityRenderState state,
            float limbAngle,
            float limbDistance
    ) {
        EntityRenderStateKccAccess access = (EntityRenderStateKccAccess) state;
        renderSlot(
                matrices,
                vertexConsumers,
                light,
                access.kingdomcomecombat$getHeadStack(),
                EquipmentSlot.HEAD,
                modelForSlot(EquipmentSlot.HEAD)
        );
        renderSlot(
                matrices,
                vertexConsumers,
                light,
                access.kingdomcomecombat$getChestStack(),
                EquipmentSlot.CHEST,
                modelForSlot(EquipmentSlot.CHEST)
        );
    }

    private BipedEntityModel<BipedEntityRenderState> modelForSlot(EquipmentSlot slot) {
        if (!CombatClientConfig.armorPhysicsCompat()) {
            return armorModel;
        }

        return slot == EquipmentSlot.HEAD ? headPhysicsModel : chestPhysicsModel;
    }

    private void renderSlot(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            ItemStack stack,
            EquipmentSlot slot,
            BipedEntityModel<BipedEntityRenderState> model
    ) {
        if (stack.isEmpty()) {
            return;
        }

        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null || equippable.slot() != slot || equippable.assetId().isEmpty()) {
            return;
        }

        setModelPose(model, slot);
        equipmentRenderer.render(
                EquipmentModel.LayerType.HUMANOID,
                equippable.assetId().get(),
                model,
                stack,
                matrices,
                vertexConsumers,
                light
        );
    }

    private void setModelPose(BipedEntityModel<BipedEntityRenderState> model, EquipmentSlot slot) {
        IllagerEntityModel<IllagerEntityRenderState> illagerModel = getContextModel();
        model.getRootPart().copyTransform(illagerModel.getRootPart());
        model.setVisible(false);
        model.head.visible = slot == EquipmentSlot.HEAD;
        model.hat.visible = slot == EquipmentSlot.HEAD;
        model.body.visible = slot == EquipmentSlot.CHEST;
        model.rightArm.visible = slot == EquipmentSlot.CHEST;
        model.leftArm.visible = slot == EquipmentSlot.CHEST;

        model.head.copyTransform(illagerModel.getHead());
        model.hat.copyTransform(illagerModel.getHat());
        if (slot == EquipmentSlot.HEAD) {
            model.head.originY += ILLAGER_HELMET_Y_OFFSET;
            model.hat.originY += ILLAGER_HELMET_Y_OFFSET;
        }
        if (illagerModel instanceof IllagerModelPartsAccess parts) {
            ModelPart body = parts.kingdomcomecombat$getBody();
            if (body != null) {
                model.body.copyTransform(body);
            }
            model.rightArm.copyTransform(parts.kingdomcomecombat$getRightArm());
            model.leftArm.copyTransform(parts.kingdomcomecombat$getLeftArm());
        }
    }
}
