package com.autoplaygm2.installer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import java.io.IOException
import java.io.InputStream

/**
 * Instala um APK usando a PackageInstaller.Session API (não precisa de root,
 * só da permissão REQUEST_INSTALL_PACKAGES). Quem chama esta função vira o
 * "installer de registro" do pacote instalado — é esse status que depois
 * permite reescrever o installerPackageName em MainActivity.spoofInstaller().
 */
object ApkInstaller {

    private const val TAG = "ApkInstaller"
    private const val ACTION_INSTALL_COMPLETE = "com.autoplaygm2.installer.INSTALL_COMPLETE"

    /** Instala o APK embutido nos assets do próprio installer-app (fluxo padrão, um toque só). */
    fun installFromAssets(context: Context, assetName: String, callback: (success: Boolean, message: String) -> Unit) {
        install(context, { context.assets.open(assetName) }, callback)
    }

    private fun install(context: Context, openStream: () -> InputStream, callback: (success: Boolean, message: String) -> Unit) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

        val sessionId = try {
            packageInstaller.createSession(params)
        } catch (e: IOException) {
            callback(false, "Erro criando sessão de instalação: ${e.message}")
            return
        }

        val session = try {
            packageInstaller.openSession(sessionId)
        } catch (e: IOException) {
            callback(false, "Erro abrindo sessão de instalação: ${e.message}")
            return
        }

        try {
            openStream().use { input ->
                session.openWrite("package", 0, -1).use { out ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }
        } catch (e: IOException) {
            callback(false, "Erro copiando o APK pra sessão: ${e.message}")
            session.abandon()
            return
        }

        // Recebe o resultado da instalação via broadcast explícito (só o nosso
        // próprio app pode enviar/receber, por isso RECEIVER_NOT_EXPORTED).
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""

                when (status) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        // O sistema precisa mostrar o diálogo clássico de "Instalar este app?"
                        @Suppress("DEPRECATION")
                        val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                        confirmIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        confirmIntent?.let { context.startActivity(it) }
                    }
                    PackageInstaller.STATUS_SUCCESS -> {
                        Log.d(TAG, "Instalação concluída com sucesso")
                        callback(true, "Instalação concluída com sucesso")
                        context.unregisterReceiver(this)
                    }
                    else -> {
                        Log.e(TAG, "Falha na instalação (status=$status): $message")
                        callback(false, "Falha na instalação (status=$status): $message")
                        context.unregisterReceiver(this)
                    }
                }
            }
        }

        val filter = IntentFilter(ACTION_INSTALL_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }

        val intent = Intent(ACTION_INSTALL_COMPLETE).setPackage(context.packageName)
        val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, piFlags)

        session.commit(pendingIntent.intentSender)
        session.close()
    }
}
