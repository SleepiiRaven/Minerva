package net.minervamc.minerva.skills;

import net.minervamc.minerva.skills.greek.aphrodite.Doves;
import net.minervamc.minerva.skills.greek.aphrodite.Serenity;
import net.minervamc.minerva.skills.greek.aphrodite.Charm;
import net.minervamc.minerva.skills.greek.aphrodite.HeartSeeker;
import net.minervamc.minerva.skills.greek.aphrodite.MirrorImage;
import net.minervamc.minerva.skills.greek.apollo.ApollosHymn;
import net.minervamc.minerva.skills.greek.apollo.ArrowsOfTheSun;
import net.minervamc.minerva.skills.greek.apollo.BurningLight;
import net.minervamc.minerva.skills.greek.apollo.EnhancedArchery;
import net.minervamc.minerva.skills.greek.apollo.PlagueVolley;
import net.minervamc.minerva.skills.greek.ares.AresBlessing;
import net.minervamc.minerva.skills.greek.ares.Cleave;
import net.minervamc.minerva.skills.greek.ares.PrimalScream;
import net.minervamc.minerva.skills.greek.ares.SpiritOfVengeance;
import net.minervamc.minerva.skills.greek.ares.TomahawkThrow;
import net.minervamc.minerva.skills.greek.artemis.CallOfTheWild;
import net.minervamc.minerva.skills.greek.artemis.HuntressAgility;
import net.minervamc.minerva.skills.greek.artemis.NimbleDash;
import net.minervamc.minerva.skills.greek.artemis.Sharpshooter;
import net.minervamc.minerva.skills.greek.artemis.SuperCharged;
import net.minervamc.minerva.skills.greek.athena.AegisRush;
import net.minervamc.minerva.skills.greek.athena.TacticalAgility;
import net.minervamc.minerva.skills.greek.athena.Parry;
import net.minervamc.minerva.skills.greek.athena.SpearOfAthena;
import net.minervamc.minerva.skills.greek.athena.SpearRain;
import net.minervamc.minerva.skills.greek.demeter.HarvestBlessing;
import net.minervamc.minerva.skills.greek.demeter.HarvestWrath;
import net.minervamc.minerva.skills.greek.demeter.RootSurge;
import net.minervamc.minerva.skills.greek.demeter.VineGrapple;
import net.minervamc.minerva.skills.greek.demeter.SeedBarrage;
import net.minervamc.minerva.skills.greek.demeter.ThornBlight;
import net.minervamc.minerva.skills.greek.dionysus.DrunkenRevelry;
import net.minervamc.minerva.skills.greek.dionysus.FrenziedDance;
import net.minervamc.minerva.skills.greek.dionysus.GrapeShot;
import net.minervamc.minerva.skills.greek.dionysus.MadGodsDrink;
import net.minervamc.minerva.skills.greek.dionysus.VineWhip;
import net.minervamc.minerva.skills.greek.hades.ChannelingOfTartarus;
import net.minervamc.minerva.skills.greek.hades.LifeSteal;
import net.minervamc.minerva.skills.greek.hades.ShadowTravel;
import net.minervamc.minerva.skills.greek.hades.SkeletalHands;
import net.minervamc.minerva.skills.greek.hades.UmbrakinesisHades;
import net.minervamc.minerva.skills.greek.hephaestus.GroundBreaker;
import net.minervamc.minerva.skills.greek.hephaestus.LivingForge;
import net.minervamc.minerva.skills.greek.hephaestus.Magmatism;
import net.minervamc.minerva.skills.greek.hephaestus.ShrapnelGrenade;
import net.minervamc.minerva.skills.greek.hephaestus.Smolder;
import net.minervamc.minerva.skills.greek.hermes.CaduceusArc;
import net.minervamc.minerva.skills.greek.hermes.FleetFootwork;
import net.minervamc.minerva.skills.greek.hermes.KineticDispatch;
import net.minervamc.minerva.skills.greek.hermes.MessengerWake;
import net.minervamc.minerva.skills.greek.hermes.TalariaStep;
import net.minervamc.minerva.skills.greek.poseidon.AquaticLimbExtensions;
import net.minervamc.minerva.skills.greek.poseidon.OceansEmbrace;
import net.minervamc.minerva.skills.greek.poseidon.OceansSurge;
import net.minervamc.minerva.skills.greek.poseidon.SeismicBlast;
import net.minervamc.minerva.skills.greek.poseidon.TidalWave;
import net.minervamc.minerva.skills.greek.zeus.LightningToss;
import net.minervamc.minerva.skills.greek.zeus.ProtectiveCloud;
import net.minervamc.minerva.skills.greek.zeus.Soar;
import net.minervamc.minerva.skills.greek.zeus.StormsEmbrace;
import net.minervamc.minerva.skills.greek.zeus.WindWall;
import net.minervamc.minerva.skills.greek.khione.ColdSnap;
import net.minervamc.minerva.skills.greek.khione.Frostbite;
import net.minervamc.minerva.skills.greek.khione.GlacialGlide;
import net.minervamc.minerva.skills.greek.khione.RimeNova;
import net.minervamc.minerva.skills.greek.khione.Shatterpoint;
import net.minervamc.minerva.skills.greek.bellona.AdvanceTheLine;
import net.minervamc.minerva.skills.greek.bellona.Cannonade;
import net.minervamc.minerva.skills.greek.bellona.PlantWarBanner;
import net.minervamc.minerva.skills.greek.bellona.StandardsCall;
import net.minervamc.minerva.skills.greek.bellona.WarFooting;
import net.minervamc.minerva.skills.greek.hestia.BankedEmbers;
import net.minervamc.minerva.skills.greek.hestia.EmberVeil;
import net.minervamc.minerva.skills.greek.hestia.PyreRelease;
import net.minervamc.minerva.skills.greek.hestia.TendTheHearth;
import net.minervamc.minerva.skills.greek.hestia.VestasVeil;
import net.minervamc.minerva.skills.greek.hecate.CrossroadsTorches;
import net.minervamc.minerva.skills.greek.hecate.ShiftFace;
import net.minervamc.minerva.skills.greek.hecate.SpectralHex;
import net.minervamc.minerva.skills.greek.hecate.TripleGoddess;
import net.minervamc.minerva.skills.greek.hecate.WitchingHour;
import net.minervamc.minerva.skills.greek.psyche.Chrysalis;
import net.minervamc.minerva.skills.greek.psyche.IridescentSoul;
import net.minervamc.minerva.skills.greek.psyche.ReleaseAnima;
import net.minervamc.minerva.skills.greek.psyche.SoulLance;
import net.minervamc.minerva.skills.greek.psyche.SoulThread;
import net.minervamc.minerva.skills.greek.janus.Doorway;
import net.minervamc.minerva.skills.greek.janus.GodOfTransitions;
import net.minervamc.minerva.skills.greek.janus.Reversal;
import net.minervamc.minerva.skills.greek.janus.Trespass;
import net.minervamc.minerva.skills.greek.janus.TwoFacedStrike;
import net.minervamc.minerva.skills.greek.iris.ChromaticBurst;
import net.minervamc.minerva.skills.greek.iris.IrisFlight;
import net.minervamc.minerva.skills.greek.iris.Prism;
import net.minervamc.minerva.skills.greek.iris.RefractionLance;
import net.minervamc.minerva.skills.greek.iris.SpectrumShift;
import net.minervamc.minerva.skills.greek.arke.CastChains;
import net.minervamc.minerva.skills.greek.arke.CinchTheChains;
import net.minervamc.minerva.skills.greek.arke.Reel;
import net.minervamc.minerva.skills.greek.arke.SharedFate;
import net.minervamc.minerva.skills.greek.arke.TarnishedLash;
import net.minervamc.minerva.skills.greek.hypnos.Dreamdrift;
import net.minervamc.minerva.skills.greek.hypnos.Lullaby;
import net.minervamc.minerva.skills.greek.hypnos.Nightmare;
import net.minervamc.minerva.skills.greek.hypnos.Sandman;
import net.minervamc.minerva.skills.greek.hypnos.VeilOfSomnus;
import net.minervamc.minerva.skills.greek.nemesis.Brace;
import net.minervamc.minerva.skills.greek.nemesis.CollectTheDebt;
import net.minervamc.minerva.skills.greek.nemesis.LedgerOfWrongs;
import net.minervamc.minerva.skills.greek.nemesis.MarkOfHubris;
import net.minervamc.minerva.skills.greek.nemesis.ScalesOfBalance;
import net.minervamc.minerva.skills.greek.persephone.Bloom;
import net.minervamc.minerva.skills.greek.persephone.Descent;
import net.minervamc.minerva.skills.greek.persephone.QueensDecree;
import net.minervamc.minerva.skills.greek.persephone.SeedsOfTheUnderworld;
import net.minervamc.minerva.skills.greek.persephone.Wither;
import net.minervamc.minerva.skills.greek.thanatos.Knell;
import net.minervamc.minerva.skills.greek.thanatos.ScytheOfLetus;
import net.minervamc.minerva.skills.greek.thanatos.ShroudOfLetus;
import net.minervamc.minerva.skills.greek.thanatos.TheInevitable;
import net.minervamc.minerva.skills.greek.thanatos.TollTheBell;
import net.minervamc.minerva.types.Skill;

