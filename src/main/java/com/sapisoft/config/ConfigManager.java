package com.sapisoft.config;

public interface ConfigManager<T>
{
	T getOption(String optionName, String configSection);

	default void setOption(String optionName, String configSection, T optionValue)
	{
		return;
	}
}
