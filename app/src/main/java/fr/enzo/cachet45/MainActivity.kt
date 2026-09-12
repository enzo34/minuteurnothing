package fr.enzo.cachet45

import android.Manifest
import android.app.Activity
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Date

class MainActivity : Activity() {

    private lateinit var boutonPrincipal: View
    private lateinit var matrice: VueMatrice
    private lateinit var sousMatrice: TextView
    private lateinit var consigne: TextView
    private lateinit var boutonSecondaire: TextView
    private lateinit var titreDuree: TextView
    private lateinit var ligneDurees: LinearLayout
    private lateinit var dernierePrise: TextView
    private lateinit var boutonTuile: TextView

    private val boucle = Handler(Looper.getMainLooper())
    private val rafraichissement = object : Runnable {
        override fun run() {
            majInterface()
            boucle.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(etatSauvegarde: Bundle?) {
        super.onCreate(etatSauvegarde)
        setContentView(R.layout.activity_main)

        boutonPrincipal = findViewById<View>(R.id.bouton_principal)!!
        matrice = findViewById<VueMatrice>(R.id.matrice)!!
        sousMatrice = findViewById<TextView>(R.id.sous_matrice)!!
        consigne = findViewById<TextView>(R.id.consigne)!!
        boutonSecondaire = findViewById<TextView>(R.id.bouton_secondaire)!!
        titreDuree = findViewById<TextView>(R.id.titre_duree)!!
        ligneDurees = findViewById<LinearLayout>(R.id.ligne_durees)!!
        dernierePrise = findViewById<TextView>(R.id.derniere_prise)!!
        boutonTuile = findViewById<TextView>(R.id.bouton_tuile)!!

        boutonPrincipal.setOnClickListener {
            Minuteur.appuiPrincipal(this)
            majInterface()
        }

        boutonSecondaire.setOnClickListener {
            when (Minuteur.etat(this)) {
                Etat.EN_COURS -> Minuteur.annuler(this)
                Etat.TERMINE -> Minuteur.acquitter(this)
                Etat.REPOS -> Unit
            }
            majInterface()
        }

        findViewById<View>(R.id.bouton_widget)!!.setOnClickListener { proposerWidget() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boutonTuile.visibility = View.VISIBLE
            boutonTuile.setOnClickListener { proposerTuile() }
        }

        construireDurees()
        demanderNotifications()
    }

    override fun onResume() {
        super.onResume()
        majInterface()
        boucle.postDelayed(rafraichissement, 1_000L)
    }

    override fun onPause() {
        super.onPause()
        boucle.removeCallbacks(rafraichissement)
    }

    // --- Interface -----------------------------------------------------------

    private fun majInterface() {
        val blanc = ContextCompat.getColor(this, R.color.blanc)
        val vert = ContextCompat.getColor(this, R.color.vert)

        when (Minuteur.etat(this)) {
            Etat.REPOS -> {
                afficherMatrice(Reglages.dureeMin(this).toString(), blanc)
                sousMatrice.text = getString(R.string.demarrer)
                sousMatrice.setTextColor(blanc)
                consigne.text = getString(R.string.consigne_repos)
                boutonSecondaire.visibility = View.GONE
                titreDuree.visibility = View.VISIBLE
                ligneDurees.visibility = View.VISIBLE
            }

            Etat.EN_COURS -> {
                afficherMatrice(compteARebours(Minuteur.restantMs(this)), blanc)
                sousMatrice.text = getString(R.string.restantes)
                sousMatrice.setTextColor(ContextCompat.getColor(this, R.color.rouge))
                consigne.text = getString(
                    R.string.consigne_en_cours_heure,
                    MinuteurWidget.heureDeFin(this)
                )
                boutonSecondaire.visibility = View.VISIBLE
                boutonSecondaire.text = getString(R.string.annuler)
                titreDuree.visibility = View.GONE
                ligneDurees.visibility = View.GONE
            }

            Etat.TERMINE -> {
                afficherMatrice("OK", vert)
                sousMatrice.text = getString(R.string.termine)
                sousMatrice.setTextColor(vert)
                consigne.text = getString(R.string.consigne_termine)
                boutonSecondaire.visibility = View.VISIBLE
                boutonSecondaire.text = getString(R.string.ok)
                titreDuree.visibility = View.GONE
                ligneDurees.visibility = View.GONE
            }
        }

        boutonPrincipal.contentDescription = "${sousMatrice.text} — ${matrice.texte}"
        dernierePrise.text = getString(R.string.derniere_prise, texteDernierePrise())
        majDurees()
    }

    /** Adapte la taille des points pour que le texte remplisse le cercle. */
    private fun afficherMatrice(texte: String, couleur: Int) {
        val colonnes = Matrice.colonnes(texte).coerceAtLeast(1)
        matrice.pasDp = (LARGEUR_UTILE_DP / colonnes).coerceAtMost(PAS_MAX_DP)
        matrice.texte = texte
        matrice.couleur = couleur
    }

    private fun compteARebours(restantMs: Long): String {
        val totalSecondes = (restantMs + 999L) / 1000L
        return String.format("%02d:%02d", totalSecondes / 60, totalSecondes % 60)
    }

    // --- Durées --------------------------------------------------------------

    private fun construireDurees() {
        ligneDurees.removeAllViews()
        val densite = resources.displayMetrics.density
        Reglages.DUREES_PROPOSEES.forEach { minutes ->
            val puce = TextView(this).apply {
                text = getString(R.string.duree_min, minutes)
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(
                    (22 * densite).toInt(),
                    (11 * densite).toInt(),
                    (22 * densite).toInt(),
                    (11 * densite).toInt()
                )
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    Reglages.definirDureeMin(this@MainActivity, minutes)
                    MinuteurWidget.rafraichir(this@MainActivity)
                    MinuteurTile.rafraichir(this@MainActivity)
                    majInterface()
                }
            }
            val parametres = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (ligneDurees.childCount > 0) parametres.marginStart = (10 * densite).toInt()
            ligneDurees.addView(puce, parametres)
        }
        majDurees()
    }

    private fun majDurees() {
        val choisie = Reglages.dureeMin(this)
        Reglages.DUREES_PROPOSEES.forEachIndexed { index, minutes ->
            val puce = ligneDurees.getChildAt(index) as? TextView ?: return@forEachIndexed
            val active = minutes == choisie
            puce.setBackgroundResource(
                if (active) R.drawable.puce_active else R.drawable.puce_inactive
            )
            puce.setTextColor(
                ContextCompat.getColor(this, if (active) R.color.noir else R.color.gris)
            )
            puce.isSelected = active
        }
    }

    // --- Dernière prise ------------------------------------------------------

    private fun texteDernierePrise(): String {
        val horodatage = Reglages.dernierePrise(this)
        if (horodatage == 0L) return getString(R.string.jamais)

        val heure = DateFormat.getTimeFormat(this).format(Date(horodatage))
        val jourPrise = Calendar.getInstance().apply { timeInMillis = horodatage }
        val aujourdhui = Calendar.getInstance()

        val memeAnnee = jourPrise.get(Calendar.YEAR) == aujourdhui.get(Calendar.YEAR)
        val ecartJours = jourPrise.get(Calendar.DAY_OF_YEAR) - aujourdhui.get(Calendar.DAY_OF_YEAR)

        return when {
            memeAnnee && ecartJours == 0 -> getString(R.string.aujourdhui, heure)
            memeAnnee && ecartJours == -1 -> getString(R.string.hier, heure)
            else -> getString(
                R.string.le_jour,
                DateFormat.getDateFormat(this).format(Date(horodatage)),
                heure
            )
        }
    }

    // --- Ajouts système ------------------------------------------------------

    private fun proposerWidget() {
        val gestionnaire = getSystemService(AppWidgetManager::class.java)
        val fournisseur = ComponentName(this, MinuteurWidget::class.java)
        val possible = gestionnaire != null && gestionnaire.isRequestPinAppWidgetSupported
        if (possible && gestionnaire!!.requestPinAppWidget(fournisseur, null, null)) return
        Toast.makeText(this, R.string.widget_deja_present, Toast.LENGTH_LONG).show()
    }

    private fun proposerTuile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val gestionnaire = getSystemService(StatusBarManager::class.java) ?: return
        gestionnaire.requestAddTileService(
            ComponentName(this, MinuteurTile::class.java),
            getString(R.string.app_name),
            Icon.createWithResource(this, R.drawable.ic_pilule),
            mainExecutor
        ) { _ -> }
    }

    private fun demanderNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val accordee = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!accordee) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    private companion object {
        /** Largeur disponible pour les points à l'intérieur du cercle, en dp. */
        const val LARGEUR_UTILE_DP = 182f
        const val PAS_MAX_DP = 14f
    }
}
