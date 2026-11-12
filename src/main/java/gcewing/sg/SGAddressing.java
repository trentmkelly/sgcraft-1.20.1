//------------------------------------------------------------------------------------------------
//
//   SG Craft - Conversions between coordinates and stargate addresses
//
//------------------------------------------------------------------------------------------------
//
//   There are 36 symbols. An address consists of 7 or 9 symbols:
//
//      CCCCCCC   or   CCCCCCCDD
//
//   where DD encodes the dimension and CCCCCCC encode the chunk coordinates within the
//   dimension. When connecting to a gate in the same dimension, DD may be omitted, allowing a
//   7-chevron gate to be used.
//
//   The coordinate range is +/- 139,967 chunks = 2,239,472 blocks.
//

package gcewing.sg;

import gcewing.sg.block.entity.SGBaseBlockEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.server.ServerLifecycleHooks;

public class SGAddressing {

    public static class AddressingError extends Exception {
        public AddressingError(String s) {
            super(s);
        }
    }

    public static final String symbolChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static final int numSymbols = symbolChars.length();
    public static final int numCoordSymbols = 7;
    public static final int numDimensionSymbols = 2;

    public static final int maxAddressLength = numCoordSymbols + numDimensionSymbols;
    public static final int maxCoord = 139967;
    public static final int minCoord = -maxCoord;
    public static final int coordRange = maxCoord - minCoord + 1;
    public static final int maxDimensionIndex = 1295;
    public static final int dimensionRange = maxDimensionIndex + 1;
    static final String padding = "---------";
    static final long mc = coordRange + 2;
    static final long pc = 93563;   //  (pc * qc) % mc == 1
    static final long qc = 153742;
    static final long md = dimensionRange + 1;
    static final long pd = 953;  //  (pd * qd) % md == 1
    static final long qd = 788;

    public static boolean isValidSymbolChar(char c) {
        return symbolChars.indexOf(c) >= 0;
    }

    public static char symbolToChar(int i) {
        return symbolChars.charAt(i);
    }

    public static int charToSymbol(char c) {
        return symbolChars.indexOf(c);
    }

    static boolean validSymbols(String s) {
        for (int i = 0; i < s.length(); i++)
            if (charToSymbol(s.charAt(i)) < 0)
                return false;
        return true;
    }

    static void validateAddress(String s) throws AddressingError {
        int l = s.length();
        if ((l == numCoordSymbols || l == numCoordSymbols + numDimensionSymbols) && validSymbols(s))
            return;
        throw new AddressingError("Malformed stargate address");
    }

    public static String normalizeAddress(String address) {
        return address.replace("-", "").replace(" ", "").toUpperCase();
    }

    public static String addressForLocation(SGLocation loc) throws AddressingError {
        int chunkx = loc.pos.getX() >> 4;
        int chunkz = loc.pos.getZ() >> 4;
        if (!inCoordRange(chunkx) || !inCoordRange(chunkz))
            throw new AddressingError("Coordinates out of stargate range");
        int di = SGDimensionMap.indexForDimension(loc.dimension);
        if (di > maxDimensionIndex)
            throw new AddressingError("Dimension not reachable by stargate");
        long c = interleaveCoords(hash(chunkx - minCoord, pc, mc), hash(chunkz - minCoord, pc, mc));
        int dp = permuteDimension(c, di);
        int d = hash(dp, pd, md);
        return longToSymbols(c, numCoordSymbols) + longToSymbols(d, numDimensionSymbols);
    }

    public static SGBaseBlockEntity findAddressedStargate(String address, Level fromWorld) throws AddressingError {
        validateAddress(address);
        String csyms = address.substring(0, numCoordSymbols);
        long c = longFromSymbols(csyms);
        int[] xz = uninterleaveCoords(c);
        int chunkX = minCoord + hash(xz[0], qc, mc);
        int chunkZ = minCoord + hash(xz[1], qc, mc);
        if (address.length() == maxAddressLength) {
            // Absolute address
            String dsyms = address.substring(numCoordSymbols);
            int d = (int) longFromSymbols(dsyms);
            int dp = hash(d, qd, md);
            int di = unpermuteDimension(c, dp);
            ResourceKey<Level> dm = SGDimensionMap.dimensionForIndex(di);
            if (dm != null)
                return getBaseTE(chunkX, chunkZ, dm);
            return null;
        } else {
            // Relative address
            return getBaseTE(chunkX, chunkZ, fromWorld.dimension());
        }
    }

    static SGBaseBlockEntity getBaseTE(int chunkX, int chunkZ, ResourceKey<Level> dimension) {
        ServerLevel toWorld = getWorld(dimension);
        if (toWorld != null && toWorld.getWorldBorder().isWithinBounds(chunkX << 4, chunkZ << 4)) {
            LevelChunk chunk = toWorld.getChunk(chunkX, chunkZ);
            for (BlockEntity te : chunk.getBlockEntities().values())
                if (te instanceof SGBaseBlockEntity gate)
                    return gate;
        }
        return null;
    }

    static int permuteDimension(long c, int d) {
        return (int) ((d + c) % dimensionRange);
    }

    static int unpermuteDimension(long c, int d) {
        int i = (int) ((d - c) % dimensionRange);
        if (i < 0)
            i += dimensionRange;
        return i;
    }

    static long interleaveCoords(int x, int z) {
        long p6 = 1;
        long c = 0;
        while (x > 0 || z > 0) {
            c += p6 * (x % 6);
            x /= 6;
            p6 *= 6;
            c += p6 * (z % 6);
            z /= 6;
            p6 *= 6;
        }
        return c;
    }

    static int[] uninterleaveCoords(long c) {
        int p6 = 1;
        int[] xy = {0, 0};
        while (c > 0) {
            xy[0] += p6 * (c % 6);
            c /= 6;
            xy[1] += p6 * (c % 6);
            c /= 6;
            p6 *= 6;
        }
        return xy;
    }

    static int hash(int i, long f, long m) {
        return (int) (((i + 1) * f) % m) - 1;
    }

    public static ServerLevel getWorld(ResourceKey<Level> dimension) {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getLevel(dimension) : null;
    }

    static boolean inCoordRange(int i) {
        return i >= minCoord && i <= maxCoord;
    }

    static String longToSymbols(long i, int n) {
        StringBuilder s = new StringBuilder();
        while (n-- > 0) {
            s.insert(0, symbolToChar((int) (i % numSymbols)));
            i /= numSymbols;
        }
        return s.toString();
    }

    static long longFromSymbols(String s) {
        long i = 0;
        for (int j = 0; j < s.length(); j++)
            i = i * numSymbols + charToSymbol(s.charAt(j));
        return i;
    }

    public static String padAddress(String address, int maxLength) {
        if (maxLength < numCoordSymbols)
            maxLength = numCoordSymbols;
        if (address.length() >= maxLength)
            return formatAddress(address, " ", " ");
        return formatAddress(address + padding.substring(address.length(), maxLength), " ", " ");
    }

    public static String formatAddress(String address, String sep1, String sep2) {
        String coord = address.substring(0, numCoordSymbols);
        String dimen = address.substring(numCoordSymbols);
        int i = (numCoordSymbols + 1) / 2;
        String result = coord.substring(0, i) + sep1 + coord.substring(i);
        if (!dimen.isEmpty())
            result += sep2 + dimen;
        return result;
    }

    public static String localAddress(String address) {
        return address.substring(0, numCoordSymbols);
    }
}
