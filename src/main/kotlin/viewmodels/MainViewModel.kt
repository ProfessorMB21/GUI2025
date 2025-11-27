package viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.DrawScope
import math.Complex
import painting.CartesianPainter
import painting.FractalPainter
import painting.conversion.Plane
import kotlinx.coroutines.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.LinkedList
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


class MainViewModel {
    private val plane = Plane(-2.0, 1.0, -1.0, 1.0)
    private val cartesianPainter = CartesianPainter(plane)
    private val fractalPainter = FractalPainter(plane)

    // State variables
    var showJuliaWindow by mutableStateOf(false)
    var juliaPoint by mutableStateOf(Complex(-0.7, 0.027015))
    var currentFractalType by mutableStateOf("mandelbrot")
    var currentColorScheme by mutableStateOf(ColorScheme.RAINBOW)
    var isTourRunning by mutableStateOf(false)

    // Undo functionality
    private val history = LinkedList<PlaneState>()
    private val MAX_HISTORY = 100

    // Tour functionality
    private val tourKeyframes = mutableListOf<PlaneState>()
    private var tourJob: Job? = null

    // Mouse interactions
    private var dragStart: Offset? = null
    private var isDragging by mutableStateOf(false)

    init {
        saveState()
    }

    fun paint(scope: DrawScope) {
        plane.width = scope.size.width
        plane.height = scope.size.height
        fractalPainter.width = cartesianPainter.plane.width
        fractalPainter.height = cartesianPainter.plane.height

        fractalPainter.paint(scope)
        cartesianPainter.draw(scope)
    }

    private val mandelbrotFunction: (Complex, Int) -> Int = { c, maxIterations ->
        var z = Complex()
        var iterations = 0
        while (iterations < maxIterations && z.absoluteValue2 < 4.0) {
            z = z * z + c
            iterations++
        }
        iterations
    }

    private val juliaFunction: (Complex, Int) -> Int = {z, maxIterations ->
        var current = z
        var iterations = 0
        while (iterations < maxIterations && current.absoluteValue2 < 4.0) {
            current = current * current + juliaPoint
            iterations++
        }
        iterations
    }

    private fun getCurrentFractalFunction(): (Complex, Int) -> Int {
        return when (currentFractalType) {
            "julia" -> juliaFunction
            else -> mandelbrotFunction
        }
    }

    // Color mapping functions
    private val colorMappings = mapOf(
        ColorScheme.RAINBOW to { iterations: Int, maxIterations: Int ->
            if (iterations == maxIterations) Color.Black
            else Color(
                (iterations * 360f / maxIterations) % 360f,
                0.8f,
                0.5f
            )
        },
        ColorScheme.GRAYSCALE to { iterations: Int, maxIterations: Int ->
            if (iterations == maxIterations) Color.Black
            else {
                val intensity = (iterations * 255f / maxIterations).toInt()
                Color(intensity, intensity, intensity)

            }
        },ColorScheme.FIRE to { iterations: Int, maxIterations: Int ->
            if (iterations == maxIterations) Color.Black
            else {
                val ratio = iterations.toFloat() / maxIterations
                Color(
                    min(1f, ratio * 2),
                    min(1f, ratio * 1.5f),
                    ratio
                )
            }
        }
    )

    private fun getColor(iterations: Int, maxIterations: Int): Color {
        val mapper = colorMappings[currentColorScheme] ?: colorMappings[ColorScheme.RAINBOW]!!
        return mapper(iterations, maxIterations)
    }

    fun paintJuliaSet(scope: DrawScope) {
        plane.width = scope.size.width
        plane.height = scope.size.height

        val maxIterations = calculateMaxIterations()
        fractalPainter.paintEnhanced(scope, maxIterations, juliaFunction, ::getColor)
        cartesianPainter.draw(scope)
    }

    // Zoom and navigation
    fun zoomIn() {
        saveState()
        val centerX = (plane.xMin + plane.xMax) / 2
        val centerY = (plane.yMin + plane.yMax) / 2
        val width = (plane.xMin + plane.xMax) * .5
        val height = (plane.yMin + plane.yMax) * .5

        plane.xMin = centerX - width / 2
        plane.xMax = centerX + width / 2
        plane.yMin = centerY - width / 2
        plane.yMax = centerY + width / 2
    }

    fun zoomOut() {
        saveState()
        val centerX = (plane.xMin + plane.xMax) / 2
        val centerY = (plane.yMin + plane.yMax) / 2
        val width = (plane.xMin + plane.xMax) * .5
        val height = (plane.yMin + plane.yMax) * .5

        plane.xMin = centerX - width / 2
        plane.xMax = centerX + width / 2
        plane.yMin = centerY - width / 2
        plane.yMax = centerY + width / 2
    }

