package net.minervamc.minerva.types;

public enum HeritageType {
    // GREEK:
    ZEUS,
    HADES,
    POSEIDON,
    DEMETER,
    ARES,
    ATHENA,
    APOLLO_GREEK,
    ARTEMIS,
    HEPHAESTUS,
    APHRODITE,
    HERMES,
    DIONYSUS,
    HECATE,
    KHIONE,
    PSYCHE_GREEK,
    IRIS,
    HESTIA,
    ARKE,
    HYPNOS,
    // ROMAN:
    JUPITER,
    PLUTO,
    NEPTUNE,
    CERES,
    MARS,
    APOLLO_ROMAN,
    DIANA,
    VULCAN,
    VENUS,
    MERCURY,
    BACCHUS,
    JANUS,
    BELLONA,
    CHIONE,
    PSYCHE_ROMAN,
    ARCUS,
    VESTA,
    ARCE,
    SOMNUS,
    // TITANS
    ATLAS,
    HYPERION,
    KOIOS,
    PROMETHEUS,
    STYX,
    OCEANUS,
    RHEA,
    MNEMOSYNE,
    // MYTHICALS:
    CYCLOPS,
    HARPY,
    DRYAD,
    SATYR,
    // FILLER:
    NONE;

    public static HeritageType fromString(String string) {
        return switch (string) {
            case "zeus" -> ZEUS;
            case "hades" -> HADES;
            case "poseidon" -> POSEIDON;
            case "demeter" -> DEMETER;
            case "ares" -> ARES;
            case "athena" -> ATHENA;
            case "apolloGreek" -> APOLLO_GREEK;
            case "artemis" -> ARTEMIS;
            case "hephaestus" -> HEPHAESTUS;
            case "aphrodite" -> APHRODITE;
            case "hermes" -> HERMES;
            case "dionysus" -> DIONYSUS;
            case "hecate" -> HECATE;
            case "khione" -> KHIONE;
            case "psycheGreek" -> PSYCHE_GREEK;
            case "iris" -> IRIS;
            case "arke" -> ARKE;
            case "hypnos" -> HYPNOS;
            case "jupiter" -> JUPITER;
            case "pluto" -> PLUTO;
            case "neptune" -> NEPTUNE;
            case "ceres" -> CERES;
            case "mars" -> MARS;
            case "apolloRoman" -> APOLLO_ROMAN;
            case "diana" -> DIANA;
            case "vulcan" -> VULCAN;
            case "venus" -> VENUS;
            case "mercury" -> MERCURY;
            case "bacchus" -> BACCHUS;
            case "janus" -> JANUS;
            case "bellona" -> BELLONA;
            case "chione" -> CHIONE;
            case "psycheRoman" -> PSYCHE_ROMAN;
            case "arcus" -> ARCUS;
            case "vesta" -> VESTA;
            case "arce" -> ARCE;
            case "somnus" -> SOMNUS;
            case "atlas" -> ATLAS;
            case "hyperion" -> HYPERION;
            case "koios" -> KOIOS;
            case "prometheus" -> PROMETHEUS;
            case "styx" -> STYX;
            case "oceanus" -> OCEANUS;
            case "rhea" -> RHEA;
            case "mnemosyne" -> MNEMOSYNE;
            case "cyclops" -> CYCLOPS;
            case "harpy" -> HARPY;
            case "dryad" -> DRYAD;
            case "satyr" -> SATYR;
            default -> NONE;
        };
    }

    public static String toString(HeritageType heritageType) {
        return switch (heritageType) {
            case ZEUS -> "zeus";
            case HADES -> "hades";
            case POSEIDON -> "poseidon";
            case DEMETER -> "demeter";
            case ARES -> "ares";
            case ATHENA -> "athena";
            case APOLLO_GREEK -> "apolloGreek";
            case ARTEMIS -> "artemis";
            case HEPHAESTUS -> "hephaestus";
            case APHRODITE -> "aphrodite";
            case HERMES -> "hermes";
            case DIONYSUS -> "dionysus";
            case HECATE -> "hecate";
            case KHIONE -> "khione";
            case PSYCHE_GREEK -> "psycheGreek";
            case IRIS -> "iris";
            case ARKE -> "arke";
            case HYPNOS -> "hypnos";


            case JUPITER -> "jupiter";
            case PLUTO -> "pluto";
            case NEPTUNE -> "neptune";
            case CERES -> "ceres";
            case MARS -> "mars";
            case APOLLO_ROMAN -> "apolloRoman";
            case DIANA -> "diana";
            case VULCAN -> "vulcan";
            case VENUS -> "venus";
            case MERCURY -> "mercury";
            case BACCHUS -> "bacchus";
            case JANUS -> "janus";
            case BELLONA -> "bellona";
            case CHIONE -> "chione";
            case PSYCHE_ROMAN -> "psycheRoman";
            case ARCUS -> "arcus";
            case VESTA -> "vesta";
            case ARCE -> "arce";
            case SOMNUS -> "somnus";


            case ATLAS -> "atlas";
            case HYPERION -> "hyperion";
            case KOIOS -> "koios";
            case PROMETHEUS -> "prometheus";
            case STYX -> "styx";
            case OCEANUS -> "oceanus";
            case RHEA -> "rhea";
            case MNEMOSYNE -> "mnemosyne";


            case CYCLOPS -> "cyclops";
            case HARPY -> "harpy";
            case DRYAD -> "dryad";
            case SATYR -> "satyr";
            default -> "none";
        };
    }
}
