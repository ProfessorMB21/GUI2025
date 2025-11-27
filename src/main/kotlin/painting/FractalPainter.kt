package painting

import androidx.compose.material.icons.materialIcon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import fractal.Mandelbrot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import math.Complex
import painting.conversion.Converter
import painting.conversion.Plane

class FractalPainter(private val plane: Plane) : Painter {
    override var width: Float
        get() = plane.width
        set(value){
            plane.width = value
        }

    override var height: Float
        get() = plane.height
        set(value){
            plane.height = value
        }

    private var renderingJob: Job? = null

    override fun paint(scope: DrawScope) {
        val m = Mandelbrot()
        repeat(width.toInt()){ iX ->
            val x = iX.toFloat()
            repeat(height.toInt()){ iY ->
                val y = iY.toFloat()
                scope.drawRect(
                    if (m.isInSet(Complex(
                            Converter.xScr2Crt(x, plane),
                            Converter.yScr2Crt(y, plane),
                    ))) Color.Red else Color.White,
                    Offset(x, y),
                    Size(1f, 1f),
                )
            }
        }
    }

    fun paintEnhanced(
        scope: DrawScope,
        maxIterations: Int,
        fractalFunction: (Complex, Int) -> Int,
        colorFunction: (Int, Int) -> Color
    ) {
        renderingJob?.cancel()  // cancel previous rendering job

        val numWorkers = Runtime.getRuntime().availableProcessors()
        val rowsPerWorker = (height / numWorkers).toInt()

        renderingJob = CoroutineScope(Dispatchers.Default).launch {
            val deferredResults = List(numWorkers) { workerIndex ->
                async {
                    renderWorker(
                        workerIndex,
                        rowsPerWorker,
                        maxIterations,
                        fractalFunction,
                        colorFunction
                    )
                }
            }

            deferredResults.awaitAll().forEach { result ->
                withContext(Dispatchers.Main) {
                    drawResults(scope, result)
                }
            }
        }
    }

    private fun renderWorker(
        workerIndex: Int,
        rowsPerWorker: Int,
        maxIterations: Int,
        fractalFunction: (Complex, Int) -> Int,
        colorFunction: (Int, Int) -> Color
    ): List<PixelData> {
        val results = mutableListOf<PixelData>()
        val startY = workerIndex * rowsPerWorker
        val endY = minOf((workerIndex + 1) * rowsPerWorker, height.toInt())

        for (y in startY until endY) {
            for (x in 0 until width.toInt()) {
                val c = Complex(
                Converter.xScr2Crt(x.toFloat(), plane),
                Converter.yScr2Crt(y.toFloat(), plane)
                )
                val iterations = fractalFunction(c, maxIterations)
                val color = colorFunction(iterations, maxIterations)
                results.add(PixelData(x, y, color))
            }
        }

        return results
    }

    private fun drawResults(scope: DrawScope, pixels: List<PixelData>) {
        pixels.forEach { pixel ->
            scope.drawRect(
                pixel.color,
                Offset(pixel.x.toFloat(), pixel.y.toFloat()),
                Size(1f, 1f)
            )
        }
    }
    data class PixelData(val x: Int, val y: Int, val color: Color)

    fun xScr2Crt(x: Float): Double = Converter.xScr2Crt(x, plane)
    fun yScr2Crt(y: Float): Double = Converter.yScr2Crt(y, plane)

}
