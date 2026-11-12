package gcewing.sg;

import net.minecraft.world.level.block.entity.BlockEntity;

/** A block entity below a stargate that forwards gate events to attached computers. */
public interface IComputerInterface {
    void postEvent(BlockEntity source, String name, Object... args);
}
