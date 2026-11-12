package gcewing.sg.client;

import gcewing.sg.SGCraft;
import gcewing.sg.block.entity.DHDBlockEntity;
import gcewing.sg.menu.DHDFuelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DHDFuelScreen extends AbstractContainerScreen<DHDFuelMenu> {

    static final ResourceLocation BG = SGCraft.loc("textures/gui/dhd_fuel_gui.png");
    static final int textColor = 0x004c66;
    static final int fuelGaugeWidth = 16;
    static final int fuelGaugeHeight = 34;
    static final int fuelGaugeX = 214;
    static final int fuelGaugeY = 84;
    static final int fuelGaugeU = 0;
    static final int fuelGaugeV = 208;

    public DHDFuelScreen(DHDFuelMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = DHDFuelMenu.guiWidth;
        imageHeight = DHDFuelMenu.guiHeight;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        DHDBlockEntity te = menu.te;
        if (te != null) {
            int level = (int) (fuelGaugeHeight * te.energyInBuffer / DHDBlockEntity.maxEnergyBuffer());
            level = Math.max(0, Math.min(level, fuelGaugeHeight));
            g.blit(BG, leftPos + fuelGaugeX, topPos + fuelGaugeY + fuelGaugeHeight - level,
                fuelGaugeU, fuelGaugeV + fuelGaugeHeight - level, fuelGaugeWidth, level, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        GuiDraw.centeredString(g, font, Component.translatable("gui.sgcraft.stargate_controller").getString(), imageWidth / 2, 8, textColor);
        g.drawString(font, Component.translatable("gui.sgcraft.fuel"), 150, 96, textColor, false);
    }
}
