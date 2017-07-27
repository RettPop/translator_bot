package com.sapisoft.config;

import com.google.gson.*;
import com.sapisoft.secrets.SimpleSecret;

import javax.print.DocFlavor;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileConfigManager implements ConfigManager<String>
{
	private String _fileName;

	public FileConfigManager(String fileName)
	{
		this._fileName = fileName;
	}

	@Override
	public String getOption(String optionName, String configSection)
	{
        InputStream in = getClass().getResourceAsStream(_fileName);

        JsonParser parser = new JsonParser();
        JsonObject obj = parser.parse(new BufferedReader(new InputStreamReader(in))).getAsJsonObject();
        JsonObject group = obj.get(configSection).getAsJsonObject();

		return group.get(optionName).getAsString();
	}

	public List<String> getValuesArray(String optionName, String configSection)
	{
		InputStream in = getClass().getResourceAsStream(_fileName);

		JsonParser parser = new JsonParser();
		JsonObject obj = parser.parse(new BufferedReader(new InputStreamReader(in))).getAsJsonObject();
		JsonObject section = obj.get(configSection).getAsJsonObject();
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
}
