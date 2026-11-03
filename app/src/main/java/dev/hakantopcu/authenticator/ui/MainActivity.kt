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
import dev.hakantopcu.authenticator.store.VaultStore

class MainActivity : FragmentActivity() {
    private var unlocked by mutableStateOf(false)
    private var lockMessage by mutableStateOf<String?>(null)
    private lateinit var state: VaultState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Codes must not show up in screenshots or the recent-apps preview.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        state = VaultState(VaultStore(applicationContext)) { getString(R.string.error_load, it) }
        setContent {
            HakanTheme {
                if (unlocked) AppRoot(state) else LockScreen(lockMessage, onUnlock = ::authenticate)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!unlocked) authenticate()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            unlocked = false
            state.clear()
        }
    }

    private fun authenticate() {
        val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        val status = BiometricManager.from(this).canAuthenticate(allowed)
        if (status == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED || status == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            lockMessage = getString(R.string.no_screen_lock)
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lockMessage = null
                    state.reload()
                    unlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    lockMessage = getString(R.string.unlock_failed, errString)
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle(getString(R.string.unlock_subtitle))
            .setAllowedAuthenticators(allowed)
            .build()
        prompt.authenticate(info)
    }
}
