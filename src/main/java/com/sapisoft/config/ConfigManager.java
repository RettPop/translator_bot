package com.sapisoft.config;

import java.util.List;

public interface ConfigManager
{
	String getOption(String optionName, String configSection);
	List<String> getValuesArray(String optionName, String configSection);

	default void setOption(String optionName, String configSection, String optionValue)
	{
		return;
	}

	default void setOption(String optionName, String configSection, List<String> valuesArray)
	{
		return;
	}
}
