package com.example

import android.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditPdfAnnotationGeometryTest {
    @Test
    fun fitAccountsForLetterboxingAndMapsPageCorners() {
        val viewport = PageViewport.fit(
            containerWidth = 400f,
            containerHeight = 800f,
            pageWidth = 300f,
            pageHeight = 400f
        )

        assertEquals(0f, viewport.left, EPSILON)
        assertEquals(133.33333f, viewport.top, 0.01f)
        assertEquals(400f, viewport.width, EPSILON)
        assertEquals(533.3333f, viewport.height, 0.01f)
        assertEquals(PagePoint(0f, 0f), viewport.screenToPage(0f, viewport.top))
        assertEquals(PagePoint(1f, 1f), viewport.screenToPage(viewport.width, viewport.bottom))
    }

    @Test
    fun pointsOutsideDisplayedPageAreRejected() {
        val viewport = PageViewport.fit(400f, 800f, 300f, 400f)

        assertNull(viewport.screenToPage(10f, viewport.top - 1f))
        assertNull(viewport.screenToPage(viewport.right + 1f, viewport.top + 10f))
    }

    @Test
    fun normalizedPointsAndThicknessSurvivePreviewResize() {
        val first = PageViewport.fit(400f, 800f, 300f, 400f)
        val resized = PageViewport.fit(720f, 900f, 300f, 400f)
        val expected = PagePoint(0.23f, 0.81f)
        val firstTouch = first.pageToScreen(expected)
        val resizedTouch = resized.pageToScreen(expected)

        assertEquals(expected.x, first.screenToPage(firstTouch.x, firstTouch.y)!!.x, EPSILON)
        assertEquals(expected.y, resized.screenToPage(resizedTouch.x, resizedTouch.y)!!.y, EPSILON)
        assertEquals(0.02f, first.normalizedStrokeWidth(8f), EPSILON)
        assertEquals(0.02f, resized.screenStrokeWidth(0.02f) / resized.width, EPSILON)
    }

    @Test
    fun mapsDisplayedCoordinatesToPdfCoordinatesForAllPageRotations() {
        val page = PdfPageGeometry(left = 10f, bottom = 20f, width = 600f, height = 800f, rotation = 0)
        assertEquals(PdfPoint(10f, 820f), page.toPdfPoint(PagePoint(0f, 0f)))
        assertEquals(PdfPoint(610f, 20f), page.toPdfPoint(PagePoint(1f, 1f)))

        val quarterTurn = page.copy(rotation = 90)
        assertEquals(800f, quarterTurn.displayWidth, EPSILON)
        assertEquals(600f, quarterTurn.displayHeight, EPSILON)
        assertEquals(PdfPoint(10f, 20f), quarterTurn.toPdfPoint(PagePoint(0f, 0f)))
        assertEquals(PdfPoint(610f, 820f), quarterTurn.toPdfPoint(PagePoint(1f, 1f)))

        assertEquals(PdfPoint(610f, 20f), page.copy(rotation = 180).toPdfPoint(PagePoint(0f, 0f)))
        assertEquals(PdfPoint(610f, 820f), page.copy(rotation = 270).toPdfPoint(PagePoint(0f, 0f)))
    }

    @Test
    fun pageAnnotationsRemainAvailableAfterConfigurationChange() {
        val original = mapOf(
            1 to PageAnnotationSet(
                strokes = listOf(
                    PdfStroke(
                        points = listOf(PagePoint(0.02f, 0.03f), PagePoint(0.98f, 0.96f)),
                        colorArgb = Color.RED,
                        width = 0.015f
                    ),
                    PdfStroke(
                        points = listOf(PagePoint(0.5f, 0.5f)),
                        colorArgb = Color.BLUE,
                        width = 0.03f
                    )
                )
            ),
            3 to PageAnnotationSet(
                text = PdfTextAnnotation("Page three", PagePoint(0.1f, 0.2f), Color.BLACK, 0.05f)
            )
        )

        val store = ViewModelStore()
        val initialState = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())[
            EditPdfAnnotationViewModel::class.java
        ]
        initialState.annotationsByPage.putAll(original)
        val stateAfterRotation = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())[
            EditPdfAnnotationViewModel::class.java
        ]

        assertSame(initialState, stateAfterRotation)
        assertEquals(original, stateAfterRotation.annotationsByPage.toMap())
        assertEquals(setOf(1, 3), stateAfterRotation.annotationsByPage.keys.toSet())
        store.clear()
    }

    @Test
    fun normalizedStrokeNearPageCenterMapsToPdfCenter() {
        val page = PdfPageGeometry(left = 0f, bottom = 0f, width = 612f, height = 792f, rotation = 0)

        assertEquals(PdfPoint(306f, 396f), page.toPdfPoint(PagePoint(0.5f, 0.5f)))
    }

    private companion object {
        const val EPSILON = 0.0001f
    }
}
