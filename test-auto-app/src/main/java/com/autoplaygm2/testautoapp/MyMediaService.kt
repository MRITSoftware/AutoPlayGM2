package com.autoplaygm2.testautoapp

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat

/**
 * Serviço mínimo de "media browser" — o suficiente pra Android Auto
 * reconhecer este app como um app de mídia válido. Não toca nada de
 * verdade, é só uma cobaia pra testar se o spoof do installer (feito
 * pelo installer-app) faz este app aparecer na lista do carro.
 */
class MyMediaService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "AutoPlayGM2Test")
        sessionToken = mediaSession.sessionToken
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaItem>>) {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId("dummy_track_1")
            .setTitle("Faixa de teste do AutoPlayGM2")
            .setSubtitle("Se você está vendo isso no carro, o spoof funcionou")
            .build()

        result.sendResult(mutableListOf(MediaItem(description, MediaItem.FLAG_PLAYABLE)))
    }

    override fun onDestroy() {
        mediaSession.release()
        super.onDestroy()
    }
}
