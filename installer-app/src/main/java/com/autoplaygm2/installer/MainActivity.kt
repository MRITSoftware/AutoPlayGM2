package com.autoplaygm2.installer

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * App de estudo do AutoPlayGM2: reproduz, de forma simplificada e só pra uso
 * pessoal, o truque que o AAAD usa pra fazer apps sideloaded aparecerem no
 * Android Auto — instalar o APK e depois reescrever o installerPackageName
 * dele pra "com.android.vending" (Play Store).
 *
 * Fluxo de um toque só: o APK de teste (test-auto-app) vem embutido dentro
 * dos assets/ deste app (colocado ali pelo workflow de CI antes do build).
 * Isso garante que ESTE app sempre vai ser o "installer de registro" do
 * pacote-alvo, que é o pré-requisito pra reescrever o installerPackageName
 * com sucesso.
 */
class MainActivity : AppCompatActivity() {

    private val tag = "AutoPlayGM2"

    // Precisa bater com o applicationId do módulo test-auto-app.
    private val targetPackage = "com.autoplaygm2.testautoapp"

    // Nome do arquivo dentro de installer-app/src/main/assets/
    private val embeddedApkAsset = "test_auto_app.apk"

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.installButton).setOnClickListener {
            installEmbeddedApk()
        }

        findViewById<Button>(R.id.checkInstallerButton).setOnClickListener {
            checkInstaller()
        }
    }

    private fun installEmbeddedApk() {
        statusText.text = "Instalando..."
        ApkInstaller.installFromAssets(this, embeddedApkAsset) { success, message ->
            runOnUiThread {
                statusText.text = message
                Log.d(tag, message)
                if (success) {
                    spoofInstaller()
                }
            }
        }
    }

    private fun spoofInstaller() {
        try {
            // Só funciona porque ESTE app é o installer de registro do
            // targetPackage (ver comentário da classe). É a mesma API pública
            // que apps "atualizadores" usam legitimamente pra se anunciar
            // como responsáveis por um pacote.
            packageManager.setInstallerPackageName(targetPackage, "com.android.vending")
            runOnUiThread {
                statusText.append("\n\nPronto! Agora conecta no Android Auto do carro e vê se \"AutoPlayGM2 Test\" apareceu na lista de apps de mídia. ✅")
            }
        } catch (e: Exception) {
            Log.e(tag, "Falha ao spoofar installer", e)
            runOnUiThread {
                statusText.append("\n\nDeu erro nessa parte: ${e.message}")
            }
        }
    }

    private fun checkInstaller() {
        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(targetPackage).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(targetPackage)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        statusText.text = if (installer == "com.android.vending") {
            "Tudo certo! O celular acha que este app veio da Play Store. ✅"
        } else {
            "Ainda não. Installer atual: ${installer ?: "nenhum (app não instalado ainda)"}"
        }
    }
}
