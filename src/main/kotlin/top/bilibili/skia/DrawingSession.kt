package top.bilibili.skia

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.slf4j.LoggerFactory
import java.util.*

/**
 * 绘图会话 - 自动追踪和释放 Skia 资源
 */
class DrawingSession : AutoCloseable {
    private val resources = mutableListOf<AutoCloseable>()
    private val sessionId = UUID.randomUUID().toString().take(8)
    private val creationTime = System.currentTimeMillis()
    private var isClosed = false

    private val logger = LoggerFactory.getLogger(DrawingSession::class.java)

    /**
     * 追踪任意 AutoCloseable 对象
     */
    fun <T : AutoCloseable> T.track(): T {
        if (!isClosed) {
            resources.add(this)
        }
        return this
    }

    /**
     * 创建 Surface 并自动追踪
     */
    fun createSurface(width: Int, height: Int): Surface {
        val surface = Surface.makeRasterN32Premul(width, height)
        return surface.track()
    }

    /**
     * 创建 Image 并自动追踪（从字节数组）
     */
    fun createImage(bytes: ByteArray): Image {
        val image = Image.makeFromEncoded(bytes)
        return image.track()
    }

    /**
     * 创建 Paragraph 并自动追踪
     */
    fun createParagraph(
        style: ParagraphStyle,
        fonts: FontCollection,
        width: Float,
        build: ParagraphBuilder.() -> Unit
    ): Paragraph {
        val builder = ParagraphBuilder(style, fonts)
        builder.build()
        val paragraph = builder.build().layout(width)
        return paragraph.track()
    }

    /**
     * 创建 TextLine 并自动追踪
     */
    fun createTextLine(text: String, font: Font): TextLine {
        val textLine = TextLine.make(text, font)
        return textLine.track()
    }

    /**
     * 创建临时 Font 并自动追踪
     */
    fun createFont(typeface: Typeface, size: Float): Font {
        val font = Font(typeface, size)
        return font.track()
    }

    /**
     * 执行绘图并返回图片数据
     */
    inline fun drawToBytes(
        width: Int,
        height: Int,
        format: EncodedImageFormat = EncodedImageFormat.PNG,
        crossinline draw: Canvas.() -> Unit
    ): ByteArray {
        val surface = createSurface(width, height)
        surface.canvas.draw()
        val image = surface.makeImageSnapshot().track()
        return image.encodeToData(format)?.bytes
            ?: throw IllegalStateException("Failed to encode image")
    }

    /**
     * 执行绘图并返回 Image（调用者负责关闭）
     */
    inline fun drawToImage(
        width: Int,
        height: Int,
        crossinline draw: Canvas.() -> Unit
    ): Image {
        val surface = createSurface(width, height)
        surface.canvas.draw()
        // 注意：返回的 Image 不追踪，由调用者负责关闭
        return surface.makeImageSnapshot()
    }

    /**
     * 关闭会话，释放所有资源
     */
    override fun close() {
        if (isClosed) return
        isClosed = true

        val duration = System.currentTimeMillis() - creationTime
        val resourceCount = resources.size

        // 逆序关闭资源（后创建的先关闭）
        resources.asReversed().forEach { resource ->
            runCatching {
                resource.close()
            }.onFailure { e ->
                logger.warn("关闭资源失败: ${resource::class.simpleName}", e)
            }
        }
        resources.clear()

        logger.debug("DrawingSession[$sessionId] 关闭，释放 $resourceCount 个资源，耗时 ${duration}ms")
    }
}
