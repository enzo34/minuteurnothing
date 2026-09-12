package fr.enzo.cachet45

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/**
 * Rendu « matrice de points » façon Nothing OS.
 *
 * La police NDot n'est pas redistribuable, donc les chiffres sont dessinés
 * point par point : une grille de 5 colonnes sur 7 lignes par glyphe, chaque
 * point étant un vrai cercle. Résultat identique à l'œil, et utilisable aussi
 * bien dans le widget (Bitmap) que dans l'appli (Canvas).
 */
object Matrice {

    const val LIGNES = 7

    private val GLYPHES: Map<Char, List<String>> = mapOf(
        '0' to listOf(
            ".###.",
            "#...#",
            "#...#",
            "#...#",
            "#...#",
            "#...#",
            ".###."
        ),
        '1' to listOf(
            "..#..",
            ".##..",
            "..#..",
            "..#..",
            "..#..",
            "..#..",
            ".###."
        ),
        '2' to listOf(
            ".###.",
            "#...#",
            "....#",
            "...#.",
            "..#..",
            ".#...",
            "#####"
        ),
        '3' to listOf(
            "#####",
            "...#.",
            "..#..",
            "...#.",
            "....#",
            "#...#",
            ".###."
        ),
        '4' to listOf(
            "...#.",
            "..##.",
            ".#.#.",
            "#..#.",
            "#####",
            "...#.",
            "...#."
        ),
        '5' to listOf(
            "#####",
            "#....",
            "####.",
            "....#",
            "....#",
            "#...#",
            ".###."
        ),
        '6' to listOf(
            "..##.",
            ".#...",
            "#....",
            "####.",
            "#...#",
            "#...#",
            ".###."
        ),
        '7' to listOf(
            "#####",
            "....#",
            "...#.",
            "..#..",
            ".#...",
            ".#...",
            ".#..."
        ),
        '8' to listOf(
            ".###.",
            "#...#",
            "#...#",
            ".###.",
            "#...#",
            "#...#",
            ".###."
        ),
        '9' to listOf(
            ".###.",
            "#...#",
            "#...#",
            ".####",
            "....#",
            "...#.",
            ".##.."
        ),
        ':' to listOf(
            ".",
            ".",
            "#",
            ".",
            "#",
            ".",
            "."
        ),
        'O' to listOf(
            ".###.",
            "#...#",
            "#...#",
            "#...#",
            "#...#",
            "#...#",
            ".###."
        ),
        'K' to listOf(
            "#...#",
            "#..#.",
            "#.#..",
            "##...",
            "#.#..",
            "#..#.",
            "#...#"
        ),
        'H' to listOf(
            "#...#",
            "#...#",
            "#...#",
            "#####",
            "#...#",
            "#...#",
            "#...#"
        ),
        'M' to listOf(
            "#...#",
            "##.##",
            "#.#.#",
            "#...#",
            "#...#",
            "#...#",
            "#...#"
        ),
        '-' to listOf(
            ".....",
            ".....",
            ".....",
            "#####",
            ".....",
            ".....",
            "....."
        ),
        ' ' to listOf("..", "..", "..", "..", "..", "..", "..")
    )

    private const val ESPACE_ENTRE_GLYPHES = 1

    private fun glyphe(caractere: Char): List<String> =
        GLYPHES[caractere] ?: GLYPHES[' ']!!

    /** Nombre de colonnes de points occupées par le texte. */
    fun colonnes(texte: String): Int {
        if (texte.isEmpty()) return 0
        var total = 0
        texte.forEach { total += glyphe(it).first().length }
        return total + ESPACE_ENTRE_GLYPHES * (texte.length - 1)
    }

    /**
     * Dessine le texte sur un canevas, le coin haut-gauche de la grille à (x, y).
     *
     * @param pas distance entre deux centres de points
     * @param diametre taille d'un point (laisse un interstice si < pas)
     */
    fun dessiner(
        canevas: Canvas,
        texte: String,
        x: Float,
        y: Float,
        pas: Float,
        diametre: Float,
        peinture: Paint
    ) {
        var colonne = 0
        texte.forEach { caractere ->
            val motif = glyphe(caractere)
            motif.forEachIndexed { ligne, contenu ->
                contenu.forEachIndexed { decalage, point ->
                    if (point == '#') {
                        canevas.drawCircle(
                            x + (colonne + decalage) * pas + pas / 2f,
                            y + ligne * pas + pas / 2f,
                            diametre / 2f,
                            peinture
                        )
                    }
                }
            }
            colonne += motif.first().length + ESPACE_ENTRE_GLYPHES
        }
    }

    /** Bitmap prêt à être posé dans un RemoteViews (widget). */
    fun bitmap(texte: String, pas: Float, diametre: Float, couleur: Int): Bitmap {
        val largeur = (colonnes(texte) * pas).toInt().coerceAtLeast(1)
        val hauteur = (LIGNES * pas).toInt().coerceAtLeast(1)
        val image = Bitmap.createBitmap(largeur, hauteur, Bitmap.Config.ARGB_8888)
        val canevas = Canvas(image)
        val peinture = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = couleur }
        dessiner(canevas, texte, 0f, 0f, pas, diametre, peinture)
        return image
    }
}
