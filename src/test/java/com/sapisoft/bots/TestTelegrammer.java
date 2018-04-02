package com.sapisoft.bots;

import com.sapisoft.stats.FileCountersManager;
import org.junit.Test;

import static com.sapisoft.bots.BotCommand.Commands.*;
import static org.fest.assertions.api.Assertions.assertThat;

public class TestTelegrammer
{
	@Test
	public void testShouldReturnFullTranslateToCommand() throws Exception
	{
		Telegrammer telegrammer = new Telegrammer();
		BotCommand command = telegrammer.parseCommand("/translate en text");
		assert(command.command())
				.equals(TRANSLATE_TO);
	}

	@Test
	public void testShouldReturnFullTranslateFromToCommand() throws Exception
	{
		Telegrammer telegrammer = new Telegrammer();
		BotCommand command = telegrammer.parseCommand("/translate sv en Din försäkringsgivare är Länsförsäkringar Blekinge.\n" +
				"\n" +
				"När du klickar på \"Fortsätt\" kommer du till en sida med dina kontaktuppgifter och det är inte förrän du fyllt i dessa och kommit till ytterligare en bekräftelsesida som din försäkring är tecknad.");
		assert(command.command())
				.equals(TRANSLATE_FROM_TO);
	}

	@Test
	public void testShouldReturnNotFullCommand() throws Exception
	{
		Telegrammer telegrammer = new Telegrammer();
		BotCommand command = telegrammer.parseCommand("/translate text");
		assertThat(command.command())
				.isEqualTo(NOTFULL);
	}

	@Test
	public void testShouldReturnCounterDeltaCommand() throws Exception
	{
		Telegrammer telegrammer = new Telegrammer();
		BotCommand command = telegrammer.parseCommand("/counterdelta counterName month  ");
		assertThat(command.command())
				.isEqualTo(COUNTERDELTA);
		assertThat(command.parameters().get("counter"))
				.isEqualTo("counterName");
		assertThat(command.parameters().get("period"))
				.isEqualTo("month");
	}

	@Test
	public void testShouldReturnUnknownCommand() throws Exception
	{
		Telegrammer telegrammer = new Telegrammer();
		BotCommand command = telegrammer.parseCommand("/unknowncommand text");
		assert(command.command())
				.equals(UNKNOWN);
	}

	@Test
	public void testShouldReturnNopCommand() throws Exception
	{
		Telegrammer telegrammer = new Telegrammer();
		BotCommand command = telegrammer.parseCommand("not a command text");
		assert(command.command())
				.equals(NOP);
	}

	@Test
	public void testShouldReadCountersFileAndDirFromSystemProperty()
	{
		String countersFile = "/var/transbot/counters.json";
		System.setProperty(Telegrammer.CONFIG_COUNTERS_FILE, countersFile);
		String countersDir = "/var/transbot/counters/";
		System.setProperty(Telegrammer.CONFIG_COUNTERS_DIR, countersDir);
		Telegrammer telegrammer = new Telegrammer();
		if(telegrammer.getCountsManager() instanceof FileCountersManager)
		{
			FileCountersManager countersManager = telegrammer.getCountsManager();
			assertThat(countersManager.getCountersDir())
					.isEqualTo(countersDir);
			assertThat(countersManager.getCountersFileName())
					.isEqualTo(countersFile);
		}
	}
}