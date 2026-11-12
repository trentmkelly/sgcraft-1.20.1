package gcewing.sg.client;

import gcewing.sg.SGCraft;
import gcewing.sg.block.entity.PowerUnitBlockEntity;
import gcewing.sg.menu.PowerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PowerScreen extends AbstractContainerScreen<PowerMenu> {

    static final ResourceLocation BG = SGCraft.loc("textures/gui/power_gui.png");
    static final int guiWidth = 128;
    static final int guiHeight = 64;

    public PowerScreen(PowerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = guiWidth;
        imageHeight = guiHeight;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight, 128, 64);
        PowerUnitBlockEntity te = menu.te;
        if (te != null) {
            int w = (int) (25 * te.energyBuffer / PowerUnitBlockEntity.maxEnergyBuffer);
            if (w > 0)
                g.fill(leftPos + 19, topPos + 27, leftPos + 19 + w, topPos + 37, 0xFFFF0000);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        PowerUnitBlockEntity te = menu.te;
        int color = 0x404040;
        GuiDraw.centeredString(g, font, Component.translatable("gui.sgcraft.power_unit").getString(), imageWidth / 2, 8, color);
        GuiDraw.rightAlignedString(g, font, "FE", 72, 28, color);
        GuiDraw.rightAlignedString(g, font, String.format("%.0f", te != null ? te.energyBuffer : 0), 121, 28, color);
        GuiDraw.rightAlignedString(g, font, Component.translatable("gui.sgcraft.max").getString(), 72, 42, color);
        GuiDraw.rightAlignedString(g, font, String.format("%.0f", PowerUnitBlockEntity.maxEnergyBuffer), 121, 42, color);
    }
}
