#!/usr/bin/env python3
"""Assemble l'APK : entrées de base.apk + classes.dex, avec alignement 4 octets.

Android refuse d'installer une appli ciblant l'API 30+ dont resources.arsc est
compressé ou mal aligné ; c'est exactement ce que fait zipalign, réimplémenté
ici puisque les build-tools ne sont pas disponibles.
"""

import struct
import sys
import zipfile

ENTETE_LOCAL = 30
ID_PADDING = 0xD935  # identifiant de champ « extra » utilisé pour le bourrage


def extra_alignement(decalage: int, longueur_nom: int) -> bytes:
    """Champ extra dont la taille amène le début des données sur un multiple de 4."""
    reste = (decalage + ENTETE_LOCAL + longueur_nom) % 4
    if reste == 0:
        return b""
    taille = (4 - reste) + 4  # + 4 pour loger l'en-tête du champ extra
    return struct.pack("<HH", ID_PADDING, taille - 4) + b"\x00" * (taille - 4)


def assembler(base: str, dex: str, destination: str) -> None:
    with zipfile.ZipFile(base) as source:
        entrees = [(info, source.read(info.filename)) for info in source.infolist()]

    with open(dex, "rb") as f:
        contenu_dex = f.read()
    info_dex = zipfile.ZipInfo("classes.dex", date_time=(1980, 1, 1, 0, 0, 0))
    info_dex.compress_type = zipfile.ZIP_DEFLATED
    entrees.append((info_dex, contenu_dex))

    with zipfile.ZipFile(destination, "w") as sortie:
        for info, contenu in entrees:
            entree = zipfile.ZipInfo(info.filename, date_time=info.date_time)
            entree.compress_type = info.compress_type
            entree.external_attr = info.external_attr
            entree.create_system = 0
            if entree.compress_type == zipfile.ZIP_STORED:
                entree.extra = extra_alignement(
                    sortie.fp.tell(), len(entree.filename.encode("utf-8"))
                )
            sortie.writestr(entree, contenu)


def verifier(chemin: str) -> int:
    """Contrôle que chaque entrée STORED démarre sur un multiple de 4."""
    problemes = 0
    with open(chemin, "rb") as f, zipfile.ZipFile(chemin) as z:
        for info in z.infolist():
            f.seek(info.header_offset + 26)
            longueur_nom, longueur_extra = struct.unpack("<HH", f.read(4))
            debut = info.header_offset + ENTETE_LOCAL + longueur_nom + longueur_extra
            if info.compress_type == zipfile.ZIP_STORED and debut % 4 != 0:
                print(f"  MAL ALIGNE  {info.filename} @ {debut}")
                problemes += 1
            else:
                marque = "stocké" if info.compress_type == zipfile.ZIP_STORED else "compressé"
                print(f"  ok {marque:9} {info.filename} @ {debut}")
    return problemes


if __name__ == "__main__":
    if sys.argv[1] == "verifier":
        sys.exit(1 if verifier(sys.argv[2]) else 0)
    assembler(sys.argv[1], sys.argv[2], sys.argv[3])
    print("assemblé:", sys.argv[3])