public interface Skills {
    Skill SHADOW_TRAVEL = new ShadowTravel();
    Skill SKELETAL_HANDS = new SkeletalHands();
    Skill UMBRAKINESIS_HADES = new UmbrakinesisHades();
    Skill CHANNELING_OF_TARTARUS = new ChannelingOfTartarus();
    Skill WIND_WALL = new WindWall();
    Skill SOAR = new Soar();
    Skill LIGHTNING_TOSS = new LightningToss();
    Skill STORMS_EMBRACE = new StormsEmbrace();
    Skill SEISMIC_BLAST = new SeismicBlast();
    Skill OCEANS_SURGE = new OceansSurge();
    Skill TIDAL_WAVE = new TidalWave();
    Skill AQUATIC_LIMB_EXTENSIONS = new AquaticLimbExtensions();
    Skill LIFE_STEAL = new LifeSteal();
    Skill OCEANS_EMBRACE = new OceansEmbrace();
    Skill PROTECTIVE_CLOUD = new ProtectiveCloud();
    Skill APOLLOS_HYMN = new ApollosHymn();
    Skill ARROWS_OF_THE_SUN = new ArrowsOfTheSun();
    Skill BURNING_LIGHT = new BurningLight();
    Skill PLAGUE_VOLLEY = new PlagueVolley();
    Skill ENHANCED_ARCHERY = new EnhancedArchery();
    Skill DEFAULT_PASSIVE = new DefaultSkill();
    Skill CALL_OF_THE_WILD = new CallOfTheWild();
    Skill HUNTRESS_AGILITY = new HuntressAgility();
    Skill NIMBLE_DASH = new NimbleDash();
    Skill SHARPSHOOTER = new Sharpshooter();
    Skill SUPER_CHARGED = new SuperCharged();
    Skill DRUNKEN_REVELRY = new DrunkenRevelry();
    Skill FRENZIED_DANCE = new FrenziedDance();
    Skill GRAPE_SHOT = new GrapeShot();
    Skill VINE_WHIP = new VineWhip();
    Skill MAD_GODS_DRINK = new MadGodsDrink();
    Skill ARES_BLESSING = new AresBlessing();
    Skill SPIRIT_OF_VENGEANCE = new SpiritOfVengeance();
    Skill CLEAVE = new Cleave();
    Skill PRIMAL_SCREAM = new PrimalScream();
    Skill TOMAHAWK_THROW = new TomahawkThrow();
    Skill GROUND_BREAKER = new GroundBreaker();
    Skill LIVING_FORGE = new LivingForge();
    Skill MAGMATISM = new Magmatism();
    Skill SHRAPNEL_GRENADE = new ShrapnelGrenade();
    Skill SMOLDER = new Smolder();
    Skill SERENITY = new Serenity();
    Skill CHARM = new Charm();
    Skill HEART_SEEKER = new HeartSeeker();
    Skill MIRROR_IMAGE = new MirrorImage();
    Skill DOVES = new Doves();
    Skill PARRY = new Parry();
    Skill SPEAR_OF_ATHENA = new SpearOfAthena();
    Skill AEGIS_RUSH = new AegisRush();
    Skill SPEAR_RAIN = new SpearRain();
    Skill TACTICAL_AGILITY = new TacticalAgility();
    Skill ROOT_SURGE = new RootSurge();
    Skill SEED_BARRAGE = new SeedBarrage();
    Skill THORN_BLIGHT = new ThornBlight();
    Skill HARVEST_WRATH = new HarvestWrath();
    Skill HARVEST_BLESSING = new HarvestBlessing();
    Skill VINE_GRAPPLE = new VineGrapple();
    Skill TALARIA_STEP = new TalariaStep();
    Skill CADUCEUS_ARC = new CaduceusArc();
    Skill MESSENGER_WAKE = new MessengerWake();
    Skill KINETIC_DISPATCH = new KineticDispatch();
    Skill FLEET_FOOTWORK = new FleetFootwork();

