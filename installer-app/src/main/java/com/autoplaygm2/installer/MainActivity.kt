package com.autoplaygm2.installer

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

/**
 * App de estudo do AutoPlayGM2.
 *
 * Histórico: a ideia original era reescrever o installerPackageName do
 * pacote-alvo pra "com.android.vending" via PackageManager.setInstallerPackageName()
 * (o truque clássico do AAAD). NA PRÁTICA, em qualquer aparelho com a Play
 * Store de verdade instalada, o Android barra isso — o sistema exige que
 * quem alega ser "com.android.vending" tenha o MESMO certificado de
 * assinatura da Google, o que só a própria Google tem. É uma trava de
 * segurança deliberada, não um bug nosso (erro típico:
 * "Caller does not have same cert as new installer package").
 *
 * O caminho que realmente funciona hoje é bem mais simples: o próprio
 * Android Auto tem um "modo desenvolvedor" oculto com um toggle
 * "Fontes desconhecidas" que libera apps de terceiros compatíveis, sem
 * spoof nenhum. Este app instala o APK de teste e guia pra essa
 * configuração.
 *
 * Os botões "[Debug]" e "[Avançado]" existem só pra quem quiser
 * confirmar/explorar o comportamento do spoof — não fazem parte do fluxo
 * normal recomendado (e o [Avançado] exige Shizuku configurado, então só
 * faz sentido no seu próprio aparelho, não no de quem só vai testar o app).
 */
class MainActivity : AppCompatActivity() {

    private val tag = "AutoPlayGM2"

    // Precisa bater com o applicationId do módulo test-auto-app.
    private val targetPackage = "com.autoplaygm2.testautoapp"

    // Nome do arquivo dentro de installer-app/src/main/assets/
    private val embeddedApkAsset = "test_auto_app.apk"

    // Pacote oficial do app Android Auto no celular.
    private val androidAutoPackage = "com.google.android.projection.gearhead"

    private lateinit var statusText: TextView

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == ShizukuSpoof.REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    runShizukuSpoof()
                } else {
                    runOnUiThread {
                        statusText.text = "[Shizuku] Permissão negada."
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.installButton).setOnClickListener {
            installEmbeddedApk()
        }

        findViewById<Button>(R.id.checkInstallerButton).setOnClickListener {
            openAndroidAuto()
        }

        findViewById<Button>(R.id.debugSpoofButton).setOnClickListener {
            debugTrySpoofAndCheck()
        }

        findViewById<Button>(R.id.shizukuSpoofButton).setOnClickListener {
            debugShizukuSpoof()
        }

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        super.onDestroy()
    }

    private fun installEmbeddedApk() {
        statusText.text = "Instalando..."
        ApkInstaller.installFromAssets(this, embeddedApkAsset) { success, message ->
            runOnUiThread {
                Log.d(tag, message)
                if (success) {
                    showNextSteps()
                } else {
                    statusText.text = message
                }
            }
        }
    }

    private fun showNextSteps() {
        statusText.text = "Instalado! ✅\n\n" +
            "Agora, no app Android Auto do celular:\n" +
            "1. Abra Configurações > Sobre.\n" +
            "2. Toque 10x seguidas na versão do app até liberar o \"Modo desenvolvedor\".\n" +
            "3. Volte, entre em \"Configurações de desenvolvedor\" e ative \"Fontes desconhecidas\".\n" +
            "4. Conecte no carro e veja se \"AutoPlayGM2 Test\" aparece na lista de apps de mídia.\n\n" +
            "Toque no botão abaixo pra abrir o Android Auto direto."
    }

    private fun openAndroidAuto() {
        val intent = packageManager.getLaunchIntentForPackage(androidAutoPackage)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Não encontrei o app Android Auto instalado neste celular", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Tenta o spoof clássico (privilégio normal do app) e mostra exatamente
     * o que o sistema responde — sucesso, ou o erro (geralmente
     * SecurityException por causa da checagem de certificado).
     */
    private fun debugTrySpoofAndCheck() {
        val before = readInstaller()

        val spoofResult = try {
            packageManager.setInstallerPackageName(targetPackage, "com.android.vending")
            "Chamada aceita sem exceção."
        } catch (e: Exception) {
            Log.e(tag, "Spoof falhou", e)
            "${e.javaClass.simpleName}: ${e.message}"
        }

        val after = readInstaller()

        statusText.text = "[Debug]\n" +
            "Installer antes da tentativa: ${before ?: "(nenhum)"}\n" +
            "Resultado da chamada: $spoofResult\n" +
            "Installer depois da tentativa: ${after ?: "(nenhum)"}\n\n" +
            if (after == "com.android.vending") {
                "Spoof funcionou neste aparelho! ✅"
            } else {
                "Spoof não pegou neste aparelho (esperado se ele tiver a Play Store genuína)."
            }
    }

    /**
     * Tenta o spoof via Shizuku (privilégio de shell). Exige que o app
     * Shizuku esteja instalado e com o serviço rodando (ver README).
     */
    private fun debugShizukuSpoof() {
        if (!ShizukuSpoof.isAvailable()) {
            statusText.text = "[Shizuku] Não encontrei o serviço do Shizuku rodando neste " +
                "aparelho. Instale o app Shizuku e inicie o serviço antes (veja o README, " +
                "seção Shizuku)."
            return
        }
        if (!ShizukuSpoof.hasPermission()) {
            statusText.text = "[Shizuku] Pedindo permissão..."
            ShizukuSpoof.requestPermission()
            return
        }
        runShizukuSpoof()
    }

    private fun runShizukuSpoof() {
        statusText.text = "[Shizuku] Rodando..."
        Thread {
            val before = readInstaller()
            val result = ShizukuSpoof.setInstallerViaShell(targetPackage, "com.android.vending")
            val after = readInstaller()
            runOnUiThread {
                statusText.text = "[Shizuku]\n" +
                    "Installer antes: ${before ?: "(nenhum)"}\n" +
                    "$result\n" +
                    "Installer depois: ${after ?: "(nenhum)"}\n\n" +
                    if (after == "com.android.vending") {
                        "Funcionou de verdade! ✅"
                    } else {
                        "Ainda não bateu."
                    }
            }
        }.start()
    }

    private fun readInstaller(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(targetPackage).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(targetPackage)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
