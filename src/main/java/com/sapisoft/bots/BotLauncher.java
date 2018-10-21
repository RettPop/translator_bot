package com.sapisoft.bots;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Created by rettpop on 2017-06-24.
 */
public class BotLauncher
{
    public BotLauncher()
    {
        ApiContextInitializer.init();
        TelegramBotsApi api = new TelegramBotsApi();
        try
        {
            api.registerBot(new Telegrammer());
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
        }
    }
}
