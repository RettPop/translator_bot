package com.sapisoft.stats;

import com.google.gson.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class FileCountersManager implements CountersManager
{
	private static final Logger LOG = LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
	public static final String PROPERTY_UPDATED = "updated";
	public static final String PROPERTY_OLD_VALUE = "oldValue";
	public static final String PROPERTY_VALUE = "value";
	public static final String PROPERTY_CREATED = "created";
	public static final String PROPERTY_NAME = "name";
	public static final String SECTION_COUNTERS = "counters";
	public static final String CSV_SEPARATOR = ";";
	public static final String COUNTER_FILE_PREFIX = "counter_";
	public static final int COLUMN_VALUE = 2;
	private final String _fileName;
	private final String _countersDir;

	private Map<String, Counter> _counters = new ConcurrentHashMap<>();


	public FileCountersManager(String countersFile, String countersDir)
	{
		_fileName = countersFile;
		_countersDir = countersDir;

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
					.setMaxValue(newValue)
					.build();
		}
		else
		{
			Float maxValue = newCounter.getCounterValue() > newValue ? newCounter.getCounterValue() : newValue;

			newCounter = Counter.builder(newCounter)
					.setOldValue(newCounter.getCounterValue())
					.setCounterValue(newValue)
					.setUpdated(new Date())
					.setMaxValue(maxValue)
					.build();
		}
		_counters.put(counter.name(), newCounter);

		writeCounters(_fileName);
		writeCounter(newCounter);
		return newValue;
	}

	private void writeCounter(Counter counter)
	{
		synchronized (counter)
		{
			String counterFileName = Paths.get(_countersDir, COUNTER_FILE_PREFIX + counter.name().hashCode()).toAbsolutePath().toString();

			try (Writer writer = new FileWriter(counterFileName , true))
			{
				StringBuilder str = new StringBuilder(StringEscapeUtils.escapeCsv(counter.name()));
				str.append(CSV_SEPARATOR).append(counter.getOldValue());
				str.append(CSV_SEPARATOR).append(counter.getCounterValue());
				str.append(CSV_SEPARATOR).append(counter.getCreated().getTime());
				str.append(CSV_SEPARATOR).append(counter.getUpdated().getTime());
				str.append("\n");
				writer.append(str.toString());
			}
			catch (IOException e)
			{
				LOG.error("Error write counter {} file {}: ", counter.name(), counterFileName, e);
			}
		}
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
	public Counter getCounter(Counter existingCounter)
	{
		Counter counter = _counters.get(existingCounter.name());
		return counter;
	}

	@Override
	public List<Counter> getCounterStatesForPeriod(Counter counter, Date startDate, Date lastDate)
	{
		ArrayList<Counter> states = new ArrayList<>();
		readCounterFromCSV(counter.name(), state -> {
			if(state.getUpdated().compareTo(startDate) >= 0 && state.getUpdated().compareTo(lastDate) <= 0)
			{
				states.add(state);
			}
		});

		return states;
	}

	public List<String> textual()
	{
		List<String> counters = new ArrayList<>();

		for (Map.Entry<String, Counter> oneEntry: _counters.entrySet())
		{
			StringBuilder str = new StringBuilder("Counters:\n");
			Counter oneCounter = oneEntry.getValue();
			str.append("name: ").append(oneCounter.name()).append("\n");
			str.append("file: ").append(COUNTER_FILE_PREFIX + oneCounter.name().hashCode()).append("\n");
			str.append("value: ").append(oneCounter.getCounterValue()).append("\n");
			str.append("old value: ").append(oneCounter.getOldValue()).append("\n");
			str.append("max value: ").append(oneCounter.getMaxValue()).append("\n");
			str.append("updated: ").append(oneCounter.getUpdated()).append("\n");
			str.append("created: ").append(oneCounter.getCreated()).append("\n");

			counters.add(str.toString());
		}

		return counters;
	}

	//TODO: rewrite it in more modern way
	private Map<String, Counter> readCounters(String fileName)
	{
		Map<String, Counter> countersMap = new ConcurrentHashMap<>();

		try
		{
			// read counters list
			JsonParser parser = new JsonParser();
			JsonObject obj;
			synchronized (_counters)
			{
				FileReader in = new FileReader(fileName);
				obj = parser.parse(new BufferedReader(in)).getAsJsonObject();
			}

			JsonObject countersBlock = obj.getAsJsonObject(SECTION_COUNTERS);
			Set<Map.Entry<String, JsonElement>> counters = countersBlock.entrySet();

			// read counters from corresponding CSV files
			for (Map.Entry<String, JsonElement> oneElement : counters)
			{
				String counterName = oneElement.getKey();
				try
				{
					AtomicReference<Float> maxValue = new AtomicReference<>(Float.MIN_VALUE);
					Counter newCounter = readCounterFromCSV(counterName,
							c -> maxValue.set(c.getCounterValue() > maxValue.get() ? c.getCounterValue() : maxValue.get()));

					// need to set counter's maxValue
					newCounter = Counter.builder(newCounter)
							.setMaxValue(maxValue.get())
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

	interface CounterStateProcess
	{
		void processCounterState(Counter counter);
	}

	private Counter readCounterFromCSV(String counterName, CounterStateProcess stateCallback)
	{
		String counterFileName = Paths.get(_countersDir, COUNTER_FILE_PREFIX + counterName.hashCode()).toAbsolutePath().toString();
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(counterFileName),StandardCharsets.UTF_8))
		{
			// reading CSV file line by line until last record
			String currentLine = reader.readLine();
			Counter counterState = null;
			String lastLine = null;
			while (currentLine != null)
			{
				lastLine = currentLine;
				currentLine = reader.readLine();
				// parse counter state either if we have callback or it was last line in the file
				if(null != stateCallback || currentLine == null)
				{
					String[] columns = lastLine.split(CSV_SEPARATOR);
					counterState = getCounterFromColums(counterName, columns);
					if(null != stateCallback)
					{
						stateCallback.processCounterState(counterState);
					}
				}
			}

			return counterState;
		}
		catch (IOException e)
		{
			LOG.error("Error read counter {} file {}: ", counterName, counterFileName, e);
		}

		return null;
	}

	private Counter getCounterFromColums(String counterName, String[] columns)
	{
		if(columns.length < 5)
		{
			return null;
		}

		Counter counter = null;
		try
		{
			Date created = new Date(Long.parseLong(columns[3]));
			Date updated = new Date(Long.parseLong(columns[4]));
			counter = Counter.builder()
					.setCounterName(counterName)
					.setOldValue(Float.parseFloat(columns[1]))
					.setCounterValue(Float.parseFloat(columns[COLUMN_VALUE]))
					.setCreated(created)
					.setUpdated(updated)
					.build();
		}
		catch (Exception e)
		{
			LOG.debug("Error getting value while reading counter from CSV");
		}

		return counter;
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
