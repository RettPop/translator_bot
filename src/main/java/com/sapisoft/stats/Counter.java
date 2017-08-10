package com.sapisoft.stats;

import java.util.Date;

public class Counter
{
	private String counterName;
	private Float oldValue;
	private Float counterValue;
	private Float maxValue;
	private Date created;
	private Date updated;


//	private Counter(String counterName, Float oldValue, Float counterValue, Date created, Date updated)
//	{
//		this.counterName = counterName;
//		this.oldValue = oldValue;
//		this.counterValue = counterValue;
//		this.created = created;
//		this.updated = updated;
//	}

	public String name()
	{
		return counterName;
	}

	public static Counter fromString(String counterName)
	{
		return Counter.builder()
				.setCounterName(counterName)
				.build();
	}

	public static CounterBuilder builder()
	{
		return new CounterBuilder();
	}

	public static CounterBuilder builder(Counter counter)
	{
		return new CounterBuilder(counter);
	}

	public Float getOldValue()
	{
		return oldValue;
	}

	public Float getCounterValue()
	{
		return counterValue;
	}

	public Date getCreated()
	{
		return created;
	}

	public Date getUpdated()
	{
		return updated;
	}

	public Float getMaxValue()
	{
		return maxValue;
	}

	public static class CounterBuilder
	{
		private String counterName;
		private Float oldValue = Float.NaN;
		private Float counterValue;
		private Float maxValue = Float.NaN;
		private Date created = new Date();
		private Date updated = new Date();

		private CounterBuilder()
		{

		}

		private CounterBuilder(Counter counter)
		{
			counterName = counter.name();
			oldValue = counter.getOldValue();
			counterValue = counter.getCounterValue();
			created = counter.getCreated();
			updated = counter.getUpdated();
		}

		CounterBuilder setCounterName(String counterName)
		{
			this.counterName = counterName;
			return this;
		}

		CounterBuilder setOldValue(Float oldValue)
		{
			this.oldValue = oldValue;
			return this;
		}

		CounterBuilder setCounterValue(Float counterValue)
		{
			this.counterValue = counterValue;
			return this;
		}

		CounterBuilder setCreated(Date created)
		{
			this.created = created;
			return this;
		}

		CounterBuilder setUpdated(Date updated)
		{
			this.updated = updated;
			return this;
		}

		CounterBuilder setMaxValue(Float maxValue)
		{
			this.maxValue = maxValue;
			return this;
		}

		Counter build()
		{
			Counter counter = new Counter();
			counter.counterName = counterName;
			counter.maxValue = maxValue;
			counter.counterValue = counterValue;
			counter.oldValue = oldValue;
			counter.created = created;
			counter.updated = updated;

			return counter;
		}
	}
}
