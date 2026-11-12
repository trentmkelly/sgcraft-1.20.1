package gcewing.sg.client;

import gcewing.sg.SGAddressing;
import gcewing.sg.SGCraft;
import gcewing.sg.block.entity.SGBaseBlockEntity;
import gcewing.sg.menu.SGBaseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SGBaseScreen extends AbstractContainerScreen<SGBaseMenu> {

    static final ResourceLocation BG = SGCraft.loc("textures/gui/sg_gui.png");
    static final int textColor = 0x004c66;

    String address;
    String formattedAddress;
    boolean addressValid;

    public SGBaseScreen(SGBaseMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = SGBaseMenu.guiWidth;
        imageHeight = SGBaseMenu.guiHeight;
    }

    @Override
    protected void init() {
        super.init();
        getAddress();
        if (addressValid && minecraft != null)
            minecraft.keyboardHandler.setClipboard(formattedAddress);
    }

    void getAddress() {
        SGBaseBlockEntity te = menu.te;
        if (te != null && te.homeAddress != null) {
            address = te.homeAddress;
            formattedAddress = SGAddressing.formatAddress(address, "-", "-");
            addressValid = true;
        } else {
            address = "";
            formattedAddress = te != null && te.addressError != null ? te.addressError : "";
            addressValid = false;
        }
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
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        int cx = imageWidth / 2;
        if (addressValid)
            SGSymbols.drawAddressSymbols(g, cx, 22, address);
        GuiDraw.centeredString(g, font, Component.translatable("gui.sgcraft.stargate_address").getString(), cx, 8, textColor);
        GuiDraw.centeredString(g, font, formattedAddress, cx, 72, textColor);
        GuiDraw.centeredString(g, font, Component.translatable("gui.sgcraft.base_camouflage").getString(), 92, 92, textColor);
    }
}
