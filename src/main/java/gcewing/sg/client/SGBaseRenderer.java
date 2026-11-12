package gcewing.sg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import gcewing.sg.SGConfig;
import gcewing.sg.SGCraft;
import gcewing.sg.block.entity.SGBaseBlockEntity;
import gcewing.sg.util.GateTransform;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Renders a merged stargate: outer ring, rotating symbol ring, chevrons, iris and the rippling
 * event horizon. Geometry and texture layout are taken from the original SGBaseTERenderer, which
 * used immediate-mode OpenGL; here the same vertices go through PoseStack/VertexConsumer.
 */
public class SGBaseRenderer implements BlockEntityRenderer<SGBaseBlockEntity> {

    static final ResourceLocation STARGATE_TEX = SGCraft.loc("textures/entity/stargate.png");
    static final ResourceLocation EVENT_HORIZON_TEX = SGCraft.loc("textures/entity/eventhorizon.png");
    static final ResourceLocation IRIS_TEX = SGCraft.loc("textures/entity/iris.png");

    static final int numRingSegments = 32;
    static final double ringInnerRadius = 2.0;
    static final double ringMidRadius = 2.25;
    static final double ringOuterRadius = 2.5;
    static final double ringDepth = 0.5;
    static final double ringOverlap = 1 / 64.0;
    static final double ringZOffset = 0.0001;
    static final double chevronInnerRadius = 2.25;
    static final double chevronOuterRadius = ringOuterRadius + 1 / 16.0;
    static final double chevronWidth = (chevronOuterRadius - chevronInnerRadius) * 1.5;
    static final double chevronDepth = 0.125;
    static final double chevronBorderWidth = chevronWidth / 6;
    static final double chevronMotionDistance = 1 / 8.0;

    static final int textureTilesWide = 32;
    static final int textureTilesHigh = 2;
    static final double textureScaleU = 1.0 / (textureTilesWide * 16);
    static final double textureScaleV = 1.0 / (textureTilesHigh * 16);

    static final int ringFaceTextureIndex = 0x01;
    static final int ringTextureIndex = 0x00;
    static final int ringSymbolTextureIndex = 0x20;
    static final int chevronTextureIndex = 0x03;
    static final int chevronLitTextureIndex = 0x02;

    static final double ringSymbolTextureLength = 512.0;
    static final double ringSymbolTextureHeight = 16.0;
    static final double ringSymbolSegmentWidth = ringSymbolTextureLength / numRingSegments;

    public static final int ehGridRadialSize = 5;
    public static final int ehGridPolarSize = numRingSegments;
    static final double ehBandWidth = ringInnerRadius / ehGridRadialSize;

    static final int numIrisBlades = 12;

    static final int[][] chevronEngagementSequences = {
        {9, 3, 4, 5, 6, 0, 1, 2, 9}, // 7 symbols (9 = never engaged)
        {7, 3, 4, 5, 8, 0, 1, 2, 6}  // 9 symbols
    };

    static final double[] s = new double[numRingSegments + 1];
    static final double[] c = new double[numRingSegments + 1];

    static {
        for (int i = 0; i <= numRingSegments; i++) {
            double a = 2 * Math.PI * i / numRingSegments;
            s[i] = Math.sin(a);
            c[i] = Math.cos(a);
        }
    }

