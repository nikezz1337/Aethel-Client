package dev.ethereal.api.utils.render.taksa;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class TaksaModel {

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart chest;
    private final ModelPart back;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart tail;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart root;

    public TaksaModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
        this.neck = root.getChild("neck");
        this.body = root.getChild("body");
        this.chest = body.getChild("chest");
        this.back = body.getChild("back");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.leftBackLeg = root.getChild("left_back_leg");
        this.rightBackLeg = root.getChild("right_back_leg");
        this.tail = root.getChild("tail");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        ModelPartData headData = modelPartData.addChild("head",
            ModelPartBuilder.create()
                .uv(0, 0).cuboid(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 4.0F)
                .uv(21, 0).cuboid(-1.5F, 0.0F, -7.0F, 3.0F, 3.0F, 3.0F),
            ModelTransform.pivot(0.0F, 10.5F, -6.8F));

        headData.addChild("left_ear",
            ModelPartBuilder.create()
                .uv(32, 4).cuboid(0.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F)
                .uv(34, 1).cuboid(0.0F, -5.5F, -0.75F, 1.0F, 1.0F, 2.0F),
            ModelTransform.pivot(3.0F, 3.0F, -2.0F));

        headData.addChild("right_ear",
            ModelPartBuilder.create()
                .uv(32, 4).mirrored().cuboid(-1.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F)
                .uv(34, 1).mirrored().cuboid(-1.0F, -5.5F, -0.75F, 1.0F, 1.0F, 2.0F),
            ModelTransform.pivot(-3.0F, 3.0F, -2.0F));

        modelPartData.addChild("neck",
            ModelPartBuilder.create()
                .uv(15, 7).cuboid(-2.95F, -1.0F, -4.0F, 5.9F, 5.0F, 6.0F),
            ModelTransform.of(0.0F, 10.5F, -5.0F, -0.43633232F, 0, 0));

        ModelPartData bodyData = modelPartData.addChild("body",
            ModelPartBuilder.create(),
            ModelTransform.pivot(0.0F, 13.5F, -5.0F));

        bodyData.addChild("chest",
            ModelPartBuilder.create()
                .uv(32, 13).cuboid(-4.0F, -3.5F, -3.0F, 8.0F, 7.0F, 6.0F),
            ModelTransform.pivot(0.0F, 0.0F, 3.0F));

        bodyData.addChild("back",
            ModelPartBuilder.create()
                .uv(3, 19).cuboid(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 11.0F),
            ModelTransform.pivot(0.0F, -0.5F, 5.5F));

        modelPartData.addChild("front_left_leg",
            ModelPartBuilder.create()
                .uv(42, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
            ModelTransform.pivot(1.5F, 16.0F, -3.0F));

        modelPartData.addChild("front_right_leg",
            ModelPartBuilder.create()
                .uv(42, 0).mirrored().cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
            ModelTransform.pivot(-1.5F, 16.0F, -3.0F));

        modelPartData.addChild("left_back_leg",
            ModelPartBuilder.create()
                .uv(52, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
            ModelTransform.pivot(1.5F, 16.0F, 9.0F));

        modelPartData.addChild("right_back_leg",
            ModelPartBuilder.create()
                .uv(52, 0).mirrored().cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
            ModelTransform.pivot(-1.5F, 16.0F, 9.0F));

        modelPartData.addChild("tail",
            ModelPartBuilder.create()
                .uv(2, 12).cuboid(-1.0F, 2.0F, -1.0F, 2.0F, 8.0F, 2.0F),
            ModelTransform.of(0.0F, 9.0F, 10.0F, (float) Math.PI / 8F, 0, 0));

        return TexturedModelData.of(modelData, 60, 36);
    }

    public void setAngles(float ageInTicks, TaksaBrain brain) {
        head.yaw = brain.getYaw() * ((float) Math.PI / 180F);
        head.pitch = brain.getPitch() * ((float) Math.PI / 180F);
        
        frontLeftLeg.pitch = (float) Math.cos(brain.limbSwing * 0.6662F) * 1.4F * brain.limbSwingAmount;
        frontRightLeg.pitch = (float) Math.cos(brain.limbSwing * 0.6662F + (float) Math.PI) * 1.4F * brain.limbSwingAmount;
        leftBackLeg.pitch = (float) Math.cos(brain.limbSwing * 0.6662F + (float) Math.PI) * 1.4F * brain.limbSwingAmount;
        rightBackLeg.pitch = (float) Math.cos(brain.limbSwing * 0.6662F) * 1.4F * brain.limbSwingAmount;
        
        if (brain.isLay()) {
            frontLeftLeg.pitch = (float) Math.toRadians(-90.0F);
            frontRightLeg.pitch = (float) Math.toRadians(-90.0F);
            leftBackLeg.pitch = (float) Math.toRadians(90.0F);
            rightBackLeg.pitch = (float) Math.toRadians(90.0F);
            frontLeftLeg.yaw = (float) Math.toRadians(-22.0F);
            frontRightLeg.yaw = (float) Math.toRadians(22.0F);
            leftBackLeg.yaw = (float) Math.toRadians(22.0F);
            rightBackLeg.yaw = (float) Math.toRadians(-22.0F);
        } else {
            frontLeftLeg.yaw = 0;
            frontRightLeg.yaw = 0;
            leftBackLeg.yaw = 0;
            rightBackLeg.yaw = 0;
        }

        tail.pitch = (float) Math.toRadians(brain.isLay() ? 45.0F : 22.0F);
        tail.roll = (float) (Math.toRadians(-22.5F) + (float) Math.PI / 8F + (float) Math.cos(ageInTicks * 0.15F) * 0.3F);
    }

    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, TaksaBrain brain) {
        matrices.push();
        matrices.translate(0.0F, 1.2F - (brain.isLay() ? 0.3F : 0.0F), 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(brain.getBody()));
        root.render(matrices, vertices, light, overlay);
        matrices.pop();
    }
}
