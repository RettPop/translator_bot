package com.sapisoft.stats;

import com.google.gson.*;
import com.sapisoft.config.ConfigManager;
import com.sapisoft.config.FileConfigManager;
import org.fest.assertions.data.MapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileCountersManager implements CountersManager
{
	private static final Logger LOG = LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
	public static final String PROPERTY_UPDATED = "updated";
	public static final String PROPERTY_OLD_VALUE = "oldValue";
	public static final String PROPERTY_VALUE = "value";
	public static final String PROPERTY_CREATED = "created";
	public static final String PROPERTY_NAME = "name";
	public static final String SECTION_COUNTERS = "counters";
	private final String _fileName;

	private Map<String, Counter> _counters = new ConcurrentHashMap<>();


	public FileCountersManager(String countersFile)
	{
		_fileName = countersFile;
		_counters = readCounters(_fileName);
	}

	@Override
	public Float setCounterValue(Counter counter, float newValue)
	{
		Counter newCounter = _counters.get(counter.name());
		if (null == newCounter)
		{
			newCounter = Counter.builder()
					.setCounterName(counter.name())
					.setOldValue(Float.NaN)
					.setCounterValue(newValue)
					.setCreated(new Date())
					.setUpdated(new Date())
					.build();
		}
		else
		{
			newCounter = Counter.builder(newCounter)
					.setOldValue(newCounter.getCounterValue())
					.setCounterValue(newValue)
					.setUpdated(new Date())
					.build();
		}
		_counters.put(counter.name(), newCounter);

		writeCounters(_fileName);
		return newValue;
	}

	@Override
	public Float changeCounterValue(Counter counterId, float valueDelta)
	{
		Counter oldCounter = _counters.get(counterId.name());
		if(oldCounter == null)
		{
			return setCounterValue(counterId, valueDelta);
		}

		Float newValue = oldCounter.getCounterValue() + valueDelta;
		setCounterValue(oldCounter, newValue);

		return newValue;
	}

	@Override
	public Float getCounterValue(Counter existingCounter)
	{
		Counter counter = _counters.get(existingCounter.name());
		if(null == counter)
		{
			return Float.NaN;
		}

		return counter.getCounterValue();
	}

	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder("Counters:\n");
		for (Map.Entry<String, Counter> oneEntry: _counters.entrySet())
		{
			Counter oneCounter = oneEntry.getValue();
			str.append("name: ").append(oneCounter.name()).append("\n");
			str.append("value: ").append(oneCounter.getCounterValue()).append("\n");
			str.append("old value: ").append(oneCounter.getOldValue()).append("\n");
			str.append("updated: ").append(oneCounter.getUpdated()).append("\n");
			str.append("created: ").append(oneCounter.getCreated()).append("\n");
			str.append("\n");
		}

		return str.toString();
	}

	//TODO: rewrite it in more modern way
	private Map<String, Counter> readCounters(String fileName)
	{
		Map<String, Counter> countersMap = new ConcurrentHashMap<>();

		try
		{
			JsonParser parser = new JsonParser();
			JsonObject obj;
			synchronized (_counters)
			{
				FileReader in = new FileReader(fileName);
				obj = parser.parse(new BufferedReader(in)).getAsJsonObject();
			}
			JsonObject countersBlock = obj.getAsJsonObject(SECTION_COUNTERS);
			Set<Map.Entry<String, JsonElement>> counters = countersBlock.entrySet();

			for (Map.Entry<String, JsonElement> oneElement : counters)
			{
				JsonObject counterObject = oneElement.getValue().getAsJsonObject();
				String counterName = oneElement.getKey();

				try
				{
					Date created = new Date(counterObject.get(PROPERTY_CREATED).getAsLong());
					Date updated = new Date(counterObject.get(PROPERTY_UPDATED).getAsLong());
					Counter newCounter = Counter.builder()
							.setCounterName(counterName)
							.setCounterValue(counterObject.get(PROPERTY_VALUE).getAsFloat())
							.setOldValue(counterObject.get(PROPERTY_OLD_VALUE).getAsFloat())
							.setCreated(created)
							.setUpdated(updated)
							.build();

					countersMap.put(counterName, newCounter);
				}
				catch (Exception e)
				{
					LOG.debug("Error creating counter {}: ", counterName, e);
				}
			}
		}
		catch (Exception e)
		{
			LOG.error("Error reading counters file: ", e);
		}

		return  countersMap;
	}

	//TODO: rewrite it in more modern way
	private void writeCounters(String fileName)
	{
		JsonObject countersSection = new JsonObject();
		for (Map.Entry<String, Counter> oneEntry : _counters.entrySet())
		{
			Counter counter = oneEntry.getValue();
			JsonObject obj = new JsonObject();
			obj.addProperty(PROPERTY_NAME, counter.name());
			obj.addProperty(PROPERTY_VALUE, counter.getCounterValue());
			obj.addProperty(PROPERTY_OLD_VALUE, counter.getOldValue());
			obj.addProperty(PROPERTY_CREATED, counter.getCreated().getTime());
			obj.addProperty(PROPERTY_UPDATED, counter.getUpdated().getTime());
			countersSection.add(counter.name(), obj);
		}

		JsonObject root = new JsonObject();
		root.add(SECTION_COUNTERS, countersSection);

		synchronized (_counters)
		{
			try (Writer writer = new FileWriter(fileName, false))
			{
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				gson.toJson(root, writer);
			}
			catch (IOException e)
			{
				LOG.error("Error write counters file {}: ", fileName, e);
			}
		}
	}
}
