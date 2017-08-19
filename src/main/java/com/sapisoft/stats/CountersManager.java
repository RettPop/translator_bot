package com.sapisoft.stats;

import java.util.Date;
import java.util.List;

public interface CountersManager
{
	Float setCounterValue(Counter counter, float newValue);
	Float changeCounterValue(Counter counter, float valueDelta);
	Float getCounterValue(Counter counter);
	Counter getCounter(Counter counter);
	List<Counter> getCounterStatesForPeriod(Counter counter, Date startDate, Date lastDate);
	List<Counter> getCounters();
	Integer getCounterId(Counter counter);
	Counter getCounterById(Integer id);
}
