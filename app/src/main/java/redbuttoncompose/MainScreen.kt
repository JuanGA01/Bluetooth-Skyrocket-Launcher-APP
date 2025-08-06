package redbuttoncompose

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redbutton.redbuttoncompose.R

@Composable
fun MainScreen(viewModel: CountdownViewModel) {
    val isCounting by viewModel.isCounting
    val count by viewModel.currentCount

    val lcdFont = FontFamily(
        Font(R.font.lcd_font, weight = FontWeight.Normal)
    )

    val alpha by animateFloatAsState(
        targetValue = if (isCounting) 1f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LCDAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .background(Color(0xFF6B8E23), shape = RoundedCornerShape(12.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.lcdDisplay.value,
                    fontSize = 64.sp,
                    fontFamily = lcdFont,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(alpha),
                    style = TextStyle(
                        letterSpacing = 4.sp,
                        shadow = Shadow(
                            color = Color(0x66000000),
                            blurRadius = 4f
                        )
                    )
                )
            }

            // 👇 AQUI el espaciado REAL entre LCD y botón iniciar
            Spacer(modifier = Modifier.height(48.dp))

            FancyButton(
                text = "INICIAR",
                onClick = { viewModel.startCountdown() },
                size = 200.dp,
                colorDefault = Color(0xFFE53935),
                colorPressed = Color(0xFFB71C1C)
            )

            Spacer(modifier = Modifier.height(32.dp))


            Box(modifier = Modifier.height(120.dp)) {
                if (isCounting) {
                    FancyButton(
                        text = "CANCELAR",
                        onClick = { viewModel.cancelCountdown() },
                        size = 120.dp,
                        colorDefault = Color(0xFFBDBDBD),
                        colorPressed = Color(0xFF757575)
                    )
                }
            }

        }
    }
}



@Composable
fun FancyButton(
    text: String,
    onClick: () -> Unit,
    size: Dp,
    colorDefault: Color,
    colorPressed: Color
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    val shadow by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 12.dp,
        animationSpec = tween(durationMillis = 100),
        label = "elevation"
    )

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            if (isPressed) colorPressed else colorDefault,
            if (isPressed) colorPressed else colorDefault
        )
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .shadow(elevation = shadow, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(brush = gradientBrush)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = (size.value * 0.16).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

