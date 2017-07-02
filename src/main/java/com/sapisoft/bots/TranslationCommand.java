package com.sapisoft.bots;


import com.sapisoft.translator.Translation;

import java.util.Locale;

/**
 * Created by rettpop on 2017-07-02.
 */
public class TranslationCommand
{
	private final Long targetChannelId;

	private final Translation translation;

	private TranslationCommand(Long targetChannelId, Translation translation)
	{
		this.targetChannelId = targetChannelId;
		this.translation = translation;
	}

	public static TranslationCommand createTranslation(Long targetChannelId, Translation translation)
	{
		return new TranslationCommand(targetChannelId, translation);
	}

	public Long targetChannelId()
	{
		return targetChannelId;
	}

	public Translation translation()
	{
		return translation;
	}

}
