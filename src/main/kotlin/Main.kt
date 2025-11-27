import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key.Companion.Menu
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.PaintPanel
import viewmodels.MainViewModel

@Composable
@Preview
fun App(viewModel: MainViewModel = MainViewModel()) {
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            ControlPanel(viewModel)
            Divider(color = Color.Gray, thickness = 1.dp)
            PaintPanel(Modifier.fillMaxSize())
            {
                viewModel.paint(it)
            }
        }
    }
}

fun main(): Unit = application {
    val viewModel = MainViewModel()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Fractals 2025"
    ) {
        // Menu bar
        MenuBar {
            Menu("File") {
                Item("Save as JPG", onClick = {  })
                Item("Save Custom", onClick = {  })
                Item("Load Custom", onClick = {  })
                Separator()
                Item("Exit", onClick = ::exitApplication)
            }
            Menu("View") {
                Item("Reset View", onClick = { viewModel.resetView() })
                Item("Zoom In", onClick = { viewModel.zoomIn() })
                Item("Zoom Out", onClick = { viewModel.zoomOut() })
            }
            Menu("Fractal") {
                Item("Mandelbrot Set", onClick = { viewModel.setFractalType("mandelbrot") })
                Item("Julia Set", onClick = {  })
            }
            Menu("Tour") {
                Item("Start Fractal Tour", onClick = { viewModel.startFractalTour() })
                Item("Add Keyframe", onClick = { viewModel.addTourKeyframe() })
            }
        }

        App(viewModel)
    }

    // Julia set window
    if (viewModel.showJuliaWindow) {
        Window(
            onCloseRequest = { viewModel.showJuliaWindow = false },
            title = "Julia Set"
        ) {
            JuliaSetApp(viewModel)
        }
    }
}


@Composable
    fun ControlPanel(viewModel: MainViewModel) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(MaterialTheme.colors.primarySurface),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { viewModel.zoomIn() }) {
                Text("Zoom In")
            }
            Button(onClick = { viewModel.zoomOut() }) {
                Text("Zoom Out")
            }
            Button(onClick = { viewModel.resetView() }) {
                Text("Reset")
            }
            Button(onClick = { }) {
                Text("Save JPG")
            }
            Button(onClick = { viewModel.showJuliaWindow }) {
                Text("Julia Set")
            }
            Button(onClick = { viewModel.startFractalTour() }) {
                Text("Start Tour")
            }
        }
    }

@Composable
fun JuliaSetApp(viewModel: MainViewModel) {
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Julia Set for point: ${viewModel.juliaPoint}",
                modifier = Modifier.padding(8.dp)
            )
            PaintPanel(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                viewModel.paintJuliaSet(it)
            }
        }
    }
}
