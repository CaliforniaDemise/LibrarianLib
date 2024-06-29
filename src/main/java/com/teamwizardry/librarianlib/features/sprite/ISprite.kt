package com.teamwizardry.librarianlib.features.sprite

/*
 * Created by bluexin.
 * Made for LibrarianLib, under GNU LGPL v3.0
 * (a copy of which can be found at the repo root)
 */

import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly


/**
 * Abstraction for Sprites.
 * Most use cases will use the [Sprite] implementation, however
 * the [LTextureAtlasSprite] is needed in some cases.
 */
@SideOnly(Side.CLIENT)
@JvmDefaultWithoutCompatibility
interface ISprite {

    /**
     * Binds the texture to be used for rendering.
     */
    fun bind()

    /**
     * The minimum U coordinate (0-1)
     */
    fun minU(animFrames: Int = 0): Float

    fun minU() = minU(0) // JVM overload

    /**
     * The minimum V coordinate (0-1)
     */
    fun minV(animFrames: Int = 0): Float

    fun minV() = minU(0) // JVM overload

    /**
     * The maximum U coordinate (0-1)
     */
    fun maxU(animFrames: Int = 0): Float

    fun maxU() = minU(0) // JVM overload

    /**
     * The maximum V coordinate (0-1)
     */
    fun maxV(animFrames: Int = 0): Float

    fun maxV() = minU(0) // JVM overload

    /**
     * Draws the sprite to the screen
     * @param x The x position to draw at
     * *
     * @param y The y position to draw at
     */
    fun draw(animTicks: Int, x: Float, y: Float) {
        DrawingUtil.draw(this, animTicks, x, y, width.toFloat(), height.toFloat())
    }

    /**
     * Draws the sprite to the screen with a custom width and height
     * @param x The x position to draw at
     * *
     * @param y The y position to draw at
     * *
     * @param width The width to draw the sprite
     * *
     * @param height The height to draw the sprite
     */
    fun draw(animTicks: Int, x: Float, y: Float, width: Float, height: Float) {
        DrawingUtil.draw(this, animTicks, x, y, width, height)
    }

    /**
     * Draws the sprite to the screen with a custom width and height by clipping or tiling instead of stretching/squashing
     * @param x
     * *
     * @param y
     * *
     * @param width
     * *
     * @param height
     */
    @Deprecated("Replaced with sprite pinning functionality", replaceWith = ReplaceWith(
        "pinnedWrapper(!reverseY, reverseY, !reverseX, reverseX)" +
            ".draw(animTicks, x, y, width.toFloat(), height.toFloat())"
    ))
    fun drawClipped(animTicks: Int, x: Float, y: Float, width: Int, height: Int, reverseX: Boolean = false, reverseY: Boolean = false) {
        this.pinnedWrapper(!reverseY, reverseY, !reverseX, reverseX).draw(animTicks, x, y, width.toFloat(), height.toFloat())
    }

    @Deprecated("Replaced with sprite pinning functionality", replaceWith = ReplaceWith(
        "pinnedWrapper(true, true, false, false).draw(animTicks, x, y, width.toFloat(), height.toFloat())"
    ))
    fun drawClipped(animTicks: Int, x: Float, y: Float, width: Int, height: Int) =
        drawClipped(animTicks, x, y, width, height, false, false)

    /**
     * The logical width of this sprite.
     */
    val width: Int

    @Deprecated("`width` is now logical width", replaceWith = ReplaceWith("width"))
    val inWidth: Int get() = width

    /**
     * The logical height of this sprite.
     */
    val height: Int

    @Deprecated("`height` is now logical height", replaceWith = ReplaceWith("height"))
    val inHeight: Int get() = height

    /**
     * The UV width of this sprite.
     */
    val uSize: Float

    /**
     * The UV height of this sprite.
     */
    val vSize: Float

    /**
     * Frames for this sprite (if animated). Must be >= 1
     */
    val frameCount: Int

    /**
     * The fraction of the sprite along the minimum U edge that should not be distorted when stretching the sprite.
     */
    val minUCap: Float get() = 0f

    /**
     * The fraction of the sprite along the minimum V edge that should not be distorted when stretching the sprite.
     */
    val minVCap: Float get() = 0f

    /**
     * The fraction of the sprite along the maximum U edge that should not be distorted when stretching the sprite.
     */
    val maxUCap: Float get() = 0f

    /**
     * The fraction of the sprite along the maximum V edge that should not be distorted when stretching the sprite.
     */
    val maxVCap: Float get() = 0f

    /**
     * Whether the top of this sprite should be pinned. If this is false the top of the texture will truncate or
     * repeat if the sprite is drawn shorter or taller than normal.
     *
     * If both this and [pinBottom] are false, this sprite will render as if both were true
     */
    val pinTop: Boolean get() = true

    /**
     * Whether the bottom of this sprite should be pinned. If this is false the bottom of the texture will truncate or
     * repeat if the sprite is drawn shorter or taller than normal.
     *
     * If both this and [pinTop] are false, this sprite will render as if both were true
     */
    val pinBottom: Boolean get() = true

    /**
     * Whether the left side of this sprite should be pinned. If this is false the left side of the texture will
     * truncate or repeat if the sprite is drawn narrower or wider than normal.
     *
     * If both this and [pinRight] are false, this sprite will render as if both were true
     */
    val pinLeft: Boolean get() = true

    /**
     * Whether the right side of this sprite should be pinned. If this is false the right side of the texture will
     * truncate or repeat if the sprite is drawn narrower or wider than normal.
     *
     * If both this and [pinLeft] are false, this sprite will render as if both were true
     */
    val pinRight: Boolean get() = true

    /**
     * The number of clockwise 90° rotations should be made when drawing this sprite.
     */
    val rotation: Int get() = 0

    fun pinnedWrapper(top: Boolean, bottom: Boolean, left: Boolean, right: Boolean): ISprite {
        return PinnedWrapper(this, top, bottom, left, right)
    }
}