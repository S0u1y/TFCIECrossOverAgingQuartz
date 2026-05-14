package wizzy.ietfc_crossover_calendar_buds;

import net.minecraft.nbt.CompoundTag;

public class ChunkTickData {
    // Stores the TFC Calendar tick when this chunk was last active
    private long lastUpdateTick = -1;

    public long getLastUpdateTick() { return lastUpdateTick; }
    public void setLastUpdateTick(long tick) { this.lastUpdateTick = tick; }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("lastTfcUpdate", lastUpdateTick);
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        lastUpdateTick = nbt.getLong("lastTfcUpdate");
    }
}