    // Khione — Freeze & Shatter
    Skill GLACIAL_GLIDE = new GlacialGlide();
    Skill RIME_NOVA = new RimeNova();
    Skill SHATTERPOINT = new Shatterpoint();
    Skill COLD_SNAP = new ColdSnap();
    Skill FROSTBITE = new Frostbite();

    // Bellona — Banners as Weapons
    Skill ADVANCE_THE_LINE = new AdvanceTheLine();
    Skill PLANT_WAR_BANNER = new PlantWarBanner();
    Skill CANNONADE = new Cannonade();
    Skill STANDARDS_CALL = new StandardsCall();
    Skill WAR_FOOTING = new WarFooting();

    // Hestia — Hearthfire Bank
    Skill EMBER_VEIL = new EmberVeil();
    Skill TEND_THE_HEARTH = new TendTheHearth();
    Skill PYRE_RELEASE = new PyreRelease();
    Skill VESTAS_VEIL = new VestasVeil();
    Skill BANKED_EMBERS = new BankedEmbers();

    // Hecate — Three Faces
    Skill SHIFT_FACE = new ShiftFace();
    Skill CROSSROADS_TORCHES = new CrossroadsTorches();
    Skill SPECTRAL_HEX = new SpectralHex();
    Skill WITCHING_HOUR = new WitchingHour();
    Skill TRIPLE_GODDESS = new TripleGoddess();

