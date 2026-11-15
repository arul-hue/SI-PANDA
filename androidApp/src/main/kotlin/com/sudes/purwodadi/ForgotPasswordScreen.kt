package com.sudes.purwodadi

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sudes.purwodadi.ui.theme.*

@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
    isLoading: Boolean,
    onBackToLoginClicked: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var showErrorValidation by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val errorMessage by authViewModel.errorMessage.collectAsState()

    BackHandler {
        if (showSuccessDialog) {
            showSuccessDialog = false
            authViewModel.clearError()
            onBackToLoginClicked()
        } else if (!isLoading) {
            authViewModel.clearError()
            onBackToLoginClicked()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GreenDesaPrimary,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                authViewModel.clearError()
                                onBackToLoginClicked()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Kembali ke Halaman Masuk",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "Atur Ulang Password",
                                style = AppTypography.HeadingLarge.copy(color = Color.White)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Masukkan email terdaftar untuk reset password",
                                style = AppTypography.BodyRegular.copy(color = Color.White.copy(alpha = 0.9f))
                            )
                        }
                    }
                }
            },
            containerColor = GrayBackground,
            contentWindowInsets = WindowInsets.ime
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // INPUT EMAIL DENGAN VALIDASI MERAH
                            val isEmailError = showErrorValidation && email.isBlank()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Email",
                                    style = AppTypography.LabelForm,
                                    color = if (isEmailError) Color(0xFFDC2626) else TextPrimary
                                )
                                if (isEmailError) {
                                    Text(
                                        text = "Wajib diisi",
                                        style = AppTypography.Caption.copy(color = Color(0xFFDC2626))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    if (errorMessage != null) authViewModel.clearError()
                                },
                                placeholder = {
                                    Text(
                                        "Masukkan email Anda",
                                        color = TextPlaceholder,
                                        style = AppTypography.BodyRegular
                                    )
                                },
                                textStyle = AppTypography.BodyRegular.copy(color = TextPrimary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                isError = isEmailError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    disabledContainerColor = Color.White,
                                    unfocusedBorderColor = if (isEmailError) Color(0xFFDC2626) else GrayBorder,
                                    focusedBorderColor = if (isEmailError) Color(0xFFDC2626) else GreenDesaPrimary,
                                    errorBorderColor = Color(0xFFDC2626),
                                    focusedLabelColor = GreenDesaPrimary,
                                    cursorColor = GreenDesaPrimary
                                )
                            )

                            // KOTAK PERINGATAN WAJIB DIISI / ERROR FIREBASE
                            if (isEmailError) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Peringatan",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Mohon isi alamat email terlebih dahulu.",
                                            color = Color(0xFFDC2626),
                                            style = AppTypography.BodyRegular.copy(
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            } else if (!errorMessage.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Peringatan",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = errorMessage ?: "",
                                            color = MaterialTheme.colorScheme.error,
                                            style = AppTypography.BodyRegular.copy(
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (email.isBlank()) {
                                        showErrorValidation = true
                                    } else {
                                        showErrorValidation = false
                                        authViewModel.sendPasswordResetEmail(email.trim()) {
                                            showSuccessDialog = true
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenDesaPrimary)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Reset Password",
                                        style = AppTypography.ButtonText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // POPUP SUKSES (Menutupi layar penuh & memblokir klik latar belakang)
        if (showSuccessDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {},
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFF0FDF4),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(68.dp),
                            shape = CircleShape,
                            color = GreenDesaLight
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email Sent",
                                    tint = GreenDesaPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Periksa Email Anda",
                            style = AppTypography.HeadingLarge,
                            color = GreenDesaPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Tautan reset password telah dikirim ke",
                            style = AppTypography.BodyRegular,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = email.ifBlank { "email@desa.id" },
                            style = AppTypography.HeadingMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Silakan cek kotak masuk atau folder spam Anda.",
                            style = AppTypography.Caption,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                showSuccessDialog = false
                                authViewModel.clearError()
                                onBackToLoginClicked()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenDesaPrimary)
                        ) {
                            Text(
                                text = "Kembali ke Halaman Masuk",
                                style = AppTypography.ButtonText
                            )
                        }
                    }
                }
            }
        }
    }
}