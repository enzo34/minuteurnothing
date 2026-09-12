package fr.enzo.cachet45

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Tuile des réglages rapides : démarrer depuis le volet déroulant, sans ouvrir l'appli. */
class MinuteurTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        mettreAJour()
    }

    override fun onClick() {
        super.onClick()
        Minuteur.appuiPrincipal(this)
        mettreAJour()
    }

    private fun mettreAJour() {
        val tuile = qsTile ?: return
        when (Minuteur.etat(this)) {
            Etat.REPOS -> {
                tuile.state = Tile.STATE_INACTIVE
                tuile.label = getString(R.string.app_name)
                sousTitre(tuile, getString(R.string.tuile_repos, Reglages.dureeMin(this)))
            }
            Etat.EN_COURS -> {
                tuile.state = Tile.STATE_ACTIVE
                tuile.label = formaterRestant(Minuteur.restantMs(this))
                sousTitre(tuile, getString(R.string.tuile_en_cours))
            }
            Etat.TERMINE -> {
                tuile.state = Tile.STATE_ACTIVE
                tuile.label = getString(R.string.termine)
                sousTitre(tuile, getString(R.string.tuile_termine))
            }
        }
        tuile.updateTile()
    }

    private fun sousTitre(tuile: Tile, texte: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tuile.subtitle = texte
        }
    }

    companion object {

        fun rafraichir(contexte: Context) {
            try {
                TileService.requestListeningState(
                    contexte,
                    ComponentName(contexte, MinuteurTile::class.java)
                )
            } catch (_: IllegalArgumentException) {
                // La tuile n'est pas ajoutée au volet : rien à rafraîchir.
            }
        }

        fun formaterRestant(restantMs: Long): String {
            val totalSecondes = (restantMs + 999) / 1000
            val minutes = totalSecondes / 60
            val secondes = totalSecondes % 60
            return String.format("%02d:%02d", minutes, secondes)
        }
    }
}