    // Psyche — Anima Recall
    Skill RELEASE_ANIMA = new ReleaseAnima();
    Skill SOUL_THREAD = new SoulThread();
    Skill SOUL_LANCE = new SoulLance();
    Skill CHRYSALIS = new Chrysalis();
    Skill IRIDESCENT_SOUL = new IridescentSoul();

    // Janus — Threshold Portals
    Skill DOORWAY = new Doorway();
    Skill REVERSAL = new Reversal();
    Skill TWO_FACED_STRIKE = new TwoFacedStrike();
    Skill TRESPASS = new Trespass();
    Skill GOD_OF_TRANSITIONS = new GodOfTransitions();

    // Iris — Chromatic Contrast
    Skill IRIS_FLIGHT = new IrisFlight();
    Skill SPECTRUM_SHIFT = new SpectrumShift();
    Skill REFRACTION_LANCE = new RefractionLance();
    Skill CHROMATIC_BURST = new ChromaticBurst();
    Skill PRISM = new Prism();

    // Arke — Chains of Tartarus
    Skill REEL = new Reel();
    Skill CAST_CHAINS = new CastChains();
    Skill TARNISHED_LASH = new TarnishedLash();
    Skill CINCH_THE_CHAINS = new CinchTheChains();
    Skill SHARED_FATE = new SharedFate();

    // Hypnos — Contagious Sleep
    Skill DREAMDRIFT = new Dreamdrift();
    Skill LULLABY = new Lullaby();
    Skill NIGHTMARE = new Nightmare();
    Skill VEIL_OF_SOMNUS = new VeilOfSomnus();
    Skill SANDMAN = new Sandman();

    // Nemesis — Vengeance Ledger
    Skill BRACE = new Brace();
    Skill SCALES_OF_BALANCE = new ScalesOfBalance();
    Skill COLLECT_THE_DEBT = new CollectTheDebt();
    Skill MARK_OF_HUBRIS = new MarkOfHubris();
    Skill LEDGER_OF_WRONGS = new LedgerOfWrongs();

    // Persephone — Pomegranate Seeds
    Skill DESCENT = new Descent();
    Skill BLOOM = new Bloom();
    Skill WITHER = new Wither();
    Skill QUEENS_DECREE = new QueensDecree();
    Skill SEEDS_OF_THE_UNDERWORLD = new SeedsOfTheUnderworld();

    // Thanatos — Death Timers / Reaping
    Skill SHROUD_OF_LETUS = new ShroudOfLetus();
    Skill KNELL = new Knell();
    Skill SCYTHE_OF_LETUS = new ScytheOfLetus();
    Skill TOLL_THE_BELL = new TollTheBell();
    Skill THE_INEVITABLE = new TheInevitable();

    Skill DEFAULT = new DefaultSkill();
}
