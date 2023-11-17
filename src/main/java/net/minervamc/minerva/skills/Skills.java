package net.minervamc.minerva.skills;

import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import net.minervamc.minerva.skills.greek.hades.ChannelingOfTartarus;
import net.minervamc.minerva.skills.greek.hades.LifeSteal;
import net.minervamc.minerva.skills.greek.hades.ShadowTravel;
import net.minervamc.minerva.skills.greek.hades.SkeletalHands;
import net.minervamc.minerva.skills.greek.hades.UmbrakinesisHades;
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
    Skill DEFAULT_PASSIVE = new DefaultSkill();
    Skill[] PASSIVES = {
            LIFE_STEAL,
            OCEANS_EMBRACE,
            PROTECTIVE_CLOUD,
            DEFAULT_PASSIVE
    };
    Skill DEFAULT = new DefaultSkill();
}
