package com.sapisoft.stats;

public interface CountersManager
{
	Float setCounterValue(Counter counter, float newValue);
	Float changeCounterValue(Counter counter, float valueDelta);
	Float getCounterValue(Counter counter);
}
