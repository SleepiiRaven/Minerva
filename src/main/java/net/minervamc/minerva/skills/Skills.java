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
    Skill DEFAULT = new DefaultSkill();
}
