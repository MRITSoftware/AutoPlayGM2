package com.autoplaygm2.installer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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
 */
class MainActivity : AppCompatActivity() {

    private val tag = "AutoPlayGM2"

    // Nome do arquivo dentro de installer-app/src/main/assets/
    private val embeddedApkAsset = "test_auto_app.apk"

    // Pacote oficial do app Android Auto no celular.
    private val androidAutoPackage = "com.google.android.projection.gearhead"

    private lateinit var statusText: TextView

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
}
