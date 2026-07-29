package com.autoplaygm2.installer

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Tentativa "avançada" do spoof de installer: em vez de chamar
 * PackageManager.setInstallerPackageName() com o privilégio normal do nosso
 * app (que esbarra na checagem de certificado — ver MainActivity), roda o
 * comando "pm set-installer" através do Shizuku, que executa com privilégio
 * de shell do ADB. O shell é uma das exceções que o Android reconhece pra
 * essa checagem, então o comando pode ter sucesso mesmo sem nosso app ser
 * assinado pela Google.
 *
 * Pré-requisito: o app Shizuku instalado e com o serviço rodando no
 * aparelho (feito fora deste app — ver README). Isso só é viável em um
 * aparelho onde você mesmo tem acesso via ADB; não dá pra pedir isso pro
 * seu amigo fazer.
 */
object ShizukuSpoof {

    const val REQUEST_CODE = 9001

    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun requestPermission() {
        Shizuku.requestPermission(REQUEST_CODE)
    }

    /**
     * Roda "pm set-installer <alvo> <installer>" com privilégio de shell.
     * Bloqueante — chamar de uma thread de fundo, não da UI thread.
     */
    @Suppress("RestrictedApi")
    fun setInstallerViaShell(targetPackage: String, installerPackage: String): String {
        return try {
            val cmd = arrayOf("sh", "-c", "pm set-installer $targetPackage $installerPackage")
            val process = Shizuku.newProcess(cmd, null, null)
            val exitCode = process.waitFor()
            val stdout = process.inputStream.bufferedReader().readText().trim()
            val stderr = process.errorStream.bufferedReader().readText().trim()
            "exit=$exitCode" +
                (if (stdout.isNotEmpty()) "\nstdout: $stdout" else "") +
                (if (stderr.isNotEmpty()) "\nstderr: $stderr" else "")
        } catch (e: Throwable) {
            "Erro chamando Shizuku: ${e.javaClass.simpleName}: ${e.message}"
        }
    }
}
