package com.example

data class PagePoint(val x: Float, val y: Float)

data class PdfPoint(val x: Float, val y: Float)

data class PdfStroke(
    val points: List<PagePoint>,
    val colorArgb: Int,
    /** Stroke width as a fraction of the displayed page width. */
    val width: Float
)

data class PdfTextAnnotation(
    val text: String,
    val position: PagePoint,
    val colorArgb: Int,
    /** Font size as a fraction of the displayed page width. */
    val fontSize: Float
)

data class PageAnnotationSet(
    val strokes: List<PdfStroke> = emptyList(),
    val text: PdfTextAnnotation? = null
)

const val DEFAULT_TEXT_FONT_SIZE_RATIO = 0.05f

data class PageViewport(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height

    fun screenToPage(x: Float, y: Float): PagePoint? {
        if (x < left || x > right || y < top || y > bottom || width <= 0f || height <= 0f) return null
        return PagePoint((x - left) / width, (y - top) / height)
    }

    fun pageToScreen(point: PagePoint): PagePoint = PagePoint(
        left + point.x * width,
        top + point.y * height
    )

    fun normalizedStrokeWidth(screenPixels: Float): Float = screenPixels / width

    fun screenStrokeWidth(normalizedWidth: Float): Float = normalizedWidth * width

    companion object {
        fun fit(containerWidth: Float, containerHeight: Float, pageWidth: Float, pageHeight: Float): PageViewport {
            if (containerWidth <= 0f || containerHeight <= 0f || pageWidth <= 0f || pageHeight <= 0f) {
                return PageViewport(0f, 0f, 0f, 0f)
            }
            val scale = minOf(containerWidth / pageWidth, containerHeight / pageHeight)
            val width = pageWidth * scale
            val height = pageHeight * scale
            return PageViewport(
                left = (containerWidth - width) / 2f,
                top = (containerHeight - height) / 2f,
                width = width,
                height = height
            )
        }
    }
}

data class PdfPageGeometry(
    val left: Float,
    val bottom: Float,
    val width: Float,
    val height: Float,
    val rotation: Int
) {
    private val normalizedRotation: Int get() = ((rotation % 360) + 360) % 360
    val displayWidth: Float get() = if (normalizedRotation == 90 || normalizedRotation == 270) height else width
    val displayHeight: Float get() = if (normalizedRotation == 90 || normalizedRotation == 270) width else height

    fun toPdfPoint(point: PagePoint): PdfPoint = when (normalizedRotation) {
        90 -> PdfPoint(left + point.y * width, bottom + point.x * height)
        180 -> PdfPoint(left + (1f - point.x) * width, bottom + point.y * height)
        270 -> PdfPoint(left + (1f - point.y) * width, bottom + (1f - point.x) * height)
        else -> PdfPoint(left + point.x * width, bottom + (1f - point.y) * height)
    }
}
