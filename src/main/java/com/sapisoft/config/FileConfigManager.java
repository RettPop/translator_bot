package com.sapisoft.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileConfigManager implements ConfigManager
{
	private String _fileName;
	private JsonObject _config;

	public FileConfigManager(String fileName)
	{
		this._fileName = fileName;
		_config = readConfig(_fileName);
	}

	private JsonObject readConfig(String fileName)
	{
		InputStream in = getClass().getResourceAsStream(fileName);
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
