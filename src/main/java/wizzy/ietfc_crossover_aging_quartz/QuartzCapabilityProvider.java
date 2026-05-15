package wizzy.ietfc_crossover_aging_quartz;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuartzCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<ChunkTickData> CHANCE_CAP = CapabilityManager.get(new CapabilityToken<>() {});

    private ChunkTickData data = null;
    private final LazyOptional<ChunkTickData> optional = LazyOptional.of(this::createData);

    private ChunkTickData createData() {
        if (this.data == null) {
            this.data = new ChunkTickData();
        }
        return this.data;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CHANCE_CAP) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return createData().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createData().deserializeNBT(nbt);
    }
}
