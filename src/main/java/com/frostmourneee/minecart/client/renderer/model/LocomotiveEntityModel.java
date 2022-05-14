package com.frostmourneee.minecart.client.renderer.model;// Made with Blockbench 4.2.2
// Exported for Minecraft version 1.17 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.frostmourneee.minecart.common.entity.LocomotiveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class LocomotiveEntityModel<T extends LocomotiveEntity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("minecart", "locomotiveentitymodel"), "main");
	private final ModelPart body;
	private final ModelPart chimney;
	private final ModelPart hole;

	public LocomotiveEntityModel(ModelPart root) {
		this.body = root.getChild("body");
		this.chimney = root.getChild("chimney");
		this.hole = root.getChild("hole");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -1.0F, -9.0F, 14.0F, 1.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(63, 48).addBox(-8.0F, -10.0F, -10.0F, 16.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(47, 0).addBox(-8.0F, -10.0F, 9.0F, 16.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(21, 31).addBox(-8.0F, -10.0F, -9.0F, 1.0F, 10.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(0, 20).addBox(7.0F, -10.0F, -9.0F, 1.0F, 10.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(79, 12).addBox(-7.0F, -10.0F, 8.0F, 14.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(21, 20).addBox(-7.0F, -10.0F, -9.0F, 14.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 49).addBox(-7.0F, -10.0F, -8.0F, 1.0F, 9.0F, 16.0F, new CubeDeformation(0.0F))
		.texOffs(44, 48).addBox(6.0F, -10.0F, -8.0F, 1.0F, 9.0F, 16.0F, new CubeDeformation(0.0F))
		.texOffs(3, 1).addBox(-6.0F, -2.0F, -8.0F, 12.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.0F, 1.5708F, -3.1416F));

		PartDefinition chimney = partdefinition.addOrReplaceChild("chimney", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -8.5F, 0.0F, 0.0F, 1.5708F, -3.1416F));

		PartDefinition chimneyHead = chimney.addOrReplaceChild("chimneyHead", CubeListBuilder.create(), PartPose.offset(1.0F, -1.0F, 1.0F));

		PartDefinition pipe = chimney.addOrReplaceChild("pipe", CubeListBuilder.create().texOffs(9, 30).addBox(0.0F, -18.0F, -2.0F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 30).addBox(0.0F, -18.0F, 2.0F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 20).addBox(-1.0F, -18.0F, -2.0F, 1.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(3.0F, -18.0F, -2.0F, 1.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(13, 13).addBox(-1.0F, -20.0F, -2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(13, 9).addBox(3.0F, -20.0F, 2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(13, 5).addBox(-1.0F, -20.0F, 2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(3.0F, -20.0F, -2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 13).addBox(-1.0F, -21.0F, -2.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 10).addBox(-1.0F, -21.0F, 2.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(30, 31).addBox(-1.0F, -21.0F, -1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(21, 31).addBox(3.0F, -21.0F, -1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(8, 20).addBox(2.0F, -22.0F, -1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(8, 0).addBox(0.0F, -22.0F, -1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(13, 25).addBox(1.0F, -22.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 20).addBox(1.0F, -22.0F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(95, 79).addBox(0.0F, -15.0F, -1.0F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, -5.0F, 2.0F));

		PartDefinition hole = partdefinition.addOrReplaceChild("hole", CubeListBuilder.create().texOffs(0, 76).addBox(-2.0F, -4.0F, 10.0F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(11, 76).addBox(-2.0F, -3.0F, 11.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(11, 80).addBox(1.0F, -3.0F, 11.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 86).addBox(-2.0F, -3.0F, 13.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.0F, 1.5708F, -3.1416F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		body.render(poseStack, buffer, packedLight, packedOverlay);
		chimney.render(poseStack, buffer, packedLight, packedOverlay);
		hole.render(poseStack, buffer, packedLight, packedOverlay);
	}
}