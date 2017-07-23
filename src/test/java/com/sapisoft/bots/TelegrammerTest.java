package com.sapisoft.bots;

import org.junit.Test;

import static com.sapisoft.bots.BotCommand.Commands.NOTFULL;
import static com.sapisoft.bots.BotCommand.Commands.TRANSLATE;
import static org.junit.Assert.*;

public class TelegrammerTest
{
	@Test
	public void testShouldReturnFullTranslateCommand() throws Exception
	{
		Telegrammer telegrammer = new Telegrammer();
		BotCommand command = telegrammer.parseCommand("/translate en text");
		assertTrue(command.command() == TRANSLATE);
	}

	@Test
	public void testShouldReturnNotFullCommand() throws Exception
	{
		Telegrammer telegrammer = new Telegrammer();
		BotCommand command = telegrammer.parseCommand("/translate text");
		assertTrue(command.command() == NOTFULL);
	}
}