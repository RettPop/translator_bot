package com.sapisoft.bots;

import org.junit.Test;

import static com.sapisoft.bots.BotCommand.Commands.*;
import static org.junit.Assert.*;

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
		assertTrue(command.command() == NOTFULL);
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
}