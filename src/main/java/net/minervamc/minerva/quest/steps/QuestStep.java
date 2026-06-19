package net.minervamc.minerva.quest.steps;

import com.google.gson.JsonObject;
import java.util.List;
import net.minervamc.minerva.quest.Quest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The modular contract every quest step implements. Adding a new step type only requires:
 * <ol>
 *     <li>a new {@code QuestStep} subclass implementing these methods,</li>
 *     <li>one constant in {@link StepType} pointing at the subclass's no-arg constructor,</li>
 *     <li>(if event-driven) one dispatch branch in the quest listeners.</li>
 * </ol>
 * There are no switch statements over step types anywhere else — {@link StepType} is the single registry.
 *
 * <p>Completion is push-based: a step calls {@link StepContext#complete()} when it is done; the engine
 * then {@link #cleanup(StepContext)}s it and begins the next step. Time/poll steps own their own
 * {@code BukkitRunnable}; event-driven steps are dispatched to from the central quest listeners.</p>
 */
public abstract class QuestStep {

    /** @return the registry constant for this step (used for (de)serialization and the GUI picker). */
    public abstract StepType getType();

    /** @return the icon shown in the builder GUI for this step. */
    public abstract ItemStack getIcon();

    /** @return a short one-line summary shown in the step list. */
    public abstract String getEditorTitle();

    /** @return extra detail lines shown under the title in the step list. */
    public List<String> getEditorLore() {
        return List.of();
    }

    /** @return a player-facing objective line for the quest journal (with live counters where relevant). */
    public String statusLine(Player player) {
        return getEditorTitle();
    }

    // ---- Persistence of the DEFINITION (shared, immutable at runtime) ----
    public abstract void writeJson(JsonObject o);

    public abstract void readJson(JsonObject o);

    // ---- Persistence of RUNTIME STATE (per-player counters, for resume). Default: nothing. ----
    public void writeState(JsonObject o) {
    }

    public void readState(JsonObject o) {
    }

    // ---- Lifecycle ----
    /** Arm listeners / give items / start runnables. Must be safe to call again on login (resume). */
    public abstract void begin(StepContext ctx);

    /** Unregister tasks/entities. Called when the step completes or the player logs out. */
    public void cleanup(StepContext ctx) {
    }

    // ---- Authoring ----
    /** Open the in-game editor for this step. {@code back} reopens the step list when the admin is done. */
    public abstract void openEditor(Player admin, Quest quest, Runnable back);

    /** Deep-copy just the definition (no runtime counters) into a fresh instance for one player. */
    public QuestStep copyDefinition() {
        QuestStep copy = getType().create();
        JsonObject o = new JsonObject();
        this.writeJson(o);
        copy.readJson(o);
        return copy;
    }
}
