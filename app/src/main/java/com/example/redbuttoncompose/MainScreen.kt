package com.example.redbuttoncompose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(viewModel: CountdownViewModel) {
    val isCounting by viewModel.isCounting
    val count by viewModel.currentCount

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            if (isCounting) {
                Text(
                    text = count.toString(),
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            Button(
                onClick = { viewModel.startCountdown() },
                shape = CircleShape,
                modifier = Modifier
                    .size(200.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("INICIAR", color = Color.White, fontSize = 40.sp)
            }

            if (isCounting) {
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { viewModel.cancelCountdown() }) {
                    Text("Cancelar")
                }
            }
        }
    }
}
