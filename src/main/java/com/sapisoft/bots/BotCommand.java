package com.sapisoft.bots;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rettpop on 2017-07-02.
 */
public class BotCommand
{
	public enum Commands
	{
		HELP,
		STATUS,
		NOP
	}

	private Commands command;
	private List<String> parameters;

	private BotCommand(Commands command, List<String> parameters)
	{
		this.command = command;
		this.parameters = parameters != null ? parameters : new ArrayList<>();
	}

	public static BotCommand CreateCommand(Commands command, List<String> parameters)
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

	public List<String> parameters()
	{
		return parameters;
	}
}
