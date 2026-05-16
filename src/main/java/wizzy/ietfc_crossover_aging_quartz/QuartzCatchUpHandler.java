package wizzy.ietfc_crossover_aging_quartz;

import com.nmagpie.tfc_ie_addon.common.blocks.BuddingQuartzBlock;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.nmagpie.tfc_ie_addon.common.blocks.Blocks.*;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QuartzCatchUpHandler {

    private static final double GROWTH_PROBABILITY_PER_TICK = 1.0 / 122880.0;

    // Stores chunk updates safely until the main thread is ready
    private static final Queue<Runnable> CATCHUP_QUEUE = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        ChunkTickData data = chunk.getCapability(QuartzCapabilityProvider.CHANCE_CAP).orElse(null);
        if (data == null) return;

        ICalendar calendar = Calendars.get(level);

        long currentTicks = calendar.getCalendarTicks();
        long lastTicks = data.getLastUpdateTick();

        if (lastTicks == -1 || currentTicks <= lastTicks) {
            data.setLastUpdateTick(currentTicks);
            return;
        }

        long deltaTicks = currentTicks - lastTicks;
        deltaTicks = Math.min(deltaTicks, calendar.getCalendarTicksInYear());

        data.setLastUpdateTick(currentTicks);

        // DEFER EXECUTION: Add to the queue instead of running it right now
        final long finalDelta = deltaTicks;
        CATCHUP_QUEUE.add(() -> {
            // Make double sure the chunk didn't unload while waiting in the queue
            if (level.isLoaded(chunk.getPos().getWorldPosition())) {
                runCatchUp(level, chunk, finalDelta);
            }
        });
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        // Right before the chunk saves to the hard drive, update lastUpdateTicks
        chunk.getCapability(QuartzCapabilityProvider.CHANCE_CAP).ifPresent(data -> {
            long currentTicks = Calendars.get(level).getTicks();
            data.setLastUpdateTick(currentTicks);
        });
    }

    // The Server Tick works through the queue
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Process a maximum of 5 chunks per tick to prevent lag spikes
        int processed = 0;
        while (!CATCHUP_QUEUE.isEmpty() && processed < 5) {
            Runnable task = CATCHUP_QUEUE.poll();
            if (task != null) {
                task.run();
                processed++;
            }
        }
    }

    private static void runCatchUp(ServerLevel level, LevelChunk chunk, long delta) {
        LevelChunkSection[] sections = chunk.getSections();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir()) continue;

            PalettedContainer<BlockState> palette = section.getStates();
            if (palette.maybeHas(state -> state.getBlock() instanceof BuddingQuartzBlock)) {

                int sectionMinY = chunk.getMinBuildHeight() + (i * 16);

                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            BlockState state = palette.get(x, y, z);

                            if (state.getBlock() instanceof BuddingQuartzBlock) {
                                BlockPos pos = chunk.getPos().getBlockAt(x, sectionMinY + y, z);
                                simulateGrowth(level, pos, delta);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void simulateGrowth(ServerLevel level, BlockPos pos, long delta) {
        RandomSource random = level.getRandom();
        double lambda = delta * GROWTH_PROBABILITY_PER_TICK;
        if (lambda > 30) lambda = 20; //algorithm isn't efficient for lambda > 30.

        for (Direction dir : Direction.values()) {
//            https://www.johndcook.com/blog/2010/06/14/generating-poisson-random-values/
            int stagesToGrow = 0;
            double L = Math.exp(-lambda);
            double p = 1.0;

            do {
                stagesToGrow++;
                p *= random.nextDouble();
            } while (p > L);
            stagesToGrow--;
            stagesToGrow = Math.min(stagesToGrow, 4);

            if (stagesToGrow > 0) {
                applyGrowthStages(level, pos.relative(dir), dir, stagesToGrow);
            }
        }
    }

    private static void applyGrowthStages(ServerLevel level, BlockPos targetPos, Direction dir, int stages) {
        // if the block we are trying to grow into is in an unloaded chunk, abort
        if (!level.isLoaded(targetPos)) return;

        for (int i = 0; i < stages; i++) {
            BlockState currentState = level.getBlockState(targetPos);
            Block nextBlock = null;

            if (currentState.isAir()) nextBlock = SMALL_QUARTZ_BUD.get();
            else if (currentState.is(SMALL_QUARTZ_BUD.get())) nextBlock = MEDIUM_QUARTZ_BUD.get();
            else if (currentState.is(MEDIUM_QUARTZ_BUD.get())) nextBlock = LARGE_QUARTZ_BUD.get();
            else if (currentState.is(LARGE_QUARTZ_BUD.get())) nextBlock = QUARTZ_CLUSTER.get();

            if (nextBlock == null) break;

            BlockState nextState = nextBlock.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, dir)
                    .setValue(AmethystClusterBlock.WATERLOGGED, level.getFluidState(targetPos).getType() == Fluids.WATER);

            level.setBlockAndUpdate(targetPos, nextState);
        }
    }
}
