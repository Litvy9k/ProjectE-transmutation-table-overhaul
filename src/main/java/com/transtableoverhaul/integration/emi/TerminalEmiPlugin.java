package com.transtableoverhaul.integration.emi;

import com.transtableoverhaul.client.screen.TransmutationTerminalScreen;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;

@EmiEntrypoint
public class TerminalEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addExclusionArea(TransmutationTerminalScreen.class, (screen, consumer) ->
                screen.getExtraAreas().forEach(area -> consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()))));
        registry.addStackProvider(TransmutationTerminalScreen.class, (screen, mouseX, mouseY) -> {
            TransmutationTerminalScreen.GridHit hit = screen.getGridHit(mouseX, mouseY);
            return hit == null ? EmiStackInteraction.EMPTY : new EmiStackInteraction(EmiStack.of(hit.stack()), null, false);
        });
    }
}
