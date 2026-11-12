package gcewing.sg.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Textured rectangles with fractional coordinates, like Greg's BaseGui.drawTexturedRect.
 * Texture coordinates are given in units where the whole texture is (texW x texH).
 */
public final class GuiDraw {

    private GuiDraw() {
    }

    public static void rect(GuiGraphics g, ResourceLocation tex, double x, double y, double w, double h,
                            double u, double v, double us, double vs, double texW, double texH) {
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        Matrix4f m = g.pose().last().pose();
        float u0 = (float) (u / texW), u1 = (float) ((u + us) / texW);
        float v0 = (float) (v / texH), v1 = (float) ((v + vs) / texH);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.vertex(m, (float) x, (float) (y + h), 0).uv(u0, v1).endVertex();
        b.vertex(m, (float) (x + w), (float) (y + h), 0).uv(u1, v1).endVertex();
        b.vertex(m, (float) (x + w), (float) y, 0).uv(u1, v0).endVertex();
        b.vertex(m, (float) x, (float) y, 0).uv(u0, v0).endVertex();
        BufferUploader.drawWithShader(b.end());
    }

    /** Whole texture stretched over the rectangle. */
    public static void rect(GuiGraphics g, ResourceLocation tex, double x, double y, double w, double h) {
        rect(g, tex, x, y, w, h, 0, 0, 1, 1, 1, 1);
    }

    public static void setColor(double r, double g, double b) {
        RenderSystem.setShaderColor((float) r, (float) g, (float) b, 1f);
    }

    public static void resetColor() {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static void centeredString(GuiGraphics g, net.minecraft.client.gui.Font font, String s, int x, int y, int color) {
        g.drawString(font, s, x - font.width(s) / 2, y, color, false);
    }

    public static void rightAlignedString(GuiGraphics g, net.minecraft.client.gui.Font font, String s, int x, int y, int color) {
        g.drawString(font, s, x - font.width(s), y, color, false);
    }
}
