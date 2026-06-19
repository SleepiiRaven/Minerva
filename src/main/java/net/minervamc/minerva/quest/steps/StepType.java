package net.minervamc.minerva.quest.steps;

import java.util.function.Supplier;
import org.bukkit.Material;

/**
 * The single registry of step types. To add a new step type: write a {@link QuestStep} subclass and add one
 * constant here pointing at its no-arg constructor. The GUI picker iterates {@link #values()} and the JSON
 * (de)serializer reconstructs steps via {@link #valueOf(String)} + {@link #create()} — so there are no
 * step-type switch statements anywhere else.
 */
public enum StepType {
    RECEIVE_ITEM("Receive Item / Reward", Material.CHEST, ReceiveItemStep::new),
    WALK_TO("Walk to Location", Material.LEATHER_BOOTS, WalkToStep::new),
    TALK_TO_NPC("Talk to NPC", Material.VILLAGER_SPAWN_EGG, TalkToNpcStep::new),
    CUTSCENE("Cutscene", Material.PAINTING, CutsceneStep::new),
    KILL_MOBS("Kill Mobs", Material.DIAMOND_SWORD, KillMobsStep::new),
    COLLECT_ITEM("Collect Item", Material.HOPPER_MINECART, CollectItemStep::new),
    DELIVER_ITEM("Deliver Item", Material.CHEST_MINECART, DeliverItemStep::new),
    WAIT("Wait / Delay", Material.CLOCK, WaitStep::new),
    RUN_COMMAND("Run Command", Material.COMMAND_BLOCK, RunCommandStep::new),
    SET_FLAG("Set Flag", Material.LEVER, SetFlagStep::new);

    private final String displayName;
    private final Material icon;
    private final Supplier<QuestStep> factory;

    StepType(String displayName, Material icon, Supplier<QuestStep> factory) {
        this.displayName = displayName;
        this.icon = icon;
        this.factory = factory;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public QuestStep create() {
        return factory.get();
    }
}