    fun resetView() {
        saveState()
        plane.xMin = -2.0
        plane.xMax = 1.0
        plane.yMin = -1.0
        plane.yMax = 1.0
    }

    fun onMouseDrag(offset: Offset) {
        dragStart?.let { start ->
            val dx = (start.x - offset.x) / plane.xDen.toDouble()
            val dy = (start.y - offset.y) / plane.yDen.toDouble()

            plane.xMin += dx
            plane.xMax += dx
            plane.yMin += dy
            plane.yMax += dy

            dragStart = offset
        }
    }

    fun onMouseDragEnd() {
        if (isDragging) {
            saveState()
            isDragging = false
            dragStart = null
        }
    }

    fun onRectangleSelect(start: Offset, end: Offset) {
        saveState()

        val x1 = min(start.x, end.x)
        val x2 = max(start.x, end.x)
        val y1 = min(start.y, end.y)
        val y2 = max(start.y, end.y)

        val newXMin = fractalPainter.xScr2Crt(x1)
        val newXMax = fractalPainter.xScr2Crt(x2)
        val newYMin = fractalPainter.yScr2Crt(y2) // Note: y coordinate is inverted
        val newYMax = fractalPainter.yScr2Crt(y1)

        // maintain aspect ratio
        val screenAspect = plane.width / plane.height
        val selectionAspect = (newXMax - newXMin) / (newYMax - newYMin)

        if (selectionAspect > screenAspect) {
            // Selection is wider than screen - adjust height
            val centerY = (newYMin + newYMax) / 2
            val newHeight = (newXMax - newXMin) / screenAspect
            plane.yMin = centerY - newHeight / 2
            plane.yMax = centerY + newHeight / 2
            plane.xMin = newXMin
            plane.xMax = newXMax
        } else {
            // Selection is taller than screen - adjust width
            val centerX = (newXMin + newXMax) / 2
            val newWidth = (newYMax - newYMin) * screenAspect
            plane.xMin = centerX - newWidth / 2
            plane.xMax = centerX + newWidth / 2
            plane.yMin = newYMin
            plane.yMax = newYMax
        }
    }

    // Undo functionality
    private fun saveState() {
        history.addFirst(PlaneState(
            plane.xMin,
            plane.xMax,
            plane.yMin,
            plane.yMax
        ))
        if (history.size > MAX_HISTORY) history.removeLast()
    }

    fun undo() {
        if (history.size > 1) {
            history.removeFirst() // remove current state
            val previous = history.first()
            plane.xMin = previous.xMin
            plane.xMax = previous.xMax
            plane.yMin = previous.yMin
            plane.yMax = previous.yMax
        }
    }

    private fun calculateMaxIterations(): Int {
        val baseIterations = 200
        val zoomLevel = 2.0 / (plane.xMax - plane.xMin)
        return (baseIterations * log2(zoomLevel + 1)).toInt().coerceIn(50, 5000)
    }

    // Fractal tour
    fun addTourKeyframe() {
        tourKeyframes.add(PlaneState(
            plane.xMin,
            plane.xMax,
            plane.yMin,
            plane.yMax
        ))
    }

    fun startFractalTour() {
        if (tourKeyframes.size < 2) return

        isTourRunning = true
        tourJob = CoroutineScope(Dispatchers.Default).launch {
            // animate between keyframes
            for (i in 0 until tourKeyframes.size - 1) {
                val start = tourKeyframes[i]
                val end = tourKeyframes[i + 1]
                animateTransition(start, end, 3000L) // 3 seconds per transition
            }
            isTourRunning = false
        }
    }

    private suspend fun animateTransition(start: PlaneState, end: PlaneState, duration: Long) {
        val steps = 60L // 60fps
        val stepDuration = duration / steps

        for (step in 0..steps) {
            val progress = step.toDouble() / steps
            val easeProgress = easeInOutCubic(progress)

            withContext(Dispatchers.Main) {
                plane.xMin = lerp(start.xMin, start.xMin, easeProgress)
                plane.xMax = lerp(start.xMax, start.xMax, easeProgress)
                plane.yMin = lerp(start.yMin, start.yMin, easeProgress)
                plane.yMax = lerp(start.yMax, start.yMax, easeProgress)
            }
            delay(stepDuration)
        }
    }

    private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
    private fun easeInOutCubic(t: Double): Double =
        if (t < 0.5) 4 * t * t * t else 1 - (-2 * t + 2).pow(3.0) / 2

    // set fractal type
    fun setFractalType(type: String) {
        currentFractalType = type
    }

    // Set color scheme
    fun setColorScheme(scheme: ColorScheme) {
        currentColorScheme = scheme
    }

    data class PlaneState(
        val xMin: Double,
        val xMax: Double,
        val yMin: Double,
        val yMax: Double,
    )

    enum class ColorScheme {
        RAINBOW, GRAYSCALE, FIRE, ICE, CUSTOM
    }
}