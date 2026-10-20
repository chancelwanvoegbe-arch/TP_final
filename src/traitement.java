import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class traitement {


    static final String fichier_csv = "mesures_saiss.csv";
    static final String separateur = ",";
    static final String valeurManquante = "NA";
    static final int nbColonnes = 7;

    // ===== Plages de validité =====
    static final long horodatageMin = 0L;
    static final long horodatageMax = Long.MAX_VALUE;

    static final float tempMin = -50.0f;
    static final float tempMax = 60.0f;

    static final int humiditeMin = 0;
    static final int humiditeMax = 100;

    static final double precipitationMin = 0.0;
    static final double precipitationMax = 500.0;

    static final int ventMin = 0;
    static final int ventMax = 120;

    static final int codeEtatMin = 0;
    static final int codeEtatMax = 10;

    // ===== Formatage =====
    static final String formatPourcentage = "%.2f%%";

    // ===== Seuils métier =====
    static final double tBaseDjc = 10.0;
    static final double tGel = 2.0;
    static final double tChaud = 30.0;
    static final double ventFort = 35.0;
    static final double humiditeSeche = 40.0;

    // ===== Niveaux de risque (codes) =====
    static final int risqueGel = 1;
    static final int risqueIncendie = 2;
    static final int risqueStress = 3;
    static final int risqueNormal = 4;

    // ===== Conversion Fahrenheit =====
    static final double facteurFahrenheit = 9.0 / 5.0;
    static final double offsetFahrenheit = 32.0;

    // ===== Indices des agrégats par station =====
    static final int aggTmin = 0;
    static final int aggTmax = 1;
    static final int aggSommeTemp = 2;
    static final int aggNbTemp = 3;
    static final int aggCumulPrecip = 4;
    static final int aggNbGel = 5;
    static final int aggNbIncendie = 6;
    static final int aggNbStress = 7;
    static final int aggNbNormal = 8;
    static final int nbAgg = 9;

    // ===== Compteurs globaux =====
    static int lignesLues = 0;
    static int lignesAcceptees = 0;
    static int lignesRejetees = 0;

    // ===== Agrégats par station =====
    static Map<String, double[]> agregats = new HashMap<>();

    // ===== Variable de classe (question 3) =====
    static int compteurAppelsMoyenne = 0;

    // ===== Constante final (question 3) =====
    static final double MOYENNE_PAR_DEFAUT = 0.0;

    // =========================================================
    // PARTIE 3 — LECTURE ET CONVERSION
    // =========================================================

    static void traiterFichier(String chemin) {
        try (BufferedReader br = new BufferedReader(new FileReader(chemin))) {

            String ligne = br.readLine(); // en-tête
            if (ligne == null) {
                System.err.println("Fichier vide : " + chemin);
                return;
            }

            int numeroLigne = 1;
            while ((ligne = br.readLine()) != null) {
                numeroLigne++;
                lignesLues++;

                if (ligne.trim().isEmpty()) {
                    lignesRejetees++;
                    System.err.println(
                            "Ligne " + numeroLigne + " rejetée : ligne vide");
                    continue;
                }

                String causeRejet = verifierLigne(ligne);
                if (causeRejet == null) {
                    lignesAcceptees++;
                    accumulerMesure(ligne.split(separateur));
                } else {
                    lignesRejetees++;
                    System.err.println(
                            "Ligne " + numeroLigne + " rejetée : " + causeRejet);
                }
            }

        } catch (IOException e) {
            System.err.println("Erreur de lecture du fichier : " + e.getMessage());
        }
    }

    static String verifierLigne(String ligne) {
        String[] champs = ligne.split(separateur);

        if (champs.length < nbColonnes) {
            return "colonnes insuffisantes ("
                    + "Colonnes existantes:" + champs.length
                    + " Colonnes attendues: " + nbColonnes + ")";
        }

        if (!ValiditeColonne(champs[1])) {
            return "station manquante ou invalide";
        }

        String erreur = verifierChampLong(
                champs[0], "horodatage", horodatageMin, horodatageMax);
        if (erreur != null) return erreur;

        erreur = verifierChampFloat(
                champs[2], "température", tempMin, tempMax);
        if (erreur != null) return erreur;

        erreur = verifierChampINT(
                champs[3], "humidité", humiditeMin, humiditeMax);
        if (erreur != null) return erreur;

        erreur = verifierChampDouble(
                champs[4], "précipitation",
                precipitationMin, precipitationMax);
        if (erreur != null) return erreur;

        erreur = verifierChampByte(
                champs[5], "vent", ventMin, ventMax);
        if (erreur != null) return erreur;

        erreur = verifierChampByte(
                champs[6], "code_etat", codeEtatMin, codeEtatMax);
        if (erreur != null) return erreur;

        return null;
    }

    static boolean ValiditeColonne(String champ) {
        return champ != null
                && !champ.trim().isEmpty()
                && !champ.trim().equalsIgnoreCase(valeurManquante);
    }

    static String verifierChampLong(String champ, String nomChamp,
                                    long min, long max) {
        if (!ValiditeColonne(champ)) {
            return "champ " + nomChamp + " manquant ou non convertible";
        }
        long valeur;
        try {
            valeur = Long.parseLong(champ.trim());
        } catch (NumberFormatException e) {
            return "champ " + nomChamp + " non convertible";
        }
        if (valeur < min || valeur > max) {
            return "champ " + nomChamp + " hors plage [" + min + ", "
                    + max + "] : " + valeur;
        }
        return null;
    }

    static String verifierChampFloat(String champ, String nomChamp,
                                     float min, float max) {
        if (!ValiditeColonne(champ)) {
            return "champ " + nomChamp + " manquant ou non convertible";
        }
        float valeur;
        try {
            valeur = Float.parseFloat(champ.trim());
        } catch (NumberFormatException e) {
            return "champ " + nomChamp + " non convertible";
        }
        if (valeur < min || valeur > max) {
            return "champ " + nomChamp + " hors plage [" + min + ", "
                    + max + "] : " + valeur;
        }
        return null;
    }

    static String verifierChampINT(String champ, String nomChamp,
                                   int min, int max) {
        if (!ValiditeColonne(champ)) {
            return "champ " + nomChamp + " manquant ou non convertible";
        }
        int valeur;
        try {
            valeur = Integer.parseInt(champ.trim());
        } catch (NumberFormatException e) {
            return "champ " + nomChamp + " non convertible";
        }
        if (valeur < min || valeur > max) {
            return "champ " + nomChamp + " hors plage [" + min + ", "
                    + max + "] : " + valeur;
        }
        return null;
    }

    static String verifierChampDouble(String champ, String nomChamp,
                                      double min, double max) {
        if (!ValiditeColonne(champ)) {
            return "champ " + nomChamp + " manquant ou non convertible";
        }
        double valeur;
        try {
            valeur = Double.parseDouble(champ.trim());
        } catch (NumberFormatException e) {
            return "champ " + nomChamp + " non convertible";
        }
        if (valeur < min || valeur > max) {
            return "champ " + nomChamp + " hors plage [" + min + ", "
                    + max + "] : " + valeur;
        }
        return null;
    }

    static String verifierChampByte(String champ, String nomChamp,
                                    int min, int max) {
        if (!ValiditeColonne(champ)) {
            return "champ " + nomChamp + " manquant ou non convertible";
        }
        int valeur;
        try {
            valeur = Integer.parseInt(champ.trim());
        } catch (NumberFormatException e) {
            return "champ " + nomChamp + " non convertible";
        }
        if (valeur < min || valeur > max) {
            return "champ " + nomChamp + " hors plage [" + min + ", "
                    + max + "] : " + valeur;
        }
        if (valeur < Byte.MIN_VALUE || valeur > Byte.MAX_VALUE) {
            return "champ " + nomChamp + " dépasse la capacité d'un byte : "
                    + valeur;
        }
        return null;
    }

    // =========================================================
    // BILAN GLOBAL
    // =========================================================

    static void afficherBilan() {
        System.out.println();
        System.out.println(" ---- BILAN ----");
        System.out.println("Lignes lues       : " + lignesLues);
        System.out.println("Lignes acceptées  : " + lignesAcceptees);
        System.out.println("Lignes rejetées   : " + lignesRejetees);
        System.out.println("Taux d'acceptation : " + calculerTauxAcceptation());
    }

    static String calculerTauxAcceptation() {
        if (lignesLues == 0) {
            return "0,00%";
        }
        double taux = (lignesAcceptees * 100.0) / lignesLues;
        return String.format(formatPourcentage, taux);
    }

    // =========================================================
    // PARTIE 5 — INDICATEURS AGRONOMIQUES
    // =========================================================

    static void accumulerMesure(String[] champs) {
        String station = champs[1].trim();
        float temperature = Float.parseFloat(champs[2].trim());
        int humidite = Integer.parseInt(champs[3].trim());
        double precipitation = Double.parseDouble(champs[4].trim());
        byte vent = Byte.parseByte(champs[5].trim());

        double[] agg = agregats.get(station);
        if (agg == null) {
            agg = initialiserAgregats();
            agregats.put(station, agg);
        }

        if (temperature < agg[aggTmin]) agg[aggTmin] = temperature;
        if (temperature > agg[aggTmax]) agg[aggTmax] = temperature;
        agg[aggSommeTemp] += temperature;
        agg[aggNbTemp] += 1;
        agg[aggCumulPrecip] += precipitation;

        int niveau = determinerNiveauRisque(temperature, vent, humidite);
        switch (niveau) {
            case risqueGel:
                agg[aggNbGel]++;
                break;
            case risqueIncendie:
                agg[aggNbIncendie]++;
                break;
            case risqueStress:
                agg[aggNbStress]++;
                break;
            default:
                agg[aggNbNormal]++;
                break;
        }
    }

    static double[] initialiserAgregats() {
        double[] agg = new double[nbAgg];
        agg[aggTmin] = Double.MAX_VALUE;
        agg[aggTmax] = -Double.MAX_VALUE;
        return agg;
    }

    static int determinerNiveauRisque(float temperature, byte vent,
                                      int humidite) {
        if (temperature < tGel) {
            return risqueGel;
        }
        if (temperature > tChaud
                && vent > ventFort
                && humidite < humiditeSeche) {
            return risqueIncendie;
        }
        if (temperature > tChaud) {
            return risqueStress;
        }
        return risqueNormal;
    }

    static double calculerAmplitude(double tmin, double tmax) {
        return tmax - tmin;
    }

    static double calculerTemperatureMoyenne(double somme, int nb) {
        if (nb == 0) return 0.0;
        return somme / nb;
    }

    static double convertirEnFahrenheit(double celsius) {
        return celsius * facteurFahrenheit + offsetFahrenheit;
    }

    static double calculerDegresJoursCroissance(double tmin, double tmax) {
        double moyenne = (tmax + tmin) / 2.0;
        double djc = moyenne - tBaseDjc;
        return (djc < 0) ? 0.0 : djc;
    }

    static void afficherBilanParStation() {
        System.out.println();
        System.out.println("========== BILAN PAR STATION ==========");

        if (agregats.isEmpty()) {
            System.out.println("Aucune station à afficher.");
            return;
        }

        for (Map.Entry<String, double[]> entree : agregats.entrySet()) {
            afficherStation(entree.getKey(), entree.getValue());
        }
    }

    static void afficherStation(String station, double[] agg) {
        double tmin = agg[aggTmin];
        double tmax = agg[aggTmax];
        double moyenne = calculerTemperatureMoyenne(
                agg[aggSommeTemp], (int) agg[aggNbTemp]);
        double amplitude = calculerAmplitude(tmin, tmax);
        double moyenneF = convertirEnFahrenheit(moyenne);
        double djc = calculerDegresJoursCroissance(tmin, tmax);

        System.out.println();
        System.out.println("--- Station : " + station + " ---");
        System.out.println("Température minimale     : " + tmin + " °C");
        System.out.println("Température maximale     : " + tmax + " °C");
        System.out.println("Amplitude thermique      : " + amplitude + " °C");
        System.out.println("Température moyenne      : " + moyenne + " °C / "
                + moyenneF + " °F");
        System.out.println("Cumul précipitations     : "
                + agg[aggCumulPrecip] + " mm");
        System.out.println("Degrés-jours (base 10°C) : " + djc);
        System.out.println("Risques -> gel : " + (int) agg[aggNbGel]
                + " | incendie : " + (int) agg[aggNbIncendie]
                + " | stress : " + (int) agg[aggNbStress]
                + " | normal : " + (int) agg[aggNbNormal]);
    }

    // =========================================================
    // PARTIE 4 — GESTION MÉTÉO
    // =========================================================

    static final double GESTION_T_MIN = -15.0;
    static final double GESTION_T_MAX = 33.0;
    static final double GESTION_HUMIDITE_MIN = 0.0;
    static final double GESTION_HUMIDITE_MAX = 100.0;

    static final int CODE_ETAT_NORMAL = 1;
    static final int CODE_ETAT_SUSPECTE = 2;
    static final int CODE_ETAT_PANNE = 3;

    /** Vérifie que la température est plausible pour la région. */
    static boolean temperaturePlausible(double temperature) {
        return temperature >= GESTION_T_MIN && temperature <= GESTION_T_MAX;
    }

    /** Vérifie que l'humidité est plausible. */
    static boolean humiditePlausible(double humidite) {
        return humidite >= GESTION_HUMIDITE_MIN
                && humidite <= GESTION_HUMIDITE_MAX;
    }

    /** Traduit un code d'état capteur en libellé. */
    static String etatMeteo(int codeEtat) {
        switch (codeEtat) {
            case CODE_ETAT_NORMAL:
                return "Normal";
            case CODE_ETAT_SUSPECTE:
                return "Suspecte";
            case CODE_ETAT_PANNE:
                return "Panne";
            default:
                return "Code inconnu";
        }
    }

    /** Détermine si une mesure est retenue par le programme. */
    static boolean mesureRetenue(boolean convertible,
                                 double temperature,
                                 double humidite,
                                 int codeEtat) {
        return convertible
                && temperaturePlausible(temperature)
                && humiditePlausible(humidite)
                && codeEtat != CODE_ETAT_PANNE;
    }

    /** Compare l'état enregistré par le capteur et la décision du programme. */
    static void comparerMesure(boolean convertible,
                               double temperature,
                               double humidite,
                               int codeEtat) {
        boolean retenue = mesureRetenue(
                convertible, temperature, humidite, codeEtat);

        System.out.println("État enregistré par le capteur : "
                + etatMeteo(codeEtat));
        System.out.println("Mesure retenue par le programme : " + retenue);

        if (codeEtat == CODE_ETAT_NORMAL && !retenue) {
            System.out.println(
                    "Le capteur enregistre Normal, mais la mesure est rejetée.");
        } else if (codeEtat != CODE_ETAT_NORMAL && retenue) {
            System.out.println(
                    "Le capteur n'enregistre pas une mesure normale, "
                            + "mais on garde la mesure.");
        } else {
            System.out.println("Pas de contradiction.");
        }
    }

    /** Bloc de démonstration de la partie 4. */
    static void executerPartie4() {
        double temperature = 31.5;
        double humidite = 70.0;
        boolean convertible = true;
        int codeEtat = CODE_ETAT_NORMAL;

        System.out.println();
        System.out.println("========== PARTIE 4 : GESTION MÉTÉO ==========");

        if (temperaturePlausible(temperature)) {
            System.out.println("Température plausible pour la région");
        } else {
            System.out.println("Température non plausible pour la région");
        }

        if (humiditePlausible(humidite)) {
            System.out.println("Humidité relative plausible");
        } else {
            System.out.println("Humidité relative non plausible");
        }

        System.out.println("État météo : " + etatMeteo(codeEtat));

        System.out.println("Mesure retenue : "
                + mesureRetenue(convertible, temperature, humidite, codeEtat));

        comparerMesure(convertible, temperature, humidite, codeEtat);
    }

    // =========================================================
    // PARTIE 6 — MODULARITÉ ET MÉTHODES
    // =========================================================

    /** Question 1 : moyenne d'un tableau de valeurs décimales. */
    static double moyenneSurchargee(double[] decimal) {
        compteurAppelsMoyenne++;
        if (decimal == null || decimal.length == 0) {
            return MOYENNE_PAR_DEFAUT;
        }
        double somme = 0;
        for (int i = 0; i < decimal.length; i++) {
            somme += decimal[i];
        }
        return somme / decimal.length;
    }

    /** Question 1 : moyenne de deux valeurs isolées. */
    static double moyenneSurchargee(double a, double b) {
        compteurAppelsMoyenne++;
        return (a + b) / 2.0;
    }

    /** Question 2 : passage par valeur. */
    static void incrementer(int compteur) {
        compteur++;
        System.out.println("  (dans incrementer) compteur = " + compteur);
    }

    static void executerPartie6() {
        System.out.println();
        System.out.println("========== QUESTION 1 : MOYENNES SURCHARGÉES ==========");

        double[] releves = {12.5, 15.0, 9.75, 18.0, 11.25};
        double moyTab = moyenneSurchargee(releves);
        System.out.println("Moyenne (tableau)          : " + moyTab);

        double moyIsolee = moyenneSurchargee(14.0, 16.0);
        System.out.println("Moyenne (2 valeurs isolées): " + moyIsolee);

        System.out.println("Nombre d'appels cumulés    : " + compteurAppelsMoyenne);

        System.out.println();
        System.out.println("========== QUESTION 2 : PASSAGE PAR VALEUR ==========");

        int compteur = 5;
        System.out.println("Avant l'appel : compteur = " + compteur);
        incrementer(compteur);
        System.out.println("Après l'appel : compteur = " + compteur);
    }

    // =========================================================
    // MENU INTERACTIF
    // =========================================================

    static void afficherMenu() {
        System.out.println();
        System.out.println("===== AgroMétéo Saïss =====");
        System.out.println("1. Afficher le rapport d'une station");
        System.out.println("2. Afficher toutes les alertes");
        System.out.println("3. Convertir une température (°C <-> °F)");
        System.out.println("4. Afficher le bilan de qualité des données");
        System.out.println("0. Quitter");
        System.out.print("Entrez votre choix : ");
    }

    static void executerMenu(Scanner sc) {
        boolean quitter = false;
        while (!quitter) {
            afficherMenu();

            int choix;
            try {
                choix = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Veuillez saisir un nombre.");
                continue;
            }

            switch (choix) {
                case 1:
                    System.out.print("Nom de la station : ");
                    String nom = sc.nextLine().trim();
                    double[] agg = agregats.get(nom);
                    if (agg == null) {
                        System.out.println("Station inconnue : " + nom);
                    } else {
                        afficherStation(nom, agg);
                    }
                    break;

                case 2:
                    afficherBilanParStation();
                    break;

                case 3:
                    System.out.print("Température en °C : ");
                    try {
                        double celsius = Double.parseDouble(sc.nextLine().trim());
                        double fahrenheit = convertirEnFahrenheit(celsius);
                        System.out.println(celsius + " °C = " + fahrenheit + " °F");
                    } catch (NumberFormatException e) {
                        System.out.println("Valeur invalide.");
                    }
                    break;

                case 4:
                    afficherBilan();
                    break;

                case 0:
                    quitter = true;
                    System.out.println("Au revoir !");
                    break;

                default:
                    System.out.println("Opération invalide.");
                    break;
            }
        }
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {
        executerPartie6();       // Questions 1 et 2
        executerPartie4();       // Gestion météo (ex-GestionMeteo)
        traiterFichier(fichier_csv);
        afficherBilan();
        afficherBilanParStation();

        Scanner sc = new Scanner(System.in);
        executerMenu(sc);
        sc.close();
    }
}