    public SGBaseRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public boolean shouldRenderOffScreen(SGBaseBlockEntity te) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 181; // sqrt(32768), the original max render distance
    }

    @Override
    public void render(SGBaseBlockEntity te, float t, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!te.isMerged)
            return;
        // The base block is solid, so take the light level from the middle of the ring instead.
        int light = te.getLevel() != null ? LevelRenderer.getLightColor(te.getLevel(), te.getBlockPos().above(2)) : packedLight;
        pose.pushPose();
        pose.translate(0.5, 2.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(GateTransform.turnDegrees(te.facing())));
        Quads q = new Quads(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(STARGATE_TEX)), light);
        renderRing(q, ringMidRadius - ringOverlap, ringOuterRadius, false, ringZOffset);
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees((float) (te.interpolatedRingAngle(t) + SGBaseBlockEntity.ringSymbolAngle / 2)));
        q.refresh();
        renderRing(q, ringInnerRadius, ringMidRadius, true, 0);
        pose.popPose();
        q.refresh();
        renderChevrons(te, pose, q);
        if (te.hasIrisUpgrade)
            renderIris(te, t, pose, buffers, light);
        if (te.isConnected())
            renderEventHorizon(te, t, pose, buffers, light);
        pose.popPose();
    }

    // ------------------------------------------ Ring ------------------------------------------

    void renderRing(Quads q, double r1, double r2, boolean inner, double dz) {
        double z = ringDepth / 2 + dz;
        for (int i = 0; i < numRingSegments; i++) {
            q.tile(ringTextureIndex);
            if (!inner) {
                q.quad(r2 * c[i], r2 * s[i], z, 0, 0,
                    r2 * c[i], r2 * s[i], -z, 0, 16,
                    r2 * c[i + 1], r2 * s[i + 1], -z, 16, 16,
                    r2 * c[i + 1], r2 * s[i + 1], z, 16, 0);
            } else {
                q.quad(r1 * c[i], r1 * s[i], -z, 0, 0,
                    r1 * c[i], r1 * s[i], z, 0, 16,
                    r1 * c[i + 1], r1 * s[i + 1], z, 16, 16,
                    r1 * c[i + 1], r1 * s[i + 1], -z, 16, 0);
            }
            // back face
            q.quad(r1 * c[i], r1 * s[i], -z, 0, 16,
                r1 * c[i + 1], r1 * s[i + 1], -z, 16, 16,
                r2 * c[i + 1], r2 * s[i + 1], -z, 16, 0,
                r2 * c[i], r2 * s[i], -z, 0, 0);
            // front face
            double u, du, dv;
            if (!inner) {
                q.tile(ringFaceTextureIndex);
                u = 0;
                du = 16;
                dv = 16;
            } else {
                q.tile(ringSymbolTextureIndex);
                u = ringSymbolTextureLength - (i + 1) * ringSymbolSegmentWidth;
                du = ringSymbolSegmentWidth;
                dv = ringSymbolTextureHeight;
            }
            q.quad(r1 * c[i], r1 * s[i], z, u + du, dv,
                r2 * c[i], r2 * s[i], z, u + du, 0,
                r2 * c[i + 1], r2 * s[i + 1], z, u, 0,
                r1 * c[i + 1], r1 * s[i + 1], z, u, dv);
        }
    }

    // ---------------------------------------- Chevrons ----------------------------------------

    void renderChevrons(SGBaseBlockEntity te, PoseStack pose, Quads q) {
        int numChevrons = te.getNumChevrons();
        int i0 = numChevrons > 7 ? 0 : 1;
        int k = te.dialledAddress.length() > 7 ? 1 : 0;
        float a = te.angleBetweenChevrons();
        for (int i = i0; i < i0 + numChevrons; i++) {
            int j = chevronEngagementSequences[k][i];
            boolean engaged = te.chevronIsEngaged(j);
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(90 - (i - 4) * a));
            if (engaged)
                pose.translate(-chevronMotionDistance, 0, 0);
            q.refresh();
            chevron(q, engaged);
            pose.popPose();
        }
        q.refresh();
    }

    void chevron(Quads q, boolean engaged) {
        double r1 = chevronInnerRadius;
        double r2 = chevronOuterRadius;
        double z2 = ringDepth / 2;
        double z1 = z2 + chevronDepth;
        double w1 = chevronBorderWidth;
        double w2 = w1 * 1.25;
        double x1 = r1, y1 = chevronWidth / 4;
        double x2 = r2, y2 = chevronWidth / 2;

        q.tile(chevronTextureIndex);
        q.quad(x2, y2, z1, 0, 2, x1, y1, z1, 0, 16, x1 + w1, y1 - w1, z1, 4, 12, x2, y2 - w2, z1, 4, 2);
        q.quad(x2, y2, z1, 0, 0, x2, y2, z2, 0, 4, x1, y1, z2, 16, 4, x1, y1, z1, 16, 0);
        q.quad(x2, y2, z1, 16, 0, x2, y2 - w2, z1, 12, 0, x2, y2 - w2, z2, 12, 4, x2, y2, z2, 16, 4);
        q.quad(x1 + w1, y1 - w1, z1, 4, 12, x1, y1, z1, 0, 16, x1, -y1, z1, 16, 16, x1 + w1, -y1 + w1, z1, 12, 12);
        q.quad(x1, y1, z1, 0, 0, x1, y1, z2, 0, 4, x1, -y1, z2, 16, 4, x1, -y1, z1, 16, 0);
        q.quad(x2, -y2 + w2, z1, 12, 0, x1 + w1, -y1 + w1, z1, 12, 12, x1, -y1, z1, 16, 16, x2, -y2, z1, 16, 0);
        q.quad(x1, -y1, z1, 0, 0, x1, -y1, z2, 0, 4, x2, -y2, z2, 16, 4, x2, -y2, z1, 16, 0);
        q.quad(x2, -y2, z1, 0, 0, x2, -y2, z2, 0, 4, x2, -y2 + w2, z2, 4, 4, x2, -y2 + w2, z1, 4, 0);
        q.quad(x2, -y2, z2, 0, 0, x1, -y1, z2, 0, 16, x1, y1, z2, 16, 16, x2, y2, z2, 16, 0);

        // Light-up part: dimmed when idle, full-bright when engaged.
        q.tile(chevronLitTextureIndex);
        int savedLight = q.light;
        if (engaged)
            q.light = LightTexture.FULL_BRIGHT;
        else
            q.color(0.5f, 0.5f, 0.5f);
        q.quad(x2, y2 - w2, z1, 0, 4, x1 + w1, y1 - w1, z1, 4, 16, x1 + w1, 0, z1, 8, 16, x2, 0, z1, 8, 4);
        q.quad(x2, 0, z1, 8, 4, x1 + w1, 0, z1, 8, 16, x1 + w1, -y1 + w1, z1, 12, 16, x2, -y2 + w2, z1, 16, 4);
        q.quad(x2, y2 - w2, z2, 0, 0, x2, y2 - w2, z1, 0, 4, x2, -y2 + w2, z1, 16, 4, x2, -y2 + w2, z2, 16, 0);
        q.color(1, 1, 1);
        q.light = savedLight;
    }

    // ------------------------------------------ Iris ------------------------------------------

    void renderIris(SGBaseBlockEntity te, float t, PoseStack pose, MultiBufferSource buffers, int light) {
        Quads q = new Quads(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(IRIS_TEX)), light);
        q.uvScale(1, 1);
        double a = 0.8 * te.getIrisAperture(t);
        for (int i = 0; i < numIrisBlades; i++) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(360f * i / numIrisBlades));
            renderIrisBlade(pose, q, a);
            pose.popPose();
        }
    }

    void renderIrisBlade(PoseStack pose, Quads q, double a) {
        double aa = a * 60;
        double r = 2.31;
        double w1 = 2.40;
        double w2 = 1.85;
        double h0 = 0.16;
        double h1 = 1.00;
        double h = h1 - (h1 - h0) * a * a;
        double u = w2 / w1;
        double v = h / h1;
        double v0 = h0 / h1;
        double z0 = SGBaseBlockEntity.irisZPosition;
        double z1 = 0.01;
        pose.pushPose();
        pose.translate(r, 0, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) -aa));
        q.refresh();
        // Two triangle fans of five vertices each, split into quads.
        q.quad(-w1, 0, z0, 0, 0, 0, 0, z0 + z1, 1, 0, 0, h0, z0 + z1, 1, v0, -w1 + w2, h, z0, u, v);
        q.quad(-w1, 0, z0, 0, 0, -w1 + w2, h, z0, u, v, -w1, h, z0, 0, v, -w1, h, z0, 0, v);
        q.quad(-w1, 0, z0, 0, 0, -w1, h, z0, 0, v, -w1 + w2, h, z0, u, v, 0, h0, z0 - z1, 1, v0);
        q.quad(-w1, 0, z0, 0, 0, 0, h0, z0 - z1, 1, v0, 0, 0, z0 - z1, 1, 0, 0, 0, z0 - z1, 1, 0);
        pose.popPose();
        q.refresh();
    }

    // ------------------------------------- Event horizon --------------------------------------

    void renderEventHorizon(SGBaseBlockEntity te, float t, PoseStack pose, MultiBufferSource buffers, int light) {
        RenderType type = SGConfig.transparency.get()
            ? RenderType.entityTranslucent(EVENT_HORIZON_TEX)
            : RenderType.entityCutoutNoCull(EVENT_HORIZON_TEX);
        Quads q = new Quads(pose, buffers.getBuffer(type), light);
        q.uvScale(1, 1);
        q.fixedNormal = new Vector3f(0, 0, 1);
        double[][] grid = te.getEventHorizonGrid()[0];
        double rclip = 2.5 * (te.irisIsClosed() ? te.getIrisAperture(t) : 1.0);
        for (int i = 1; i < ehGridRadialSize; i++)
            for (int j = 0; j < ehGridPolarSize; j++)
                q.quad(ehVertex(grid, i, j, rclip), ehVertex(grid, i + 1, j, rclip),
                    ehVertex(grid, i + 1, j + 1, rclip), ehVertex(grid, i, j + 1, rclip));
        double[] centre = {0, 0, ehClip(grid[1][0], 0, rclip), 0, 0};
        for (int j = 0; j < ehGridPolarSize; j++)
            q.quad(centre, ehVertex(grid, 1, j, rclip), ehVertex(grid, 1, j + 1, rclip), ehVertex(grid, 1, j + 1, rclip));
    }

    double[] ehVertex(double[][] grid, int i, int j, double rclip) {
        double r = i * ehBandWidth;
        double x = r * c[j];
        double y = r * s[j];
        double z = ehClip(grid[j + 1][i], r, rclip);
        return new double[] {x, y, z, x, y};
    }

    double ehClip(double z, double r, double rclip) {
        if (r >= rclip)
            z = Math.min(z, 0);
        return z;
    }

    // --------------------------------------- Vertex output ------------------------------------

    /** Emits textured quads with per-face normals in the current pose. */
    static class Quads {
        final PoseStack pose;
        final VertexConsumer out;
        int light;
        Matrix4f m;
        Matrix3f n;
        double u0, v0, su = textureScaleU, sv = textureScaleV;
        float r = 1, g = 1, b = 1;
        Vector3f fixedNormal;

        Quads(PoseStack pose, VertexConsumer out, int light) {
            this.pose = pose;
            this.out = out;
            this.light = light;
            refresh();
        }

        void refresh() {
            m = pose.last().pose();
            n = pose.last().normal();
        }

        void uvScale(double su, double sv) {
            this.su = su;
            this.sv = sv;
            u0 = v0 = 0;
        }

        void tile(int index) {
            u0 = (index % textureTilesWide) * (textureScaleU * 16);
            v0 = (index / textureTilesWide) * (textureScaleV * 16);
        }

        void color(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        void quad(double x0, double y0, double z0, double ua, double va,
                  double x1, double y1, double z1, double ub, double vb,
                  double x2, double y2, double z2, double uc, double vc,
                  double x3, double y3, double z3, double ud, double vd) {
            quad(new double[] {x0, y0, z0, ua, va}, new double[] {x1, y1, z1, ub, vb},
                new double[] {x2, y2, z2, uc, vc}, new double[] {x3, y3, z3, ud, vd});
        }

        void quad(double[] a, double[] b, double[] c, double[] d) {
            Vector3f normal = fixedNormal != null ? fixedNormal : faceNormal(a, b, c, d);
            vertex(a, normal);
            vertex(b, normal);
            vertex(c, normal);
            vertex(d, normal);
        }

        static Vector3f faceNormal(double[] a, double[] b, double[] c, double[] d) {
            Vec3 p0 = new Vec3(a[0], a[1], a[2]);
            Vec3 e1 = new Vec3(c[0], c[1], c[2]).subtract(p0);
            Vec3 e2 = new Vec3(d[0], d[1], d[2]).subtract(new Vec3(b[0], b[1], b[2]));
            Vec3 nrm = e2.cross(e1);
            if (nrm.lengthSqr() < 1e-12)
                return new Vector3f(0, 0, 1);
            nrm = nrm.normalize();
            return new Vector3f((float) nrm.x, (float) nrm.y, (float) nrm.z);
        }

        void vertex(double[] p, Vector3f normal) {
            out.vertex(m, (float) p[0], (float) p[1], (float) p[2])
                .color(r, g, b, 1f)
                .uv((float) (u0 + p[3] * su), (float) (v0 + p[4] * sv))
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n, normal.x(), normal.y(), normal.z())
                .endVertex();
        }
    }
}
