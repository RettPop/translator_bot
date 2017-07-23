package com.sapisoft.bots;

import java.util.*;

/**
 * Created by rettpop on 2017-07-02.
 */
public class BotCommand
{
	public enum Commands
	{
		HELP,
		STATUS,
		TRANSLATE,
		UNKNOWN,
		NOTFULL,
		NOP
	}

	private Commands command;
	private Map<String, String> parameters;

	private BotCommand(Commands command, Map<String, String> parameters)
	{
		this.command = command;
		this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
	}

	public static BotCommand CreateCommand(Commands command, Map<String, String> parameters)
	{
		return new BotCommand(command, parameters);
	}

	public static BotCommand CreateCommand(Commands command)
	{
		return new BotCommand(command, null);
	}

	public static BotCommand NOPCommand()
	{
		return new BotCommand(Commands.NOP, null);
	}

	public Commands command()
	{
		return command;
	}

	public Map<String, String> parameters()
	{
		return parameters;
	}
}
