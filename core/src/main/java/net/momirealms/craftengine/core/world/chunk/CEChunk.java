package net.momirealms.craftengine.core.world.chunk;

import net.momirealms.craftengine.core.block.EmptyBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.world.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CEChunk {
    private boolean loaded;
    private final CEWorld world;
    private final ChunkPos chunkPos;
    private final CESection[] sections;
    private final WorldHeight worldHeightAccessor;
    private final List<Vec3d> entities;

    public CEChunk(CEWorld world, ChunkPos chunkPos) {
        this.world = world;
        this.chunkPos = chunkPos;
        this.worldHeightAccessor = world.worldHeight();
        this.sections = new CESection[this.worldHeightAccessor.getSectionsCount()];
        this.entities = new ArrayList<>();
        this.fillEmptySection();
    }

    public CEChunk(CEWorld world, ChunkPos chunkPos, CESection[] sections, List<Vec3d> entities) {
        this.world = world;
        this.chunkPos = chunkPos;
        this.entities = entities;
        this.worldHeightAccessor = world.worldHeight();
        int sectionCount = this.worldHeightAccessor.getSectionsCount();
        this.sections = new CESection[sectionCount];
        if (sections != null) {
            for (CESection section : sections) {
                if (section != null) {
                    int index = sectionIndex(section.sectionY());
                    this.sections[index] = section;
                }
            }
        }
        this.fillEmptySection();
    }

    public boolean isEmpty() {
        if (!this.entities.isEmpty()) return false;
        for (CESection section : this.sections) {
            if (section != null && !section.statesContainer().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void fillEmptySection() {
        for (int i = 0; i < sections.length; ++i) {
            if (sections[i] == null) {
                sections[i] = new CESection(world.worldHeight().getSectionYFromSectionIndex(i),
                        new PalettedContainer<>(null, EmptyBlock.INSTANCE.defaultState(), PalettedContainer.PaletteProvider.CUSTOM_BLOCK_STATE));
            }
        }
    }

    public void setBlockState(BlockPos pos, ImmutableBlockState state) {
        this.setBlockState(pos.x(), pos.y(), pos.z(), state);
    }

    public void setBlockState(int x, int y, int z, ImmutableBlockState state) {
        int index = sectionIndex(SectionPos.blockToSectionCoord(y));
        CESection section = this.sections[index];
        if (section == null) {
            return;
        }
        section.setBlockState((y & 15) << 8 | (z & 15) << 4 | x & 15, state);
    }

    @Nullable
    public ImmutableBlockState getBlockState(BlockPos pos) {
        return getBlockState(pos.x(), pos.y(), pos.z());
    }

    @Nullable
    public ImmutableBlockState getBlockState(int x, int y, int z) {
        int index = sectionIndex(SectionPos.blockToSectionCoord(y));
        CESection section = this.sections[index];
        if (section == null) {
            return null;
        }
        return section.getBlockState((y & 15) << 8 | (z & 15) << 4 | x & 15);
    }

    @Nullable
    public CESection sectionByIndex(int index) {
        return this.sections[index];
    }

    @Nullable
    public CESection sectionById(int sectionId) {
        return this.sections[sectionIndex(sectionId)];
    }

    public int sectionIndex(int sectionId) {
        return sectionId - this.worldHeightAccessor.getMinSection();
    }

    public int sectionY(int sectionIndex) {
        return sectionIndex + this.worldHeightAccessor.getMinSection();
    }

    @NotNull
    public CEWorld world() {
        return world;
    }

    @NotNull
    public ChunkPos chunkPos() {
        return chunkPos;
    }

    @NotNull
    public CESection[] sections() {
        return sections;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void load() {
        if (this.loaded) return;
        this.world.addLoadedChunk(this);
        this.loaded = true;
    }

    public void unload() {
        if (!this.loaded) return;
        this.world.removeLoadedChunk(this);
        this.loaded = false;
    }
}
