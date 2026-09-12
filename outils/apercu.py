#!/usr/bin/env python3
"""Génère apercu.svg : rendu fidèle du widget et de l'écran principal.

La grille de points est volontairement identique à celle de Matrice.kt, pour que
l'aperçu ne puisse pas diverger de ce que l'appli affiche réellement.
"""

import pathlib

GLYPHES = {
    "0": [".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    "1": ["..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."],
    "2": [".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"],
    "3": ["#####", "...#.", "..#..", "...#.", "....#", "#...#", ".###."],
    "4": ["...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."],
    "5": ["#####", "#....", "####.", "....#", "....#", "#...#", ".###."],
    "6": ["..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."],
    "7": ["#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."],
    "8": [".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."],
    "9": [".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."],
    ":": [".", ".", "#", ".", "#", ".", "."],
    "O": [".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    "K": ["#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"],
}

NOIR = "#000000"
ANTHRACITE = "#242424"
BLANC = "#FFFFFF"
GRIS = "#8A8A8A"
ROUGE = "#D71921"
VERT = "#3BD671"


def colonnes(texte):
    return sum(len(GLYPHES[c][0]) for c in texte) + (len(texte) - 1)


def points(texte, cx, cy, pas, couleur):
    """Dessine le texte en points, centré sur (cx, cy)."""
    largeur = colonnes(texte) * pas
    hauteur = 7 * pas
    x0, y0 = cx - largeur / 2, cy - hauteur / 2
    rayon = pas * 0.72 / 2
    sortie, col = [], 0
    for caractere in texte:
        motif = GLYPHES[caractere]
        for ligne, contenu in enumerate(motif):
            for decalage, point in enumerate(contenu):
                if point == "#":
                    sortie.append(
                        '<circle cx="%.1f" cy="%.1f" r="%.1f" fill="%s"/>'
                        % (
                            x0 + (col + decalage) * pas + pas / 2,
                            y0 + ligne * pas + pas / 2,
                            rayon,
                            couleur,
                        )
                    )
        col += len(motif[0]) + 1
    return sortie


def etiquette(texte, x, y, couleur, ancre="start", taille=15):
    return (
        '<text x="%.1f" y="%.1f" fill="%s" font-family="Helvetica,Arial,sans-serif" '
        'font-size="%d" font-weight="500" letter-spacing="%.1f" text-anchor="%s">%s</text>'
        % (x, y, couleur, taille, taille * 0.22, ancre, texte.upper())
    )


def pilule(x, y, w, h, gros, couleur_gros, gauche, couleur_gauche, droite):
    """Un widget : pilule anthracite, gros chiffres, ligne d'étiquettes."""
    e = [
        '<rect x="%d" y="%d" width="%d" height="%d" rx="%d" fill="%s"/>'
        % (x, y, w, h, h // 2, ANTHRACITE)
    ]
    e += points(gros, x + w / 2, y + h / 2 - 16, 10.4, couleur_gros)
    e.append(etiquette(gauche, x + 44, y + h - 30, couleur_gauche))
    e.append(etiquette(droite, x + w - 44, y + h - 30, GRIS, ancre="end"))
    return e


def ecran(x, y, w, h, gros, couleur_gros, sous, couleur_sous, consigne, bouton):
    """L'écran principal de l'appli."""
    e = [
        '<rect x="%d" y="%d" width="%d" height="%d" rx="46" fill="%s"/>'
        % (x, y, w, h, NOIR),
        etiquette("Hashimoto", x + w / 2, y + 70, GRIS, ancre="middle", taille=14),
    ]
    cercle_y = y + 300
    rayon = 172
    e.append(
        '<circle cx="%.1f" cy="%.1f" r="%d" fill="%s"/>'
        % (x + w / 2, cercle_y, rayon, ANTHRACITE)
    )
    pas = min(238 / colonnes(gros), 18)
    e += points(gros, x + w / 2, cercle_y - 22, pas, couleur_gros)
    e.append(etiquette(sous, x + w / 2, cercle_y + 84, couleur_sous, ancre="middle"))

    for index, ligne in enumerate(consigne):
        e.append(
            '<text x="%.1f" y="%.1f" fill="%s" font-family="Helvetica,Arial,sans-serif" '
            'font-size="17" text-anchor="middle">%s</text>'
            % (x + w / 2, cercle_y + rayon + 66 + index * 26, GRIS, ligne)
        )

    bas = cercle_y + rayon + 150
    if bouton:
        e.append(
            '<rect x="%.1f" y="%.1f" width="200" height="52" rx="26" fill="none" '
            'stroke="#3A3A3A" stroke-width="1.5"/>' % (x + w / 2 - 100, bas)
        )
        e.append(etiquette(bouton, x + w / 2, bas + 33, BLANC, ancre="middle", taille=14))
    else:
        # Les puces de durée, visibles seulement au repos.
        e.append(etiquette("Délai à respecter", x + w / 2, bas + 4, GRIS,
                           ancre="middle", taille=13))
        largeurs = [92, 92, 92]
        total = sum(largeurs) + 2 * 14
        depart = x + w / 2 - total / 2
        for index, minutes in enumerate((30, 45, 60)):
            actif = minutes == 45
            px = depart + index * (92 + 14)
            e.append(
                '<rect x="%.1f" y="%.1f" width="92" height="48" rx="24" fill="%s"/>'
                % (px, bas + 26, BLANC if actif else ANTHRACITE)
            )
            e.append(
                '<text x="%.1f" y="%.1f" fill="%s" font-family="Helvetica,Arial,sans-serif" '
                'font-size="17" text-anchor="middle">%d min</text>'
                % (px + 46, bas + 57, NOIR if actif else GRIS, minutes)
            )
    return e


def construire():
    largeur, hauteur = 1020, 980
    e = [
        '<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" '
        'viewBox="0 0 %d %d">' % (largeur, hauteur, largeur, hauteur),
        '<rect width="%d" height="%d" fill="#0B0B0B"/>' % (largeur, hauteur),
        etiquette("Widget — écran d'accueil", 56, 62, GRIS, taille=16),
    ]

    e += pilule(56, 92, 400, 168, "45", BLANC, "Cachet pris ?", BLANC, "45 min")
    e += pilule(56, 300, 400, 168, "32", BLANC, "Ne pas manger", ROUGE, "07:57")
    e += pilule(56, 508, 400, 168, "OK", VERT, "C'est bon", VERT, "Manger")

    e.append(etiquette("Au repos", 56, 290, "#5A5A5A", taille=13))
    e.append(etiquette("Pendant le délai", 56, 498, "#5A5A5A", taille=13))
    e.append(etiquette("Délai écoulé", 56, 706, "#5A5A5A", taille=13))

    e.append(etiquette("Tuile des réglages rapides", 56, 780, GRIS, taille=16))
    e.append('<rect x="56" y="806" width="192" height="112" rx="34" fill="%s"/>' % ANTHRACITE)
    e.append('<circle cx="100" cy="846" r="17" fill="none" stroke="%s" stroke-width="3"/>' % BLANC)
    e.append(etiquette("Cachet 45", 56, 900, BLANC, taille=14))
    e.append(
        '<text x="132" y="854" fill="%s" font-family="Helvetica,Arial,sans-serif" '
        'font-size="17">32:07</text>' % BLANC
    )

    e.append(etiquette("Application", 540, 62, GRIS, taille=16))
    e += ecran(
        540, 92, 424, 830,
        "45", BLANC, "Cachet pris", BLANC,
        ["Appuyez juste après avoir", "avalé le cachet."],
        None,
    )

    e.append("</svg>")
    return "\n".join(e)


if __name__ == "__main__":
    destination = pathlib.Path(__file__).resolve().parent.parent / "apercu.svg"
    destination.write_text(construire(), encoding="utf-8")
    print(destination)
