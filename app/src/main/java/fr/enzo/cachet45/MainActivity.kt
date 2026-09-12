package fr.enzo.cachet45

import android.Manifest
import android.app.Activity
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var boutonPrincipal: View
    private lateinit var matrice: VueMatrice
    private lateinit var sousMatrice: TextView
    private lateinit var consigne: TextView
    private lateinit var boutonSecondaire: TextView
    private lateinit var titreDuree: TextView
    private lateinit var ligneDurees: LinearLayout
    private lateinit var dernierePrise: TextView
    private lateinit var sonnerieActuelle: TextView
    private lateinit var boutonEcouter: TextView
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
        sonnerieActuelle = findViewById<TextView>(R.id.sonnerie_actuelle)!!
        boutonEcouter = findViewById<TextView>(R.id.bouton_sonnerie_ecouter)!!
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

        findViewById<View>(R.id.carte_derniere_prise)!!.setOnClickListener {
            startActivity(Intent(this, HistoriqueActivity::class.java))
        }

        findViewById<View>(R.id.bouton_sonnerie_systeme)!!.setOnClickListener {
            ouvrir(Sonneries.intentionSelecteur(this), DEMANDE_SONNERIE)
        }

        findViewById<View>(R.id.bouton_sonnerie_fichier)!!.setOnClickListener {
            ouvrir(Sonneries.intentionFichier(), DEMANDE_FICHIER)
        }

        boutonEcouter.setOnClickListener {
            if (Sonneries.enLecture()) Sonneries.arreter() else Sonneries.ecouter(this)
            majSonnerie()
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
        Sonneries.arreter()
    }

    // --- Interface -----------------------------------------------------------

    private fun majInterface() {
        val blanc = getColor(R.color.blanc)
        val vert = getColor(R.color.vert)

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
                sousMatrice.setTextColor(getColor(R.color.rouge))
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
        dernierePrise.text = texteDernierePrise()
        majDurees()
        majSonnerie()
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

    private fun texteDernierePrise(): String {
        val prise = Historique.derniere(this) ?: return getString(R.string.derniere_prise_aucune)
        return getString(
            R.string.derniere_prise,
            Dates.jourRelatif(this, prise.debut),
            Dates.heure(this, prise.debut)
        )
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
            puce.setTextColor(getColor(if (active) R.color.noir else R.color.gris))
            puce.isSelected = active
        }
    }

    // --- Sonnerie ------------------------------------------------------------

    private fun majSonnerie() {
        sonnerieActuelle.text = getString(R.string.sonnerie_actuelle, Sonneries.nom(this))
        val enLecture = Sonneries.enLecture()
        boutonEcouter.text =
            getString(if (enLecture) R.string.symbole_arreter else R.string.symbole_ecouter)
        boutonEcouter.contentDescription =
            getString(if (enLecture) R.string.sonnerie_arreter else R.string.sonnerie_ecouter)
    }

    private fun ouvrir(intention: Intent, code: Int) {
        try {
            startActivityForResult(intention, code)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.sonnerie_illisible, Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requete: Int, resultat: Int, donnees: Intent?) {
        super.onActivityResult(requete, resultat, donnees)
        if (resultat != RESULT_OK) return
        when (requete) {
            DEMANDE_SONNERIE ->
                Sonneries.definir(this, sonnerieChoisie(donnees), persistable = false)

            DEMANDE_FICHIER -> {
                val adresse = donnees?.data ?: return
                Sonneries.definir(this, adresse, persistable = true)
            }
        }
        majInterface()
    }

    @Suppress("DEPRECATION")
    private fun sonnerieChoisie(donnees: Intent?): Uri? {
        if (donnees == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            donnees.getParcelableExtra(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                Uri::class.java
            )
        } else {
            donnees.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
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

        const val DEMANDE_SONNERIE = 10
        const val DEMANDE_FICHIER = 11
    }
}
