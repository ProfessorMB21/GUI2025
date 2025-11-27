package painting

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import painting.conversion.Converter
import painting.conversion.Plane
import java.math.MathContext
import androidx.compose.ui.geometry.Offset

class CartesianPainter(
    val plane: Plane
) {

    var axesColor: Color = Color.Black

    fun draw(scope: DrawScope){
        drawAxes(scope)
        drawXTicks(scope)
        drawYTicks(scope)
    }

    private fun drawXTicks(scope: DrawScope) {
        var xDot = plane.xMin
        while (xDot <= plane.xMax){
            val xPos = Converter.xCrt2Scr(xDot, plane)
            drawXTick(scope, xPos, when {
                (xDot * 10).toInt() % 10 == 0 -> TickType.MAX
                (xDot * 10).toInt() % 5 == 0 -> TickType.MID
                else -> TickType.MIN
            })
            xDot = (xDot + 0.1).toBigDecimal().round(MathContext.DECIMAL32).setScale(1).toDouble()
        }

    }

    private fun drawXTick(scope: DrawScope, pos: Float, type: TickType) {
        Converter.yCrt2Scr(.0, plane).let { zero ->
            scope.drawLine(
                type.color,
                Offset(pos, zero+type.halfSize),
                Offset(pos, zero-type.halfSize)
            )
        }
    }


    private fun drawYTicks(scope: DrawScope) {
        var yDot = plane.yMin
        while (yDot <= plane.yMax){
            val yPos = Converter.yCrt2Scr(yDot, plane)
            drawYTick(scope, yPos, when {
                (yDot * 10).toInt() % 10 == 0 -> TickType.MAX
                (yDot * 10).toInt() % 5 == 0 -> TickType.MID
                else -> TickType.MIN
            })
            yDot = (yDot + 0.1).toBigDecimal().round(MathContext.DECIMAL32).setScale(1).toDouble()
        }
    }

    private fun drawYTick(scope: DrawScope, pos: Float, type: TickType) {
        Converter.xCrt2Scr(.0, plane).let { zero ->
            scope.drawLine(
                type.color,
                Offset(zero+type.halfSize, pos),
                Offset(zero-type.halfSize, pos)
            )
        }
    }

    private fun drawXLabel(scope: DrawScope, value: String){

    }

    private fun drawYLabel(scope: DrawScope, value: String){

    }

    private fun drawAxes(scope: DrawScope) {
        Converter.yCrt2Scr(.0, plane).let { zero ->
            scope.drawLine(
                axesColor,
                Offset(0f, zero),
                Offset(plane.width, zero)
            )
        }
        Converter.xCrt2Scr(.0, plane).let { zero ->
            scope.drawLine(
                axesColor,
                Offset(zero, 0f),
                Offset(zero, plane.height)
            )
        }
    }


}

enum class TickType(var color: Color, var halfSize: Float){
    MIN(Color.Black, 3f),
    MID(Color.Blue, 5f),
    MAX(Color.Red, 8f)
}
