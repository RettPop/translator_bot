package com.sapisoft.stats;

import org.fest.util.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.fest.assertions.api.Assertions.assertThat;

public class TestFileCountersManager
{
	@Rule
	public TemporaryFolder folder= new TemporaryFolder();

	@Test
	public void testShouldCreateAndReadCounter() throws Exception
	{
		File tempFile = folder.newFile();

		FileCountersManager manager = new FileCountersManager(tempFile.getAbsolutePath());
		Counter counter = Counter.fromString("testCounter");
		manager.setCounterValue(counter, 102);

		manager = new FileCountersManager(tempFile.getAbsolutePath());
		assertThat(manager.getCounterValue(counter)).isEqualTo(102f);
	}

	@Test
	public void testShouldChangeCounter() throws Exception
	{
		File tempFile = folder.newFile();

		FileCountersManager manager = new FileCountersManager(tempFile.getAbsolutePath());
		Counter counter = Counter.fromString("testCounter");
		manager.setCounterValue(counter, 102);
		manager.changeCounterValue(counter, 100);

		manager = new FileCountersManager(tempFile.getAbsolutePath());
		assertThat(manager.getCounterValue(counter)).isEqualTo(202f);
	}
}