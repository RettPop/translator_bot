package com.sapisoft.config;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import static org.fest.assertions.api.Assertions.assertThat;

public class TestFileConfigManager
{
	@Rule
	public TemporaryFolder folder= new TemporaryFolder();
	public File _tempFile;

	@Before
	public void setup() throws IOException
	{
		_tempFile = folder.newFile();
		_tempFile.setWritable(true);
		try(Writer writer = new FileWriter(_tempFile))
		{
			writer.write("{\n" +
					"    \"configSection1\":\n" +
					"    {\n" +
					"      \"optionName1\": \"option1Value\",\n" +
					"      \"optionName2\": \"option2Value\"\n" +
					"    },\n" +
					"    \"configSection2\":\n" +
					"    {\n" +
					"      \"optionName1\": \"option1Value\"\n" +
					"    }\n" +
					"}");
		}
	}

	@Test
	public void getSections()
	{
		FileConfigManager configManager = new FileConfigManager(_tempFile.getAbsolutePath());
	    assertThat(configManager.getSections().size())
			    .isEqualTo(2);
	}
}