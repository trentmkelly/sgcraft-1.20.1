package gcewing.sg.client;

import gcewing.sg.SGAddressing;
import gcewing.sg.SGCraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Drawing of stargate address glyphs (from Greg's SGScreen). */
public final class SGSymbols {

    static final ResourceLocation SYMBOLS = SGCraft.loc("textures/gui/symbols48.png");
    static final int symbolsPerRowInTexture = 10;
    static final int symbolWidthInTexture = 48;
    static final int symbolHeightInTexture = 48;
    static final int symbolTextureWidth = 512;
    static final int symbolTextureHeight = 256;
    static final int frameHeight = 44;
    static final int cellSize = 24;

    private SGSymbols() {
    }

    public static void drawAddressSymbols(GuiGraphics g, int x, int y, String address) {
        int x0 = x - address.length() * cellSize / 2;
        int y0 = y + frameHeight / 2 - cellSize / 2;
        double tw = symbolTextureWidth * cellSize / (double) symbolWidthInTexture;
        double th = symbolTextureHeight * cellSize / (double) symbolHeightInTexture;
        for (int i = 0; i < address.length(); i++) {
            int s = SGAddressing.charToSymbol(address.charAt(i));
            if (s < 0)
                continue;
            int row = s / symbolsPerRowInTexture;
            int col = s % symbolsPerRowInTexture;
            GuiDraw.rect(g, SYMBOLS, x0 + i * cellSize, y0, cellSize, cellSize,
                col * cellSize, row * cellSize, cellSize, cellSize, tw, th);
        }
    }
}
