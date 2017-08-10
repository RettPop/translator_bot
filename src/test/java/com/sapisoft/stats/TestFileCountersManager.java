package com.sapisoft.stats;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class TestFileCountersManager
{
	@Rule
	public TemporaryFolder folder= new TemporaryFolder();

	@Test
	public void testShouldCreateAndReadCounter() throws Exception
	{
		File tempFile = folder.newFile();
		File countersDir = folder.newFolder();

		FileCountersManager manager = new FileCountersManager(tempFile.getAbsolutePath(), countersDir.getAbsolutePath());
		Counter counter = Counter.fromString("testCounter");
		manager.setCounterValue(counter, 102);

		manager = new FileCountersManager(tempFile.getAbsolutePath(), countersDir.getAbsolutePath());
		assertThat(manager.getCounterValue(counter)).isEqualTo(102f);
	}

	@Test
	public void testShouldChangeCounter() throws Exception
	{
		File tempFile = folder.newFile();
		File countersDir = folder.newFolder();

		FileCountersManager manager = new FileCountersManager(tempFile.getAbsolutePath(), countersDir.getAbsolutePath());
		Counter counter = Counter.fromString("testCounter");
		manager.setCounterValue(counter, 102);
		manager.changeCounterValue(counter, 100);

		manager = new FileCountersManager(tempFile.getAbsolutePath(), countersDir.getAbsolutePath());
		assertThat(manager.getCounterValue(counter)).isEqualTo(202f);
	}

	@Test
	public void testShouldCreateCountersAndReturnListOfTextualPresentation() throws IOException
	{
		File tempFile = folder.newFile();
		File countersDir = folder.newFolder();

		FileCountersManager manager = new FileCountersManager(tempFile.getAbsolutePath(), countersDir.getAbsolutePath());
		int expected = 5;
		for (int idx = 0; idx < expected; idx++)
		{
			manager.setCounterValue(Counter.fromString("testCounter" + idx), 102);
		}

		List<String> counters = manager.textual();
		assertThat(counters.size())
				.isEqualTo(expected);
	}

	@Test
	public void testShouldReturnMaxValueOfNewCounter() throws IOException
	{
		File tempFile = folder.newFile();
		File countersDir = folder.newFolder();

		FileCountersManager manager = new FileCountersManager(tempFile.getAbsolutePath(), countersDir.getAbsolutePath());

		int expected = 5;
		int maxValue = expected * 3;
		Counter testCounter = Counter.fromString("testCounter");
		manager.setCounterValue(testCounter, expected * 2);
		manager.setCounterValue(testCounter, 1);
		manager.setCounterValue(testCounter, maxValue);
		manager.setCounterValue(testCounter, 0);

		assertThat(manager.getCounter(testCounter).getMaxValue())
				.isEqualTo(maxValue);
	}

	@Test
	public void testShouldReturnMaxValueOfCounterHasBeenRead() throws IOException
	{
		File tempFile = folder.newFile();
		File countersDir = folder.newFolder();

		FileCountersManager manager = new FileCountersManager(tempFile.getAbsolutePath(), countersDir.getAbsolutePath());

		int expected = 5;
		int maxValue = expected * 3;
		Counter testCounter = Counter.fromString("testCounter");
		manager.setCounterValue(testCounter, expected * 2);
		manager.setCounterValue(testCounter, 1);
		manager.setCounterValue(testCounter, maxValue);
		manager.setCounterValue(testCounter, 0);

		manager = new FileCountersManager(tempFile.getAbsolutePath(), countersDir.getAbsolutePath());
		assertThat(manager.getCounter(testCounter).getMaxValue())
				.isEqualTo(maxValue);
	}

	@Test
	public void testShouldReturnCounterStatesBetweenDates() throws Exception
	{
		File tempFile = folder.newFile();
		File countersDir = folder.newFolder();

		FileCountersManager manager = new FileCountersManager(tempFile.getAbsolutePath(), countersDir.getAbsolutePath());

		int expected = 5;
		int maxValue = expected * 3;
		Counter testCounter = Counter.fromString("testCounter");
		manager.setCounterValue(testCounter, 0);

		Thread.sleep(100);
		Date startDate = new Date();
		manager.setCounterValue(testCounter, 1);
		manager.changeCounterValue(testCounter, 1);
		manager.changeCounterValue(testCounter, 1);
		manager.changeCounterValue(testCounter, 1);

		Thread.sleep(100);
		manager.setCounterValue(testCounter, 2);
		Date lastDate = new Date();

		Thread.sleep(100);
		manager.setCounterValue(testCounter, 3);

		// should has 1 and 2
		ArrayList<Counter> states = new ArrayList<>(manager.getCounterStatesForPeriod(testCounter, startDate, lastDate));

		assertThat(states.size())
				.isEqualTo(5);
		assertThat(states.get(0).getCounterValue())
				.isEqualTo(1);
		assertThat(states.get(states.size() - 1).getCounterValue())
				.isEqualTo(2);
	}
}