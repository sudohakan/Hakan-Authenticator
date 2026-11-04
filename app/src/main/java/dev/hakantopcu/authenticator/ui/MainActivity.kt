package dev.hakantopcu.authenticator.ui

import dev.hakantopcu.authenticator.R
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import dev.hakantopcu.authenticator.store.VaultStore

class MainActivity : FragmentActivity() {
    private var unlocked by mutableStateOf(false)
    private var lockMessage by mutableStateOf<String?>(null)
    private lateinit var state: VaultState
    private lateinit var prompt: BiometricPrompt
    private var authInProgress = false
    // The prompt opens by itself once per return to the foreground; after a cancel only the button opens it,
    // because the prompt itself pauses and resumes the activity and would otherwise reopen in a loop.
    private var autoPromptPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Codes must not show up in screenshots or the recent-apps preview.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        state = VaultState(VaultStore(applicationContext)) { getString(R.string.error_load, it) }
        // BiometricPrompt must be built once, before the activity starts; one built later loses its callback
        // after the app returns from the background and the unlock button stops doing anything.
        prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback)
        setContent {
            HakanTheme {
                if (unlocked) AppRoot(state) else LockScreen(lockMessage, onUnlock = ::authenticate)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        autoPromptPending = !unlocked
    }

    override fun onResume() {
        super.onResume()
        if (autoPromptPending) {
            autoPromptPending = false
            authenticate()
        }
    }

    override fun onStop() {
        super.onStop()
        // No cancel here: the device PIN screen is a separate activity and stops this one mid-authentication.
        if (!isChangingConfigurations) {
            unlocked = false
            state.clear()
        }
    }

    private val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            authInProgress = false
            lockMessage = null
            state.reload()
            unlocked = true
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            authInProgress = false
            // Dismissing the prompt or leaving the app is not an error worth a message; the button stays.
            lockMessage = if (errorCode in SILENT_ERRORS) null else getString(R.string.unlock_failed, errString)
        }
    }

    private fun authenticate() {
        if (authInProgress || unlocked) return
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        val status = BiometricManager.from(this).canAuthenticate(allowed)
        if (status == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED || status == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            lockMessage = getString(R.string.no_screen_lock)
            return
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle(getString(R.string.unlock_subtitle))
            .setAllowedAuthenticators(allowed)
            .build()
        authInProgress = true
        lockMessage = null
        prompt.authenticate(info)
    }

    private companion object {
        val SILENT_ERRORS = setOf(
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_CANCELED,
        )
    }
}
