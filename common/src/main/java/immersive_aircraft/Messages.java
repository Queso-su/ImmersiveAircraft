package immersive_aircraft;

import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.network.c2s.*;
import immersive_aircraft.network.s2c.*;

public class Messages {
    public static void loadMessages() {
        // 服务器到客户端的消息
        NetworkHandler.registerMessage(Main.MOD_ID, AircraftDataMessage.TYPE, AircraftDataMessage.STREAM_CODEC, NetworkHandler::handleDefault, null);
        NetworkHandler.registerMessage(Main.MOD_ID, FireResponse.TYPE, FireResponse.STREAM_CODEC, NetworkHandler::handleDefault, null);
        NetworkHandler.registerMessage(Main.MOD_ID, InventoryUpdateMessage.TYPE, InventoryUpdateMessage.STREAM_CODEC, NetworkHandler::handleDefault, null);
        NetworkHandler.registerMessage(Main.MOD_ID, OpenGuiRequest.TYPE, OpenGuiRequest.STREAM_CODEC, NetworkHandler::handleDefault, null);
        NetworkHandler.registerMessage(Main.MOD_ID, VehicleUpgradesMessage.TYPE, VehicleUpgradesMessage.STREAM_CODEC, NetworkHandler::handleDefault, null);

        // 客户端到服务器的消息
        NetworkHandler.registerMessage(Main.MOD_ID, CollisionMessage.TYPE, CollisionMessage.STREAM_CODEC, null, NetworkHandler::handleDefault);
        NetworkHandler.registerMessage(Main.MOD_ID, CommandMessage.TYPE, CommandMessage.STREAM_CODEC, null, NetworkHandler::handleDefault);
        NetworkHandler.registerMessage(Main.MOD_ID, EnginePowerMessage.TYPE, EnginePowerMessage.STREAM_CODEC, null, NetworkHandler::handleDefault);
        NetworkHandler.registerMessage(Main.MOD_ID, FireMessage.TYPE, FireMessage.STREAM_CODEC, null, NetworkHandler::handleDefault);
        NetworkHandler.registerMessage(Main.MOD_ID, InventoryRequest.TYPE, InventoryRequest.STREAM_CODEC, null, NetworkHandler::handleDefault);
    }
}