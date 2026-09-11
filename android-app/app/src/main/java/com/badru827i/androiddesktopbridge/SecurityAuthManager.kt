package com.badru827i.androiddesktopbridge

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import java.util.concurrent.Executor

/**
 * User authentication gate for starting a trusted desktop session.
 *
 * Prefers the Android system biometric UI. Device credential (PIN/pattern/password)
 * is the fallback when the device does not have an eligible biometric or when
 * the user chooses the credential option exposed by the system prompt.
 *
 * The app never reads or stores the user's biometric template, PIN, pattern or
 * password. Android's system authentication service performs the verification.
 */
class SecurityAuthManager(private val context: Context) {
    fun authenticate(
        title: String = "Unlock Android Desktop Bridge",
        subtitle: String = "Authenticate to allow this desktop session",
        onSuccess: () -> Unit,
        onFailure: (CharSequence) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // Basic app can still require the device lock screen before use.
            val keyguard = context.getSystemService(KeyguardManager::class.java)
            if (keyguard?.isDeviceSecure == true) {
                onSuccess()
            } else {
                onFailure("No secure device credential is configured")
            }
            return
        }

        val executor: Executor = context.mainExecutor
        val manager = context.getSystemService(BiometricManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)

        val canBiometric = if (Build.VERSION.SDK_INT >= 30) {
            manager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            manager?.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
        }

        val hasCredential = keyguard?.isDeviceSecure == true

        if (!canBiometric && !hasCredential) {
            onFailure("This device has no supported biometric or PIN/pattern/password")
            return
        }

        val builder = BiometricPrompt.Builder(context)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription("Only continue if you recognize this Android Desktop Bridge session")

        if (Build.VERSION.SDK_INT >= 30) {
            val authenticators = when {
                canBiometric && hasCredential ->
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                canBiometric -> BiometricManager.Authenticators.BIOMETRIC_STRONG
                else -> BiometricManager.Authenticators.DEVICE_CREDENTIAL
            }
            builder.setAllowedAuthenticators(authenticators)
        } else {
            @Suppress("DEPRECATION")
            if (hasCredential) builder.setDeviceCredentialAllowed(true)
            else builder.setNegativeButton("Cancel", executor) { _, _ -> onFailure("Authentication cancelled") }
        }

        val prompt = builder.build()
        prompt.authenticate(executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFailure(errString)
            }

            override fun onAuthenticationFailed() {
                onFailure("Authentication failed")
            }
        })
    }
}
