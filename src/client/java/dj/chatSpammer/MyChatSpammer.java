package dj.chatspammer;

import net.fabricmc.api.ClientModInitializer;

public class MyChatSpammer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ChatCommand.register();
    }
}
