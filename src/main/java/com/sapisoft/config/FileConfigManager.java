package com.sapisoft.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class FileConfigManager implements ConfigManager
{
	private static final Logger LOG = LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

	private String _fileName;
	private JsonObject _config;

	public FileConfigManager(String fileName)
	{
		this._fileName = fileName;
		try
		{
			_config = readConfig(_fileName);
		}
		catch (FileNotFoundException e)
		{
			LOG.warn("Config file does not exist: {}", _fileName);
			_config = new JsonObject();
		}
	}

	private JsonObject readConfig(String fileName) throws FileNotFoundException
	{
		InputStream in = getClass().getResourceAsStream(fileName);
		if(null == in)
		{
			in = new FileInputStream(new File(fileName));
		}
		JsonParser parser = new JsonParser();
		JsonObject obj = parser.parse(new BufferedReader(new InputStreamReader(in))).getAsJsonObject();

		return obj;
	}

	@Override
	public String getOption(String optionName, String configSection)
	{
        JsonObject group = _config.get(configSection).getAsJsonObject();

		return group.get(optionName).getAsString();
	}

	@Override
	public List<String> getValuesArray(String optionName, String configSection)
	{
		JsonObject section = _config.get(configSection).getAsJsonObject();
		JsonArray jsonArray = section.get(optionName).getAsJsonArray();

		List<String> valuesArray = new ArrayList<>();
		for (int idx = 0; idx < jsonArray.size(); idx++)
		{
			valuesArray.add(jsonArray.get(idx).toString());
		}

		return valuesArray;
	}

	@Override
	public List<String> getSections()
	{
		return _config.entrySet()
				.stream()
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}

	public Long getLongValue(String optionName, String configSection)
	{
		return Long.parseLong(getOption(optionName, configSection));
	}

	@Override
	public void setOption(String optionName, String configSection, String optionValue)
	{
		return;
	}
}
