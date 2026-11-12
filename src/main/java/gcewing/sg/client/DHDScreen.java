package gcewing.sg.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import gcewing.sg.SGAddressing;
import gcewing.sg.SGCraft;
import gcewing.sg.SGState;
import gcewing.sg.block.entity.DHDBlockEntity;
import gcewing.sg.block.entity.SGBaseBlockEntity;
import gcewing.sg.network.SGNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/** The DHD dialling interface, drawn across the bottom of the screen. */
public class DHDScreen extends Screen {

    static final ResourceLocation DHD_GUI = SGCraft.loc("textures/gui/dhd_gui.png");
    static final ResourceLocation DHD_CENTRE = SGCraft.loc("textures/gui/dhd_centre.png");

    static final int dhdWidth = 320;
    static final int dhdHeight = 120;
    static final double dhdRadius1 = dhdWidth * 0.1;
    static final double dhdRadius2 = dhdWidth * 0.275;
    static final double dhdRadius3 = dhdWidth * 0.45;

    final BlockPos pos;
    int dhdTop, dhdCentreX, dhdCentreY;
    int closingDelay = 0;
    int addressLength = 7;

    public DHDScreen(BlockPos pos) {
        super(Component.translatable("gui.sgcraft.stargate_controller"));
        this.pos = pos;
    }

    DHDBlockEntity getControllerTE() {
        if (minecraft == null || minecraft.level == null)
            return null;
        return minecraft.level.getBlockEntity(pos) instanceof DHDBlockEntity te ? te : null;
    }

    SGBaseBlockEntity getStargateTE() {
        DHDBlockEntity cte = getControllerTE();
        return cte != null ? cte.getLinkedStargateTE() : null;
    }

    String getEnteredAddress() {
        DHDBlockEntity cte = getControllerTE();
        return cte != null ? cte.enteredAddress : "";
    }

    void setEnteredAddress(String address) {
        DHDBlockEntity cte = getControllerTE();
        if (cte != null)
            cte.enteredAddress = address;
        SGNetwork.CHANNEL.sendToServer(new SGNetwork.DHDAddressMessage(pos, SGNetwork.Action.SET_ENTERED_ADDRESS, address));
    }

    @Override
    protected void init() {
        dhdTop = height - dhdHeight;
        dhdCentreX = width / 2;
        dhdCentreY = dhdTop + dhdHeight / 2;
        SGBaseBlockEntity te = getStargateTE();
        if (te != null)
            addressLength = te.getNumChevrons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        DHDBlockEntity cte = getControllerTE();
        if (cte == null || minecraft.player == null || minecraft.player.distanceToSqr(pos.getCenter()) > 64.0) {
            onClose();
            return;
        }
        if (closingDelay > 0 && --closingDelay == 0) {
            setEnteredAddress("");
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int i = findDHDButton((int) mx, (int) my);
            if (i >= 0) {
                dhdButtonPressed(i);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    void closeAfterDelay(int ticks) {
        closingDelay = ticks;
    }

    int findDHDButton(int mx, int my) {
        int x = -(mx - dhdCentreX);
        int y = -(my - dhdCentreY);
        if (y > 0 && Math.hypot(x, y) <= dhdRadius1)
            return 0;
        y = y * dhdWidth / dhdHeight;
        double r = Math.hypot(x, y);
        if (r > dhdRadius3)
            return -1;
        if (r <= dhdRadius1)
            return 0;
        double a = Math.toDegrees(Math.atan2(y, x));
        if (a < 0)
            a += 360;
        int i0, nb;
        if (r > dhdRadius2) {
            i0 = 1;
            nb = 26;
        } else {
            i0 = 27;
            nb = 11;
        }
        return i0 + (int) Math.floor(a * nb / 360);
    }

    void dhdButtonPressed(int i) {
        buttonSound();
        if (i == 0)
            orangeButtonPressed(false);
        else if (i >= 37)
            backspace();
        else
            enterCharacter(SGAddressing.symbolToChar(i - 1));
    }

    void buttonSound() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
            backspace();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            orangeButtonPressed(true);
            return true;
        }
        return super.keyPressed(key, scancode, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        char u = Character.toUpperCase(c);
        if (SGAddressing.isValidSymbolChar(u)) {
            enterCharacter(u);
            return true;
        }
        return super.charTyped(c, mods);
    }

    void orangeButtonPressed(boolean connectOnly) {
        SGBaseBlockEntity te = getStargateTE();
        if (te != null) {
            if (te.state == SGState.Idle)
                sendConnectOrDisconnect(getEnteredAddress());
            else if (!connectOnly)
                sendConnectOrDisconnect("");
        }
    }

    void sendConnectOrDisconnect(String address) {
        SGNetwork.CHANNEL.sendToServer(new SGNetwork.DHDAddressMessage(pos, SGNetwork.Action.CONNECT_OR_DISCONNECT, address));
        closeAfterDelay(10);
    }

    void backspace() {
        if (stargateIsIdle()) {
            buttonSound();
            String a = getEnteredAddress();
            int n = a.length();
            if (n > 0)
                setEnteredAddress(a.substring(0, n - 1));
        }
    }

    void enterCharacter(char c) {
        if (stargateIsIdle()) {
            buttonSound();
            String a = getEnteredAddress();
            if (a.length() < addressLength)
                setEnteredAddress(a + c);
        }
    }

    boolean stargateIsIdle() {
        SGBaseBlockEntity te = getStargateTE();
        return te != null && te.state == SGState.Idle;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        SGBaseBlockEntity te = getStargateTE();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        GuiDraw.rect(g, DHD_GUI, (width - dhdWidth) / 2.0, height - dhdHeight, dhdWidth, dhdHeight);
        drawOrangeButton(g, te);
        if (te != null && te.state == SGState.Idle) {
            String entered = getEnteredAddress();
            SGSymbols.drawAddressSymbols(g, width / 2, dhdTop - 80, entered);
            String address = SGAddressing.padAddress(entered, addressLength);
            g.drawCenteredString(font, address, width / 2, dhdTop - 20, 0xffffff);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    void drawOrangeButton(GuiGraphics g, SGBaseBlockEntity te) {
        boolean connected = te != null && te.isActive();
        if (te == null || !te.isMerged)
            GuiDraw.setColor(0.2, 0.2, 0.2);
        else if (connected)
            GuiDraw.setColor(1.0, 0.5, 0.0);
        else
            GuiDraw.setColor(0.5, 0.25, 0.0);
        double rx = dhdWidth * 48 / 512.0;
        double ry = dhdHeight * 48 / 256.0;
        GuiDraw.rect(g, DHD_CENTRE, dhdCentreX - rx, dhdCentreY - ry - 6, 2 * rx, 1.5 * ry, 64, 0, 64, 48, 128, 64);
        GuiDraw.resetColor();
        if (connected) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            double d = 5;
            GuiDraw.rect(g, DHD_CENTRE, dhdCentreX - rx - d, dhdCentreY - ry - d - 6, 2 * (rx + d), ry + d, 0, 0, 64, 32, 128, 64);
            GuiDraw.rect(g, DHD_CENTRE, dhdCentreX - rx - d, dhdCentreY - 6, 2 * (rx + d), 0.5 * ry + d, 0, 32, 64, 32, 128, 64);
            RenderSystem.defaultBlendFunc();
        }
    }
}
