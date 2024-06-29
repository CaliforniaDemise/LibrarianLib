package com.teamwizardry.librarianlib.features.facade.provided.book

import com.teamwizardry.librarianlib.core.LibrarianLib
import com.teamwizardry.librarianlib.features.facade.GuiBase
import com.teamwizardry.librarianlib.features.facade.component.GuiComponent
import com.teamwizardry.librarianlib.features.facade.component.GuiComponentEvents
import com.teamwizardry.librarianlib.features.facade.component.GuiLayerEvents
import com.teamwizardry.librarianlib.features.facade.components.ComponentSprite
import com.teamwizardry.librarianlib.features.facade.components.ComponentText
import com.teamwizardry.librarianlib.features.facade.provided.book.context.BookContext
import com.teamwizardry.librarianlib.features.facade.provided.book.context.ComponentNavBar
import com.teamwizardry.librarianlib.features.facade.provided.book.hierarchy.book.Book
import com.teamwizardry.librarianlib.features.facade.provided.book.hierarchy.entry.Entry
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.sprite.Sprite
import com.teamwizardry.librarianlib.features.sprite.Texture
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.TextFormatting
import java.awt.Color

/**
 * Property of Demoniaque.
 * All rights reserved.
 */
@Suppress("LeakingThis")
open class ModGuiBook(override val book: Book) : GuiBase(), IBookGui {
    override val cachedSearchContent = book.contentCache
    private var sheetRL = ResourceLocation(book.textureSheet)
    private var guideBookSheet = Texture(ResourceLocation(sheetRL.namespace, "textures/" + sheetRL.path + ".png"), 384, 384)

    override var bindingSprite: Sprite = guideBookSheet.getSprite("binding")
    override var pageSprite: Sprite = guideBookSheet.getSprite("book")
    override var bannerSprite: Sprite = guideBookSheet.getSprite("banner")
    override var paperSprite: Sprite = guideBookSheet.getSprite("paper")
    override var bookmarkSprite: Sprite = guideBookSheet.getSprite("bookmark")
    override var searchIconSprite: Sprite = guideBookSheet.getSprite("magnifier")
    override var titleBarSprite: Sprite = guideBookSheet.getSprite("title_bar")
    override var nextSpritePressed: Sprite = guideBookSheet.getSprite("arrow_next_pressed")
    override var nextSprite: Sprite = guideBookSheet.getSprite("arrow_next")
    override var backSpritePressed: Sprite = guideBookSheet.getSprite("arrow_back_pressed")
    override var backSprite: Sprite = guideBookSheet.getSprite("arrow_back")
    override var homeSpritePressed: Sprite = guideBookSheet.getSprite("arrow_home_pressed")
    override var homeSprite: Sprite = guideBookSheet.getSprite("arrow_home")
    override var processArrow: Sprite = guideBookSheet.getSprite("process_arrow")
    override var materialIcon: Sprite = guideBookSheet.getSprite("material_icon")

    final override val mainBookComponent: ComponentSprite
    final override val paperComponent: ComponentSprite
    final override val bindingComponent: ComponentSprite

    override var focus: GuiComponent? = null

    final override val navBar: ComponentNavBar


    init {
        this.main.size = vec(146, 180)
        mainBookComponent = ComponentSprite(pageSprite, 0, 0)
        mainBookComponent.color = book.bookColor

        paperComponent = ComponentSprite(paperSprite, 0, 0)
        mainBookComponent.add(paperComponent)
        bindingComponent = ComponentSprite(bindingSprite, 0, 0)
        bindingComponent.color = book.bindingColor
        mainBookComponent.add(bindingComponent)

        navBar = ComponentNavBar(this, mainBookComponent.size.xi / 2 - 35, mainBookComponent.size.yi, 70)
        mainBookComponent.add(navBar)

        main.add(this.mainBookComponent)
    }

    override var context: BookContext = focusOn(BookContext(this, book))

    override fun makeNavigationButton(offsetIndex: Int, entry: Entry, extra: ((GuiComponent) -> Unit)?): GuiComponent {
        val indexButton = GuiComponent(0, 16 * offsetIndex, this.mainBookComponent.size.xi - 32, 16)

        extra?.invoke(indexButton)
        indexButton.BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) { placeInFocus(entry) }

        // SUB INDEX PLATE RENDERING
        val title = entry.title.toString()
        val icon = entry.icon

        val textComponent = ComponentText(20, Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT / 2, ComponentText.TextAlignH.LEFT, ComponentText.TextAlignV.TOP)
        textComponent.unicode = true
        textComponent.text = title
        indexButton.add(textComponent)

        indexButton.BUS.hook(GuiComponentEvents.MouseMoveInEvent::class.java) { textComponent.text = " " + TextFormatting.ITALIC.toString() + title }

        indexButton.BUS.hook(GuiComponentEvents.MouseMoveOutEvent::class.java) { textComponent.text = TextFormatting.RESET.toString() + title }

        indexButton.tooltip_im {
            val list = mutableListOf<String>()
            entry.title?.add(list)
            entry.desc?.addDynamic(list)

            for (i in 1 until list.size)
                list[i] = TextFormatting.GRAY.toString() + list[i]
            list
        }

        val render = IBookGui.getRendererFor(icon, vec(16.0, 16.0))

        indexButton.BUS.hook(GuiLayerEvents.PreDrawEvent::class.java) { render() }

        return indexButton
    }

    override fun updateTextureData(sheet: String, outerColor: Color, bindingColor: Color) {
        val newSheet = ResourceLocation(sheet)
        if (sheetRL != newSheet) {
            sheetRL = newSheet
            guideBookSheet = Texture(ResourceLocation(sheetRL.namespace, "textures/" + sheetRL.path + ".png"), 384, 384)

            bindingSprite = guideBookSheet.getSprite("binding")
            pageSprite = guideBookSheet.getSprite("book")
            bannerSprite = guideBookSheet.getSprite("banner")
            paperSprite = guideBookSheet.getSprite("paper")
            bookmarkSprite = guideBookSheet.getSprite("bookmark")
            searchIconSprite = guideBookSheet.getSprite("magnifier")
            titleBarSprite = guideBookSheet.getSprite("title_bar")
            nextSpritePressed = guideBookSheet.getSprite("arrow_next_pressed")
            nextSprite = guideBookSheet.getSprite("arrow_next")
            backSpritePressed = guideBookSheet.getSprite("arrow_back_pressed")
            backSprite = guideBookSheet.getSprite("arrow_back")
            homeSpritePressed = guideBookSheet.getSprite("arrow_home_pressed")
            homeSprite = guideBookSheet.getSprite("arrow_home")
            processArrow = guideBookSheet.getSprite("process_arrow")
            materialIcon = guideBookSheet.getSprite("material_icon")
            mainBookComponent.sprite = pageSprite
            paperComponent.sprite = paperSprite
            bindingComponent.sprite = bindingSprite
        }

        mainBookComponent.color = outerColor
        bindingComponent.color = bindingColor
    }

    companion object {

        var ERROR = Sprite(ResourceLocation(LibrarianLib.MODID, "textures/gui/book/error/error.png"))
        var FOF = Sprite(ResourceLocation(LibrarianLib.MODID, "textures/gui/book/error/fof.png"))
    }
}
