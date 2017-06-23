package com.sapisoft.bots;

import com.sapisoft.secrets.ResourcesSecretsManager;
import com.sapisoft.secrets.SimpleSecret;
import com.sapisoft.translator.Translation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.sapisoft.azuretranslator.AzureTranslator;
import com.sapisoft.translator.Translator;

import java.util.*;

/**
 * Created by eviazhe on 2017-05-24.
 */
public class Telegrammer extends TelegramLongPollingBot
{
    private static final Logger LOG = LoggerFactory.getLogger(Telegrammer.class);

    private static final long sourceChatId = -1001087417333l;
    private static final long targetChatId = -1001078341977l;
    private static final String SECRETS_GROUP = "com.sapisoft.bots.Translator";

    private String _apiKey;
    private String _botName;
    private final Translator _transl = new AzureTranslator();
    private final ResourcesSecretsManager _secretsManager = new ResourcesSecretsManager("keys.json");
    private Map<Long, List<Long>> _routing = new HashMap<>();

    public Telegrammer()
    {
        _routing.put(sourceChatId, Arrays.asList(targetChatId));
    }

    public static void main(String[] args)
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

    @Override
    public void onUpdateReceived(Update update)
    {
        if(update.hasMessage() && update.getMessage().hasText())
        {
            processPrivateMessage(update);
        }
        else if(update.hasChannelPost())
        {
            processChatMessage(update);
        }
    }

    private void processChatMessage(Update update)
    {
        List<Long> targetChannels = _routing.get(update.getChannelPost().getChatId());

        if(targetChannels == null)
        {
            LOG.info("Unknown source chat: {}", update.getChannelPost().getChat().getTitle());
            return;
        }

        LOG.info("Message arrived from channel: {} : {}", update.getChannelPost().getChat().getId(), update.getChannelPost().getChat().getTitle());
        Message msg = update.getChannelPost();
        User usr = msg.getFrom();
        SendMessage message = new SendMessage()
                .setChatId(msg.getChat().getId())
                .setText(msg.getText());

        Translation translation = Translation.SourceTranslation(Locale.forLanguageTag("sv"),
                Locale.forLanguageTag("en"),
                message.getText());
        Translation translatedMsg = _transl.translate(translation);
        if(!translatedMsg.getResultText().isEmpty())
        {
            String report = translatedMsg.getResultText()
                    + "\n<i>(" + translatedMsg.getSourceLocale().getLanguage() + "->" + translatedMsg.getDestinationLocale().getLanguage()
                    + " from \"" + update.getChannelPost().getChat().getTitle() + "\")</i>";

            for (Long oneTarget : targetChannels)
            {
                LOG.info("Sending message to channel: {}", oneTarget);
                sendTextToChat(report, Long.toString(oneTarget));
            }
        }
        else
        {
            LOG.info("Empty message arrived");
        }
    }

    private void processPrivateMessage(Update update)
    {
        LOG.info("Private message arrived: {}", update.getMessage().getText());

        SendMessage message = new SendMessage()
                .setChatId("" + update.getMessage().getChatId())
                .setText(update.getMessage().getText());
        try
        {
            sendMessage(message);
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername()
    {
        if(_botName == null)
        {
            SimpleSecret botNameSecret = _secretsManager.getSecret("botName", SECRETS_GROUP);
            if (botNameSecret != null) {
                _botName = botNameSecret.getSecret();
            }
        }

        return _botName;
    }

    @Override
    public String getBotToken()
    {
        if(_apiKey == null)
        {
            SimpleSecret simpleSecret = _secretsManager.getSecret("botToken", SECRETS_GROUP);
            if(simpleSecret != null)
            {
                _apiKey = simpleSecret.getSecret();
            }
        }

        return _apiKey;
    }

    private void sendTextToChat(String text, String chatId)
    {
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text)
                .setParseMode("HTML");
        try
        {
            sendMessage(message);
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
        }
    }
}
