package com.sapisoft.bots;

import com.sapisoft.azuretranslator.AzureTranslator;
import com.sapisoft.config.FileConfigManager;
import com.sapisoft.googletranslator.GoogleTranslator;
import com.sapisoft.secrets.ResourcesSecretsManager;
import com.sapisoft.secrets.SimpleSecret;
import com.sapisoft.stats.Counter;
import com.sapisoft.stats.FileCountersManager;
import com.sapisoft.translator.Translation;
import com.sapisoft.translator.Translator;
import com.sapisoft.utils.ShortenUrlExpander;
import org.fest.util.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
	public static final String PERIOD_MONTH = "month";
	public static final String PERIOD_WEEK = "week";
	public static final String PERIOD_DAY = "day";
	public static final String CONFIG_COUNTERS_FILE = "countersFile";
	public static final String CONFIG_COUNTERS_DIR = "countersDir";

	private String _apiKey;
	private String _botName;
	private final Translator _transl = new AzureTranslator();
	private final FileConfigManager _confManager = new FileConfigManager("/config/config.json");
	private final ResourcesSecretsManager _secretsManager = new ResourcesSecretsManager("/secrets/keys.json");
	private Map<Long, List<TranslationCommand>> _routing;

	private final FileCountersManager _countsManager;
	private final Counter COUNTER_TRANSLATES = Counter.fromString("translations.Number");
	private final Counter COUNTER_TOTAL_CHARS = Counter.fromString("translations.Characters");
	private final Counter COUNTER_MSG_LENGTH = Counter.fromString("translations.MessageLength");
	private final Counter COUNTER_TRANSL_ERRORS = Counter.fromString("translations.Error");
	private final Counter COUNTER_COMMANDS_TOTAL = Counter.fromString("telegrammer.Commands");

	public Telegrammer()
	{
		_routing = getRoutings();

		// read system properties before. If null, read from config
		String countersFile = Optional.ofNullable(System.getProperty(CONFIG_COUNTERS_FILE))
				.orElse(_confManager.getOption(CONFIG_COUNTERS_FILE, "statistics"));

		String countersDir = Optional.ofNullable(System.getProperty(CONFIG_COUNTERS_DIR))
				.orElse(_confManager.getOption(CONFIG_COUNTERS_DIR, "statistics"));

		_countsManager = new FileCountersManager(countersFile, countersDir);

		LOG.info("Starting v.{}", CLASS_VERSION);
	}

	private Map<Long, List<TranslationCommand>> getRoutings()
	{
		FileConfigManager routingsCfg = new FileConfigManager("/config/routings.json");
		List<String> sections = routingsCfg.getSections();
		HashMap<Long, ArrayList<TranslationCommand>> routingsArray = new HashMap<>();

		for (String oneSection : sections)
		{
			Long sourceChatId = routingsCfg.getLongValue("sourceChat", oneSection);
			Long targetChatId = routingsCfg.getLongValue("destinationChat", oneSection);
			String srcLang = routingsCfg.getOption("sourceLocale", oneSection);
			String dstLang = routingsCfg.getOption("destinationLocale", oneSection);
			Translation translation = Translation.DestinationTranslation(Locale.forLanguageTag(dstLang), "");
			if(null != srcLang)
			{
				translation = Translation.SourceTranslation(Locale.forLanguageTag(srcLang), Locale.forLanguageTag(dstLang), "");
			}

			TranslationCommand command = TranslationCommand.createTranslation(targetChatId, translation);
			if(routingsArray.containsKey(sourceChatId))
			{
				// already have array or routes. just adding new item
				routingsArray.get(sourceChatId).add(command);
			}
			else
			{
				// need to create new array of routes
				routingsArray.put(sourceChatId, new ArrayList<>(Collections.singleton(command)));
			}
		}

		// convert routes array to list
		HashMap<Long, List<TranslationCommand>> routings = new HashMap<>();
		for (Map.Entry<Long, ArrayList<TranslationCommand>> oneRoute : routingsArray.entrySet())
		{
			routings.put(oneRoute.getKey(), Collections.unmodifiableList(oneRoute.getValue()));
		}

		return routings;
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
		if (null != command && (UNKNOWN != command.command()))
		{
			_countsManager.changeCounterValue(COUNTER_COMMANDS_TOTAL, 1);
			Message message = update.hasMessage() ? update.getMessage() : update.getChannelPost();
			executeCommand(command, message);
		}
		else
		{
			if(update.hasCallbackQuery())
			{
				processCallback(update);
			}
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

	private void processCallback(Update update)
	{
		CallbackQuery callbackQuery = update.getCallbackQuery();
		String[] data = callbackQuery.getData().split(":");
		if(data.length < 3)
		{
			executeCommand(BotCommand.CreateCommand(NOTFULL), update.getMessage());
			return;
		}

		if(BotCommand.Commands.COUNTERDELTA.toString().compareToIgnoreCase(data[0]) == 0)
		{
			Integer counterId = Integer.parseInt(data[1]);
			Counter counter = _countsManager.getCounterById(counterId);
			if(counter != null)
			{
				String period = data[2];
				HashMap<String, String> params = new HashMap<>();
				params.put("counter", counter.name());
				params.put("period", period);
				BotCommand command = BotCommand.CreateCommand(BotCommand.Commands.COUNTERDELTA, params);
				executeCommandCounterdelta(command, callbackQuery.getMessage(), callbackQuery.getFrom());
			}
			else
			{
				sendTextToChat("Unknown counter", update.getMessage().getChatId());
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

			if (translatedMsg.getResultText().isEmpty())
			{
				_countsManager.changeCounterValue(COUNTER_TRANSL_ERRORS, 1);
				LOG.info("Empty message translation arrived");
				return;
			}

			String linksText = getTranslatedLinksText(msg, new GoogleTranslator(), translatedMsg);

			_countsManager.changeCounterValue(COUNTER_TRANSLATES, 1);
			_countsManager.changeCounterValue(COUNTER_TOTAL_CHARS, msg.getText().length());
			_countsManager.setCounterValue(COUNTER_MSG_LENGTH, msg.getText().length());
			_countsManager.changeCounterValue(Counter.fromString("translations.FromChat." + sourceChat.getId()), 1);

			String report = translatedMsg.getResultText()
					+ linksText
					+ "\n<i>(" + translatedMsg.getSourceLocale().getLanguage() + "->" + translatedMsg.getDestinationLocale().getLanguage()
					+ " from \"" + update.getChannelPost().getChat().getTitle() + "\")</i>";

			LOG.info("Sending message to channel: {}", oneCommand.targetChannelId());
			sendTextToChat(report, oneCommand.targetChannelId());
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
				executeCommandCounterdelta(command, updateMessage, updateMessage.getFrom());
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

	private void executeCommandCounterdelta(BotCommand command, Message updateMessage, User user)
	{
		List<String> permittedChats = _confManager.getValuesArray("counters.show", "permissions");
		if(user != null)
		{
			Integer userId = user.getId();
			if(!permittedChats.contains(Integer.toString(userId)))
			{
				sendTextToChat("You are not allowed to view counters", updateMessage.getChatId());
				return;
			}
		}
		else
		{
			sendTextToChat("You are not allowed to view counters", updateMessage.getChatId());
			return;
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
		Date startDate = lastDate;
		if(PERIOD_MONTH.equals(periodName))
		{
			startDate = Date.from(LocalDate.now()
					.atStartOfDay()
					.withDayOfMonth(1)
					.atZone(ZoneId.systemDefault())
					.toInstant());
		}
		else if(PERIOD_DAY.equals(periodName))
		{
			startDate = Date.from(LocalDate.now()
					.atStartOfDay()
					.atZone(ZoneId.systemDefault())
					.toInstant());
		}
		else if(PERIOD_WEEK.equals(periodName))
		{
			startDate = Date.from(LocalDate.now()
					.atStartOfDay()
					.with(DayOfWeek.MONDAY)
					.atZone(ZoneId.systemDefault())
					.toInstant());
		}
		else
		{
			executeCommand(BotCommand.CreateCommand(NOTFULL), updateMessage);
		}

		List<Counter> counterStates = _countsManager.getCounterStatesForPeriod(counter, startDate, lastDate);
		if(counterStates.size() < 2)
		{
			sendTextToChat("Not enough values to calculate delta", updateMessage.getChatId());
			return;
		}

		float delta = counterStates.get(counterStates.size() - 1).getCounterValue() - counterStates.get(0).getCounterValue();
		sendTextToChat("Counter " + counter.name() + " delta since beginning of " + periodName + " is: " + delta, updateMessage.getChatId());
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
		List<Counter> counters = _countsManager.getCounters();
		for (Counter oneCounter : counters)
		{
			StringBuilder str = new StringBuilder("Counter:\n");
			str.append("name: ").append(oneCounter.name()).append("\n");
			str.append("id: ").append(_countsManager.getCounterId(oneCounter)).append("\n");
			str.append("value: ").append(oneCounter.getCounterValue()).append("\n");
			str.append("old value: ").append(oneCounter.getOldValue()).append("\n");
			str.append("max value: ").append(oneCounter.getMaxValue()).append("\n");
			str.append("updated: ").append(oneCounter.getUpdated()).append("\n");
			str.append("created: ").append(oneCounter.getCreated()).append("\n");

			// adding button under the counter
			Integer id = _countsManager.getCounterId(oneCounter);
			InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

			ArrayList<InlineKeyboardButton> buttons = new ArrayList<>();
			for (String period : Arrays.asList(PERIOD_MONTH, PERIOD_WEEK, PERIOD_DAY))
			{
				buttons.add(new InlineKeyboardButton()
					.setText("Δ " + period)
					.setCallbackData(BotCommand.Commands.COUNTERDELTA + ":" + id + ":" + period));
			}
			keyboardMarkup.setKeyboard(Arrays.asList(buttons));

			SendMessage btnMessage = new SendMessage();
//			btnMessage.enableMarkdown(true);
			btnMessage.setText(str.toString());
			btnMessage.setChatId(updateMessage.getChatId());
			btnMessage.setReplyMarkup(keyboardMarkup);

			try
			{
				execute(btnMessage);
				LOG.debug("Sending keyboard: {}", btnMessage);
			}
			catch (TelegramApiException e)
			{
				LOG.debug("Error sending keyboard to the channel: ", e);
			}
		}
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
			_countsManager.changeCounterValue(COUNTER_TRANSL_ERRORS, 1);
			sendTextToChat("Error translating text", updateMessage.getChatId());
			return;
		}

		String linksText = getTranslatedLinksText(updateMessage, new GoogleTranslator(), translTo);

		sendTextToChat(translTo.getResultText() + linksText, updateMessage.getChatId());
		_countsManager.changeCounterValue(Counter.fromString("translations.FromChat." + updateMessage.getChatId()), 1);
		_countsManager.changeCounterValue(COUNTER_TOTAL_CHARS, translFrom.getSourceText().length());
		_countsManager.setCounterValue(COUNTER_MSG_LENGTH, translFrom.getSourceText().length());
		_countsManager.changeCounterValue(COUNTER_TRANSLATES, 1);
	}

	private String getTranslatedLinksText(Message updateMessage, Translator translator, Translation translTo)
	{
		List<String> msgURLs = getMessageURLs(updateMessage);
		StringBuilder linksText = new StringBuilder();
		if(msgURLs.size() > 0)
		{
			int number = 1;
			ShortenUrlExpander expander = new ShortenUrlExpander();
			linksText.append("\nLinks:");
			for (String oneURL : msgURLs)
			{
				// will use base URL if will fail expanding it
				String longURL = oneURL;
				try
				{
					longURL = expander.expand(oneURL);
				}
				catch (IOException e)
				{
					LOG.debug("Error expanding URL {}", oneURL, e);
				}

				String translationURL = translator.pageTranslationURL(longURL, translTo);
				linksText.append("\n" + (number++) + ": ")
						.append(translationURL);
			}
		}
		return linksText.toString();
	}

//	private String getTranslatedLinksText(Message updateMessage, Translator translator, Translation translation)
//	{
//		StringBuilder msgTail = new StringBuilder();
//		List<String> msgURLs = getMessageURLs(updateMessage);
//
//		for (MessageEntity oneEntity : updateMessage.getEntities())
//		{
//			if("url".equals(oneEntity.getType()))
//			{
//				try
//				{
//					String longURL = expander.expand(oneEntity.getText());
//					String translationURL = googleTranslator.pageTranslationURL(longURL, translTo);
//					msgTail.append("\n")
//							.append(translationURL);
//				}
//				catch (IOException e)
//				{
//					LOG.debug("Error expanding URL {}", oneEntity.getText(), e);
//				}
//			}
//		}
//	}

	private List<String> getMessageURLs(Message updateMessage)
	{
		ArrayList<String> urls = new ArrayList<>();

		for (MessageEntity oneEntity : updateMessage.getEntities())
		{
			if("url".equals(oneEntity.getType()))
			{
				urls.add(oneEntity.getText());
			}
		}

		return urls;
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
		Pattern rxCommand = Pattern.compile("(/\\S+)\\s+(\\S+)\\s+(month|week|day)\\s*$", Pattern.CASE_INSENSITIVE);
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

	@VisibleForTesting
	FileCountersManager getCountsManager()
	{
		return _countsManager;
	}

}
