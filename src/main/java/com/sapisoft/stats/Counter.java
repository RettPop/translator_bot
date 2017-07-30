package com.sapisoft.stats;

import java.util.Date;

public class Counter
{
	private String counterName;
	private Float oldValue;
	private Float counterValue;
	private Date created;
	private Date updated;

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

	private Counter(String counterName, Float oldValue, Float counterValue, Date created, Date updated)
	{
		this.counterName = counterName;
		this.oldValue = oldValue;
		this.counterValue = counterValue;
		this.created = created;
		this.updated = updated;
	}

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

	public static class CounterBuilder
	{
		private String counterName;
		private Float oldValue = Float.NaN;
		private Float counterValue;
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

		Counter build()
		{
			return new Counter(counterName, oldValue, counterValue, created, updated);
		}
	}
}
