package com.teamwizardry.librarianlib.foundation.block

import net.minecraftforge.client.model.generators.BlockStateProvider
import net.minecraftforge.common.extensions.IForgeBlock

/**
 * An interface for implementing Foundation's extended block functionality.
 */
interface IFoundationBlock: IForgeBlock {
    /**
     * Generates the models for this block
     */
    @JvmDefault
    fun generateBlockState(gen: BlockStateProvider) {
    }
}