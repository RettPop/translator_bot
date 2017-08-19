package com.sapisoft.bots;

import com.sapisoft.azuretranslator.AzureTranslator;
import com.sapisoft.config.FileConfigManager;
import com.sapisoft.secrets.ResourcesSecretsManager;
import com.sapisoft.secrets.SimpleSecret;
import com.sapisoft.stats.Counter;
import com.sapisoft.stats.FileCountersManager;
import com.sapisoft.translator.Translation;
import com.sapisoft.translator.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sapisoft.bots.BotCommand.Commands.*;

/**
 *
 */
public class Telegrammer extends TelegramLongPollingBot
{
	private static final Logger LOG = LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
	private static final String CLASS_VERSION = new Object(){}.getClass().getPackage().getImplementationVersion();

	private static final String SECRETS_GROUP = "com.sapisoft.bots.Translator";
	private static final String SECRETS_GROUP_TEST = "com.sapisoft.bots.Translator.test";

	private String _apiKey;
	private String _botName;
	private final Translator _transl = new AzureTranslator();
	private final FileConfigManager _confManager = new FileConfigManager("/config/config.json");
	private final ResourcesSecretsManager _secretsManager = new ResourcesSecretsManager("/secrets/keys.json");
	private Map<Long, List<TranslationCommand>> _routing = new HashMap<>();
	private final FileCountersManager _countsManager;
	private Counter _counterTranslates = Counter.fromString("translations.Number");
	private Counter _counterTotalChars = Counter.fromString("translations.Characters");
	private Counter _counterMsgLength = Counter.fromString("translations.MessageLength");
	private Counter _counterTranslErrors = Counter.fromString("translations.Error");

