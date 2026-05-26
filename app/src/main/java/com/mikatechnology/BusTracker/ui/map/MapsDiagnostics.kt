package com.mikatechnology.BusTracker.ui.map

import android.content.Context
import android.content.pm.PackageManager
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

fun appSigningSha1(context: Context): String? {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        val signature = packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()
            ?: return null

        val factory = CertificateFactory.getInstance("X509")
        val certificate = factory.generateCertificate(ByteArrayInputStream(signature.toByteArray()))
        val x509 = certificate as X509Certificate

        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(x509.encoded)
        digest.digest().joinToString(":") { byte ->
            "%02X".format(byte)
        }
    } catch (_: Exception) {
        null
    }
}

/** Google Cloud bazen iki noktasız format ister. */
fun formatSha1ForConsole(sha1WithColons: String): String {
    return sha1WithColons.replace(":", "").uppercase()
}
