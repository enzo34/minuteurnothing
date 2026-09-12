import com.android.apksig.ApkSigner;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Signe l'APK en v1 (JAR) et v2, le minimum exigé pour une cible API 30+. */
public final class Signeur {

    public static void main(String[] args) throws Exception {
        File entree = new File(args[0]);
        File sortie = new File(args[1]);
        File magasin = new File(args[2]);
        char[] motDePasse = args[3].toCharArray();
        String alias = args[4];
        int minSdk = Integer.parseInt(args[5]);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream flux = new FileInputStream(magasin)) {
            ks.load(flux, motDePasse);
        }

        PrivateKey cle = (PrivateKey) ks.getKey(alias, motDePasse);
        if (cle == null) {
            throw new IllegalStateException("alias introuvable dans le magasin : " + alias);
        }
        Certificate[] chaine = ks.getCertificateChain(alias);
        List<X509Certificate> certificats = new ArrayList<>();
        for (Certificate certificat : chaine) {
            certificats.add((X509Certificate) certificat);
        }

        ApkSigner.SignerConfig config =
                new ApkSigner.SignerConfig.Builder(alias, cle, certificats).build();

        ApkSigner signeur = new ApkSigner.Builder(Collections.singletonList(config))
                .setInputApk(entree)
                .setOutputApk(sortie)
                .setMinSdkVersion(minSdk)
                .setV1SigningEnabled(false)
                .setV2SigningEnabled(true)
                .build();
        signeur.sign();

        System.out.println("signé : " + sortie.getAbsolutePath()
                + " (" + sortie.length() + " octets)");
    }
}
