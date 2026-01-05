package immersive_aircraft.network;

import immersive_aircraft.network.s2c.FireResponse;
import immersive_aircraft.network.s2c.InventoryUpdateMessage;
import immersive_aircraft.network.s2c.OpenGuiRequest;

/**
 * Default message handler implementation used on the server side
 * to avoid null pointer exceptions when messageHandler is accessed
 */
public class DefaultMessageHandler implements MessageHandler {
    @Override
    public void handleOpenGuiRequest(OpenGuiRequest request) {
        // No server-side implementation needed for GUI requests
    }

    @Override
    public void handleInventoryUpdate(InventoryUpdateMessage message) {
        // No server-side implementation needed for inventory updates
    }

    @Override
    public void handleFire(FireResponse fireResponse) {
        // No server-side implementation needed for fire particles
    }
}