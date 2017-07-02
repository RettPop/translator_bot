package com.sapisoft.bots;

import com.sapisoft.secrets.ResourcesSecretsManager;
import com.sapisoft.secrets.SimpleSecret;
import com.sapisoft.translator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.sapisoft.azuretranslator.AzureTranslator;

import java.util.*;

import static com.sapisoft.bots.BotCommand.Commands.HELP;
import static com.sapisoft.bots.BotCommand.Commands.STATUS;

/**
 *
 */
public class Telegrammer extends TelegramLongPollingBot
{
	private static final Logger LOG = LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

	private static final long sourceChatId = -1001087417333L;
	private static final long targetChatId = -1001078341977L;
	private static final String SECRETS_GROUP = "com.sapisoft.bots.Translator";

	private String _apiKey;
	private String _botName;
	private final Translator _transl = new AzureTranslator();
	private final ResourcesSecretsManager _secretsManager = new ResourcesSecretsManager("/secrets/keys.json");
	private Map<Long, List<TranslationCommand>> _routing = new HashMap<>();

	public Telegrammer()
	{
		TranslationCommand command = TranslationCommand.createTranslation(targetChatId, Translation.DestinationTranslation(Locale.ENGLISH, ""));
		_routing.put(sourceChatId, Arrays.asList(command));
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
			LOG.debug("Error while registering bot: ", e);
		}
	}

	@Override
	public void onUpdateReceived(Update update)
	{
		BotCommand command = findCommand(update);
		if (null != command)
		{
			Message message = update.hasMessage() ? update.getMessage() : update.getChannelPost();
			executeCommand(command, message);
		}
		else
		{
			if (update.hasMessage() && update.getMessage().hasText())
			{
				User usr = update.getMessage().getFrom();
				Chat sourceChat = update.getMessage().getChat();
				LOG.info("Message arrived from channel: {} from user {}", sourceChat, usr);

				processPrivateMessage(update);
			}
			else if (update.hasChannelPost())
			{
				Message msg = update.getChannelPost();
				User usr = msg.getFrom();
				Chat sourceChat = msg.getChat();
				LOG.info("Message arrived from channel: {} from user {} ", sourceChat, usr);

				processChatMessage(update);
			}
		}
	}

	private void processChatMessage(Update update)
	{
		List<TranslationCommand> translationCommands = _routing.get(update.getChannelPost().getChatId());

		Message msg = update.getChannelPost();
		User usr = update.getChannelPost().getFrom();
		Chat sourceChat = update.getChannelPost().getChat();

		if (translationCommands == null)
		{
			LOG.info("No commands for source chat: {}", sourceChat.getTitle());
			return;
		}

		LOG.info("Message arrived from channel: {} : {} from user {}", sourceChat.getId(), sourceChat.getTitle(), usr);

		for (TranslationCommand oneCommand : translationCommands)
		{
			SendMessage message = new SendMessage()
					.setChatId(sourceChat.getId())
					.setText(msg.getText());

			Translation translation = Translation.SourceTranslation(oneCommand.translation().getSourceLocale(),
					oneCommand.translation().getDestinationLocale(),
					message.getText());
			Translation translatedMsg = _transl.translate(translation);

			if (!translatedMsg.getResultText().isEmpty())
			{
				String report = translatedMsg.getResultText()
						+ "\n<i>(" + translatedMsg.getSourceLocale().getLanguage() + "->" + translatedMsg.getDestinationLocale().getLanguage()
						+ " from \"" + update.getChannelPost().getChat().getTitle() + "\")</i>";

				LOG.info("Sending message to channel: {}", oneCommand.targetChannelId());
				sendTextToChat(report, oneCommand.targetChannelId());
			}
			else
			{
				LOG.info("Empty message translation arrived");
			}
		}
	}

	private void processPrivateMessage(Update update)
	{
		LOG.info("Private message arrived: {}", update.getMessage().getText());

		try
		{
			SendMessage message = new SendMessage()
					.setChatId("" + update.getMessage().getChatId())
					.setText(update.getMessage().getText());
			sendMessage(message);
		}
		catch (TelegramApiException e)
		{
			LOG.debug("Telegram API general error: ", e);
		}
	}


	private BotCommand findCommand(Update update)
	{
		Message updateMessage = update.hasMessage() ? update.getMessage() : update.getChannelPost();

		if(null == updateMessage)
		{
			LOG.debug("Received empty update");
			return null;
		}

		String commandText = updateMessage.getText();
		BotCommand botCommand = null;

		if(updateMessage.hasEntities())
		{
			for (MessageEntity entity: updateMessage.getEntities())
			{
				if("bot_command".equals(entity.getType()))
				{
					botCommand = parseCommand(commandText);
					LOG.info("Received command {}", entity);
				}
				else if("mention".equals(entity.getType()))
				{
					String strBeforeMent = commandText.substring(0, entity.getOffset());
					String strAfterMent = commandText.substring(entity.getOffset() + entity.getLength(),
							commandText.length());
					String mentioning = strBeforeMent + strAfterMent;
					LOG.info("Bot was mentioned with message: \"{}\"", mentioning);
					botCommand = BotCommand.NOPCommand();
				}
			}
		}

		return botCommand;
	}

	private void executeCommand(BotCommand command, Message updateMessage)
	{
		if(command.command() == HELP)
		{
			SendMessage message = new SendMessage()
					.setChatId("" + updateMessage.getChatId())
					.setText(updateMessage.getText());

			message.setText("Probably useless for you bot as it works with hardcoded channels as for now. But if you are inderested in what does it do anyway, it receives messages from one channel, translates them (with Microsoft Locale API) and sends to another one.");
			try
			{
				sendMessage(message);
			}
			catch (TelegramApiException e)
			{
				LOG.debug("Error while sending message: ", e);
			}
		}
		else if (command.command() == STATUS)
		{
			SendMessage message = new SendMessage()
					.setChatId("" + updateMessage.getChatId())
					.setText(updateMessage.getText());

			message.setText("Alive");
			try
			{
				sendMessage(message);
			}
			catch (TelegramApiException e)
			{
				LOG.debug("Error while sending message: ", e);
			}
		}
	}

	private BotCommand parseCommand(String commandText)
	{
		BotCommand command = BotCommand.NOPCommand();

		if(commandText.startsWith("/help"))
		{
			command = BotCommand.CreateCommand(HELP);
		}
		else if(commandText.startsWith("/status"))
		{
			command = BotCommand.CreateCommand(STATUS);
		}

		return command;
	}

	@Override
	public String getBotUsername()
	{
		if (_botName == null)
		{
			SimpleSecret botNameSecret = _secretsManager.getSecret("botName", SECRETS_GROUP);
			if (botNameSecret != null)
			{
				_botName = botNameSecret.getSecret();
			}
		}

		return _botName;
	}

	@Override
	public String getBotToken()
	{
		if (_apiKey == null)
		{
			SimpleSecret simpleSecret = _secretsManager.getSecret("botToken", SECRETS_GROUP);
			if (simpleSecret != null)
			{
				_apiKey = simpleSecret.getSecret();
			}
		}

		return _apiKey;
	}

	private void sendTextToChat(String text, Long chatId)
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
			LOG.debug("Error sending message to the channel: ", e);
		}
	}
}
