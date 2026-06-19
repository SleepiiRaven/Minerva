package net.minervamc.minerva.quest.listeners;

import java.util.ArrayList;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.minervamc.minerva.Minerva;
import net.minervamc.minerva.quest.ActiveQuest;
import net.minervamc.minerva.quest.QuestManager;
import net.minervamc.minerva.quest.steps.DeliverItemStep;
import net.minervamc.minerva.quest.steps.QuestStep;
import net.minervamc.minerva.quest.steps.StepContext;
import net.minervamc.minerva.quest.steps.TalkToNpcStep;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Citizens-only listener (registered only when Citizens is present). Routes NPC right-clicks to editor NPC
 * binding and the active quest's talk/deliver step. Quests are NOT started here — admins wire a Citizens
 * command (e.g. "/quests start &lt;id&gt;") to the NPC so starting stays under their control.
 */
public class QuestNpcListener implements Listener {

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        int npcId = event.getNPC().getId();

        // 1) An editor is waiting for the admin to pick an NPC.
        if (QuestManager.consumeNpcBind(player, npcId)) return;

        // 2) Any active quest whose current step targets this NPC handles the click (copy: completing advances).
        for (ActiveQuest active : new ArrayList<>(QuestManager.getActives(player))) {
            QuestStep step = active.currentStep();
            StepContext ctx = new StepContext(player, active, Minerva.getInstance());
            if (step instanceof TalkToNpcStep talk && talk.getNpcId() == npcId) {
                talk.handleTalk(npcId, ctx);
                return;
            } else if (step instanceof DeliverItemStep deliver && deliver.getNpcId() == npcId) {
                deliver.handleDeliver(npcId, ctx);
                return;
            }
        }
    }
}
