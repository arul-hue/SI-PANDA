package com.sudes.purwodadi

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sudes.purwodadi.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isAppReady: Boolean = true, // Bisa dihubungkan ke status siapnya Firebase/Data di MainActivity
    onTimeout: () -> Unit = {}
) {
    val loadingDuration = 2200
    var startAnimation by remember { mutableStateOf(false) }

    // Animasi progres mulus dari 0.0f sampai 1.0f dalam waktu loadingDuration
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = loadingDuration,
            easing = FastOutSlowInEasing
        ),
        label = "SplashLoadingProgress"
    )

    // Memulai animasi dan menunggu hingga animasi 100% + data siap
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(loadingDuration.toLong())
        while (!isAppReady) {
            delay(100)
        }
        onTimeout()
    }

    // Teks status dinamis mengikuti persentase loading
    val statusText = when {
        animatedProgress < 0.35f -> "Menyiapkan aplikasi..."
        animatedProgress < 0.75f -> "Menghubungkan layanan desa..."
        animatedProgress < 0.98f -> "Memuat data pengguna..."
        else -> "Selesai!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenDesaPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 50.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(1.dp))

            // 1. BAGIAN ATAS: Logo & Judul SuDes
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(110.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_sudes),
                            contentDescription = "Logo SuDes",
                            modifier = Modifier.size(140.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "SuDes",
                    style = AppTypography.HeadingLarge.copy(color = Color.White)
                )
            }

            // 2. BAGIAN TENGAH: Indikator Loading Determinate & Teks
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .width(180.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)), // Sudut bar membulat rapi
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = statusText,
                    style = AppTypography.BodyRegular.copy(color = Color.White.copy(alpha = 0.95f))
                )
            }

            // 3. BAGIAN BAWAH: Teks Copyright
            Text(
                text = "Pemerintah Desa Purwodadi Simpang © 2026",
                style = AppTypography.Caption.copy(color = Color.White.copy(alpha = 0.8f))
            )
        }
    }
}