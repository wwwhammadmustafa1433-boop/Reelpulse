package com.example.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseOutline
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseSurfaceElevated
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary

enum class AuthScreenStep {
    WELCOME,
    SIGN_IN,
    SIGN_UP
}

data class PendingSignUpProfile(
    val displayName: String,
    val handle: String,
    val mobileNumber: String
)

private val phoneRegex = Regex("^[+]?[0-9\\s\\-()]{7,18}$")

@Composable
fun AuthFlowHost(
    onSignUpPrepared: (PendingSignUpProfile) -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(AuthScreenStep.WELCOME) }

    when (step) {
        AuthScreenStep.WELCOME -> {
            WelcomeAuthScreen(
                onNavigateSignIn = { step = AuthScreenStep.SIGN_IN },
                onNavigateSignUp = { step = AuthScreenStep.SIGN_UP },
                modifier = modifier
            )
        }
        AuthScreenStep.SIGN_IN -> {
            BackHandler { step = AuthScreenStep.WELCOME }
            SignInScreen(
                onBack = { step = AuthScreenStep.WELCOME },
                onSwitchToSignUp = { step = AuthScreenStep.SIGN_UP },
                onAuthSuccess = onAuthSuccess,
                modifier = modifier
            )
        }
        AuthScreenStep.SIGN_UP -> {
            BackHandler { step = AuthScreenStep.WELCOME }
            SignUpScreen(
                onBack = { step = AuthScreenStep.WELCOME },
                onSwitchToSignIn = { step = AuthScreenStep.SIGN_IN },
                onSignUpPrepared = onSignUpPrepared,
                onAuthSuccess = onAuthSuccess,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun WelcomeAuthScreen(
    onNavigateSignIn: () -> Unit,
    onNavigateSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "welcome_pulse")
    val ringScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF130B18),
                        PulseObsidian,
                        Color(0xFF07131A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(PulseCrimson, PulseCyan))
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_reelpulse_fg_1795510698664),
                        contentDescription = "ReelPulse brand mark",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Text(
                    text = "ReelPulse",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = PulseTextPrimary
                )
                Text(
                    text = "Short-form stories tuned to your pulse. Sign in or create your creator profile to sync your personalized feed.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PulseTextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateSignIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseCrimson,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("welcome_sign_in_button")
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onNavigateSignUp,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PulseCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("welcome_sign_up_button")
                ) {
                    Text(
                        text = "Sign Up",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PulseCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun SignInScreen(
    onBack: () -> Unit,
    onSwitchToSignUp: () -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseObsidian)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("sign_in_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Welcome",
                        tint = PulseTextPrimary
                    )
                }
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTextPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = PulseSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PulseOutline, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PulseCrimson.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = PulseCrimson
                            )
                        }
                        Column {
                            Text(
                                text = "Welcome Back",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Continue with your Google account to jump straight into your feed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTextSecondary
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PulseCrimson.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseCrimson,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                    errorMessage = "Sign-in was cancelled. Tap Sign in with Google to try again."
                                }
                            )
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = PulseObsidian
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = PulseObsidian,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PulseSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = PulseCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Once signed in, your session stays active across restarts until you tap Log Out in Profile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTextSecondary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New to ReelPulse?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseTextSecondary
                )
                TextButton(onClick = onSwitchToSignUp) {
                    Text(
                        text = "Sign Up",
                        color = PulseCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SignUpScreen(
    onBack: () -> Unit,
    onSwitchToSignIn: () -> Unit,
    onSignUpPrepared: (PendingSignUpProfile) -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var fullName by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseObsidian)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("sign_up_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Welcome",
                        tint = PulseTextPrimary
                    )
                }
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTextPrimary
                )
            }

            Text(
                text = "Set up your creator identity and save it securely to Firebase.",
                style = MaterialTheme.typography.bodyMedium,
                color = PulseTextSecondary
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    validationError = null
                },
                label = { Text("Full Name / Username") },
                placeholder = { Text("e.g., Alex Rivera") },
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = PulseCyan)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PulseSurfaceElevated,
                    unfocusedContainerColor = PulseSurface,
                    focusedBorderColor = PulseCyan,
                    unfocusedBorderColor = PulseOutline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_name_input")
            )

            OutlinedTextField(
                value = handle,
                onValueChange = {
                    handle = it
                    validationError = null
                },
                label = { Text("Creator Handle (optional)") },
                placeholder = { Text("@alex.creates") },
                leadingIcon = {
                    Icon(Icons.Filled.AlternateEmail, contentDescription = null, tint = PulseTextSecondary)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PulseSurfaceElevated,
                    unfocusedContainerColor = PulseSurface,
                    focusedBorderColor = PulseCyan,
                    unfocusedBorderColor = PulseOutline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_handle_input")
            )

            OutlinedTextField(
                value = mobileNumber,
                onValueChange = {
                    mobileNumber = it
                    validationError = null
                },
                label = { Text("Mobile Number") },
                placeholder = { Text("+1 555 234 5678") },
                leadingIcon = {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = PulseCrimson)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PulseSurfaceElevated,
                    unfocusedContainerColor = PulseSurface,
                    focusedBorderColor = PulseCrimson,
                    unfocusedBorderColor = PulseOutline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_mobile_input")
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PulseSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = PulseCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Authentication uses your Google account via Firebase Auth, so your email is verified automatically with no separate password required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTextSecondary
                    )
                }
            }

            if (validationError != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PulseCrimson.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseCrimson,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val trimmedName = fullName.trim()
                    val trimmedMobile = mobileNumber.trim()
                    val digitsOnly = trimmedMobile.count { it.isDigit() }
                    when {
                        trimmedName.length < 2 -> {
                            validationError = "Please enter your full name or username (at least 2 characters)."
                        }
                        !phoneRegex.matches(trimmedMobile) || digitsOnly < 7 -> {
                            validationError = "Please enter a valid mobile number (at least 7 digits)."
                        }
                        else -> {
                            validationError = null
                            val derivedHandle = handle.trim().ifEmpty {
                                "@" + trimmedName.lowercase().replace(Regex("[^a-z0-9]+"), ".").trim('.')
                            }.let { if (it.startsWith("@")) it else "@$it" }

                            onSignUpPrepared(
                                PendingSignUpProfile(
                                    displayName = trimmedName,
                                    handle = derivedHandle,
                                    mobileNumber = trimmedMobile
                                )
                            )
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    validationError = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                    validationError = "Google Sign-In was cancelled. Tap again to finish creating your account."
                                }
                            )
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PulseCrimson,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_up_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign Up with Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseTextSecondary
                )
                TextButton(onClick = onSwitchToSignIn) {
                    Text(
                        text = "Sign In",
                        color = PulseCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