	public Telegrammer()
	{
		Long sourceChatId = _confManager.getLongValue("sourceChat", "defaultRouting");
		Long targetChatId = _confManager.getLongValue("destinationChat", "defaultRouting");
		TranslationCommand command = TranslationCommand.createTranslation(targetChatId, Translation.SourceTranslation(Translator.SWEDISH, Locale.ENGLISH, ""));
		_routing.put(sourceChatId, Collections.singletonList(command));

		String countersFile = _confManager.getOption("countersFile", "statistics");
		String countersDir = _confManager.getOption("countersDir", "statistics");
		_countsManager = new FileCountersManager(countersFile, countersDir);

		LOG.info("Starting v.{}", CLASS_VERSION);
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
		LOG.debug("Update arrived: {}", update);
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

	private List<TranslationCommand> translationCommandsForChat(Chat chat)
	{
		return _routing.get(chat.getId());
	}

	private void processChatMessage(Update update)
	{
		List<TranslationCommand> translationCommands = translationCommandsForChat(update.getChannelPost().getChat());

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
				_countsManager.changeCounterValue(_counterTranslates, 1);
				_countsManager.changeCounterValue(_counterTotalChars, msg.getText().length());
				_countsManager.setCounterValue(_counterMsgLength, msg.getText().length());
				_countsManager.changeCounterValue(Counter.fromString("translations.FromChat." + sourceChat.getId()), 1);

				String report = translatedMsg.getResultText()
						+ "\n<i>(" + translatedMsg.getSourceLocale().getLanguage() + "->" + translatedMsg.getDestinationLocale().getLanguage()
						+ " from \"" + update.getChannelPost().getChat().getTitle() + "\")</i>";

				LOG.info("Sending message to channel: {}", oneCommand.targetChannelId());
				sendTextToChat(report, oneCommand.targetChannelId());
			}
			else
			{
				_countsManager.changeCounterValue(_counterTranslErrors, 1);
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
			execute(message);
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
		switch (command.command())
		{
			case HELP:
			{
				String response = "Probably useless for you bot as it works with hardcoded channels as for now. But if you are inderested in what does it do anyway, it receives messages from one channel, translates them (with Microsoft Locale API) and sends to another one.";
				sendTextToChat(response, updateMessage.getChatId());
				break;
			}
			case STATUS:
			{
				sendTextToChat("Alive", updateMessage.getChatId());
				break;
			}
			case COUNTERS:
			{
				executeCommandCounters(updateMessage);
				break;
			}
			case COUNTERDELTA:
			{
				executeCommandCounterdelta(command, updateMessage);
				break;
			}
			case TRANSLATE_TO:
			case TRANSLATE_FROM_TO:
			{
				executeCommandTranslate(command, updateMessage);
				break;
			}
			case LANGUAGES:
				executeCommandLanguages(updateMessage);
				break;
			case NOTFULL:
			{
				sendTextToChat("Command is not properly configured", updateMessage.getChatId());
				break;
			}
			default:
			{
				sendTextToChat("Unknown command", updateMessage.getChatId());
			}
		}
	}

	private void executeCommandCounterdelta(BotCommand command, Message updateMessage)
	{
		List<String> permittedChats = _confManager.getValuesArray("counters.show", "permissions");
		if(updateMessage.getFrom() != null)
		{
			Integer userId = updateMessage.getFrom().getId();
			if(!permittedChats.contains(Integer.toString(userId)))
			{
				sendTextToChat("You are not allowed to view counters", updateMessage.getChatId());
				return;
			}
		}

		Counter counter = Counter.fromString(command.parameters().get("counter"));
		counter = _countsManager.getCounter(counter);

		if(null == counter)
		{
			sendTextToChat("Unknown counter", updateMessage.getChatId());
			return;
		}

		String periodName = command.parameters().get("period");
		Date lastDate = new Date();
		Date startDate = null;
		if("month".equals(periodName))
		{
			startDate = Date.from(LocalDate.now()
					.atStartOfDay()
					.withDayOfMonth(1)
					.atZone(ZoneId.systemDefault())
					.toInstant());
		}

		List<Counter> counterStates = _countsManager.getCounterStatesForPeriod(counter, startDate, lastDate);
		if(counterStates.size() < 2)
		{
			sendTextToChat("Not enough values to calculate delta", updateMessage.getChatId());
			return;
		}

		float delta = counterStates.get(counterStates.size() - 1).getCounterValue() - counterStates.get(0).getCounterValue();
		sendTextToChat("Counter " + counter.name() + " delta since beginning of month is: " + delta, updateMessage.getChatId());
	}

	private void executeCommandLanguages(Message updateMessage)
	{
		List<Locale> supportedLanguages = _transl.supportedLanguages();
		if(supportedLanguages == null)
		{
			sendTextToChat("Error retrieving supported languages list", updateMessage.getChatId());
		}
		else
		{

			StringBuilder languages = new StringBuilder("List of supported languages:\n");
			for (Locale oneLocale : supportedLanguages)
			{
				languages.append(oneLocale.getLanguage())
						.append(" - ")
						.append(oneLocale.getVariant())
						.append("\n");
			}
			sendTextToChat(languages.toString(), updateMessage.getChatId());
		}
	}

	private void executeCommandCounters(Message updateMessage)
	{
		List<String> permittedChats = _confManager.getValuesArray("counters.show", "permissions");
		if(updateMessage.getFrom() != null)
		{
			Integer userId = updateMessage.getFrom().getId();
			if(!permittedChats.contains(Integer.toString(userId)))
			{
				sendTextToChat("You are not allowed to view counters", updateMessage.getChatId());
				return;
			}
		}

		// send textual presentation of counters as response
		List<String> counters = _countsManager.textual();
		counters.forEach(c -> sendTextToChat(c, updateMessage.getChatId()));
	}

	private void executeCommandTranslate(BotCommand command, Message updateMessage)
	{
		List<String> permittedChats = _confManager.getValuesArray("translation", "permissions");
		if(updateMessage.getFrom() != null)
		{
			Integer userId = updateMessage.getFrom().getId();
			if(!permittedChats.contains(Integer.toString(userId)))
			{
				sendTextToChat("You are not allowed to perform translation.", updateMessage.getChatId());
				return;
			}
		}

		Locale srcTranslation = command.parameters().get("from") == null ? Locale.forLanguageTag("") : Locale.forLanguageTag(command.parameters().get("from"));
		Locale destTranslation = Locale.forLanguageTag(command.parameters().get("to"));
		String sourceText = command.parameters().get("text");
		Translation translFrom = Translation.SourceTranslation(srcTranslation, destTranslation, sourceText);
		Translation translTo = _transl.translate(translFrom);

		if(translTo.getResultText() == null || translTo.getResultText().isEmpty())
		{
			_countsManager.changeCounterValue(_counterTranslErrors, 1);
			sendTextToChat("Error translating text", updateMessage.getChatId());
		}
		else
		{
			sendTextToChat(translTo.getResultText(), updateMessage.getChatId());
			_countsManager.changeCounterValue(Counter.fromString("translations.FromChat." + updateMessage.getChatId()), 1);
			_countsManager.changeCounterValue(_counterTotalChars, translFrom.getSourceText().length());
			_countsManager.setCounterValue(_counterMsgLength, translFrom.getSourceText().length());
			_countsManager.changeCounterValue(_counterTranslates, 1);
		}
	}

	BotCommand parseCommand(String commandText)
	{
		ArrayList<String> tokens = new ArrayList<>(Arrays.asList(commandText.split("\\s+")));
		String firstWord = tokens.get(0).toLowerCase();

		if(!firstWord.startsWith("/"))
		{
			return BotCommand.NOPCommand();
		}

		firstWord = firstWord.substring(1, firstWord.length());

		BotCommand command = BotCommand.CreateCommand(UNKNOWN);
		switch (BotCommand.Commands.fromString(firstWord))
		{
			case HELP:
			{
				command = BotCommand.CreateCommand(HELP);
				break;
			}
			case STATUS:
			{
				command = BotCommand.CreateCommand(STATUS);
				break;
			}
			case LANGUAGES:
			{
				command = BotCommand.CreateCommand(LANGUAGES);
				break;
			}
			case COUNTERS:
			{
				command = BotCommand.CreateCommand(COUNTERS);
				break;
			}
			case COUNTERDELTA:
			{
				command = parseCommandCounterDelta(commandText);
				break;
			}
			case TRANSLATE:
			{
				command = parseCommandTranslate(commandText);
				break;
			}
			default:
				break;
		}

		return command;
	}

	private BotCommand parseCommandTranslate(String commandText)
	{
		BotCommand command;
		Pattern rxCommand = Pattern.compile("(/\\S+)\\s+(\\S{2})\\s+(\\S{2})\\s+(.+)", Pattern.DOTALL + Pattern.MULTILINE);
		Matcher matcher = rxCommand.matcher(commandText);
		if(matcher.find())
		{
			String fromLang = matcher.group(2);
			String toLang = matcher.group(3);
			String text = matcher.group(4);
			Map<String, String> params = new HashMap<>();
			params.put("from", fromLang);
			params.put("to", toLang);
			params.put("text", text);
			command = BotCommand.CreateCommand(TRANSLATE_FROM_TO, params);
			return command;
		}

		rxCommand = Pattern.compile("(/\\S+)\\s+(\\S{2})\\s+(.+)", Pattern.DOTALL + Pattern.MULTILINE);
		matcher = rxCommand.matcher(commandText);

		if(matcher.find())
		{
			String toLang = matcher.group(2);
			String text = matcher.group(3);
			Map<String, String> params = new HashMap<>();
			params.put("from", "");
			params.put("to", toLang);
			params.put("text", text);
			command = BotCommand.CreateCommand(TRANSLATE_TO, params);
			return command;
		}

		command = BotCommand.CreateCommand(NOTFULL);
		return command;
	}

	private BotCommand parseCommandCounterDelta(String commandText)
	{
		BotCommand command;
		// /command$1 <counter name>$2 <period name (month|week|day)>$3
		Pattern rxCommand = Pattern.compile("(/\\S+)\\s+(\\S+)\\s+(month)\\s*$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = rxCommand.matcher(commandText);
		if(matcher.find())
		{
			String counterName = matcher.group(2);
			String periodName = matcher.group(3);
			Map<String, String> params = new HashMap<>();
			params.put("counter", counterName);
			params.put("period", periodName);
			command = BotCommand.CreateCommand(COUNTERDELTA, params);
			return command;
		}

		command = BotCommand.CreateCommand(NOTFULL);
		return command;
	}

	@Override
	public String getBotUsername()
	{
		if (_botName == null)
		{
			String secretsGroup = SECRETS_GROUP;

			// for tests will use separate bot
			if(null == CLASS_VERSION || CLASS_VERSION.isEmpty())
			{
				secretsGroup = SECRETS_GROUP_TEST;
			}

			SimpleSecret botNameSecret = _secretsManager.getSecret("botName", secretsGroup);
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
			String secretsGroup = SECRETS_GROUP;

			// for tests will use separate bot
			if(null == CLASS_VERSION || CLASS_VERSION.isEmpty())
			{
				secretsGroup = SECRETS_GROUP_TEST;
			}

			SimpleSecret simpleSecret = _secretsManager.getSecret("botToken", secretsGroup);
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
			execute(message);
			LOG.debug("Sending message: {}", message);
		}
		catch (TelegramApiException e)
		{
			LOG.debug("Error sending message to the channel: ", e);
		}
	}
}
