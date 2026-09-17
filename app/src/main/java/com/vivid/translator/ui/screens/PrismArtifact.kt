package com.vivid.translator.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vivid.translator.ui.theme.BoneWhite
import com.vivid.translator.ui.theme.PrismCyan
import com.vivid.translator.ui.theme.PrismLime
import com.vivid.translator.ui.theme.PrismRed
import com.vivid.translator.ui.theme.PureBlack

@Composable
fun PrismArtifact(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(280.dp, 220.dp)) {
        val cubeEdge = 120.dp.toPx()
        val leftTop = Offset((size.width - cubeEdge) / 2f, (size.height - cubeEdge) / 2f)
        drawRect(
            color = PrismRed.copy(alpha = 0.55f),
            topLeft = leftTop + Offset(-14.dp.toPx(), 6.dp.toPx()),
            size = Size(cubeEdge, cubeEdge),
            style = Stroke(width = 2.dp.toPx())
        )
        drawRect(
            color = PrismCyan.copy(alpha = 0.55f),
            topLeft = leftTop + Offset(14.dp.toPx(), -6.dp.toPx()),
            size = Size(cubeEdge, cubeEdge),
            style = Stroke(width = 2.dp.toPx())
        )
        drawRect(
            color = PrismLime.copy(alpha = 0.45f),
            topLeft = leftTop + Offset(0.dp.toPx(), 12.dp.toPx()),
            size = Size(cubeEdge, cubeEdge),
            style = Stroke(width = 2.dp.toPx())
        )
        drawRect(
            color = PureBlack,
            topLeft = leftTop,
            size = Size(cubeEdge, cubeEdge)
        )
        drawRect(
            color = BoneWhite,
            topLeft = leftTop,
            size = Size(cubeEdge, cubeEdge),
            style = Stroke(width = 1.dp.toPx())
        )
        val highlightEdge = 26.dp.toPx()
        drawRect(
            color = BoneWhite,
            topLeft = leftTop + Offset(10.dp.toPx(), 10.dp.toPx()),
            size = Size(highlightEdge, 3.dp.toPx())
        )
        drawRect(
            color = BoneWhite.copy(alpha = 0.7f),
            topLeft = leftTop + Offset(10.dp.toPx(), 10.dp.toPx()),
            size = Size(3.dp.toPx(), highlightEdge)
        )
        val satelliteEdge = 44.dp.toPx()
        val satelliteTopLeft = Offset(leftTop.x + cubeEdge - 14.dp.toPx(), leftTop.y - 34.dp.toPx())
        drawRect(
            color = PrismRed.copy(alpha = 0.6f),
            topLeft = satelliteTopLeft + Offset(-5.dp.toPx(), 0.dp.toPx()),
            size = Size(satelliteEdge, satelliteEdge),
            style = Stroke(width = 2.dp.toPx())
        )
        drawRect(
            color = PrismCyan.copy(alpha = 0.6f),
            topLeft = satelliteTopLeft + Offset(5.dp.toPx(), 0.dp.toPx()),
            size = Size(satelliteEdge, satelliteEdge),
            style = Stroke(width = 2.dp.toPx())
        )
        drawRect(
            color = PureBlack,
            topLeft = satelliteTopLeft,
            size = Size(satelliteEdge, satelliteEdge)
        )
        drawRect(
            color = BoneWhite,
            topLeft = satelliteTopLeft,
            size = Size(satelliteEdge, satelliteEdge),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
