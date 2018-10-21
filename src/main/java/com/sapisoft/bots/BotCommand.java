package com.sapisoft.bots;

import java.util.*;

/**
 * Created by rettpop on 2017-07-02.
 */
public class BotCommand
{
	public enum Commands
	{
		HELP("help"),
		STATUS("status"),
		TRANSLATE("translate"),
		TRANSLATE_TO("translate"),
		TRANSLATE_FROM_TO("translate"),
		LANGUAGES("languages"),
		COUNTERS("counters"),
		COUNTERDELTA("counterdelta"),
		UNKNOWN("unknown"),
		NOTFULL("notfull"),
		NOP("nop");

		private String _stringName;
		Commands(String stringName)
		{
			_stringName = stringName;
		}

		public final String str()
		{
			return _stringName;
		}

		public static Commands fromString(String string)
		{
			Commands command = UNKNOWN;
			switch (string.toLowerCase())
			{
				case "help":
					command = HELP;
					break;
				case "s":
				case "status":
					command = STATUS;
					break;
				case "t": //alias
				case "translate":
					command = TRANSLATE;
					break;
				case "translate_to":
					command = TRANSLATE_TO;
					break;
				case "translate_from_to":
					command = TRANSLATE_FROM_TO;
					break;
				case "c":
				case "counters":
					command = COUNTERS;
					break;
				case "cd":
				case "counterdelta":
					command = COUNTERDELTA;
					break;
				case "l":
				case "languages":
					command = LANGUAGES;
					break;
				case "unknown":
					command = UNKNOWN;
					break;
				case "notfull":
					command = NOTFULL;
					break;
				case "nop":
					command = NOP;
					break;
			}

			return command;
		}
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
