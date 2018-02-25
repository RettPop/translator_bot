package com.sapisoft.googletranslator;

import com.sapisoft.translator.Translation;
import com.sapisoft.translator.Translator;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GoogleTranslator implements Translator
{
	public static final String SITETRANSLATOR_GOOGLETRANSLATOR = "https://translate.google.com/translate";

	@Override
	public List<Locale> supportedLanguages()
	{
		throw new NotImplementedException();
	}

	@Override
	public Map<Locale, List<Locale>> supportedDirections()
	{
		throw new NotImplementedException();
	}

	@Override
	public Translation translate(Translation message)
	{
		throw new NotImplementedException();
	}

	@Override
	public String pageTranslationURL(String sourceURL, Translation message)
	{
		String formURL = new StringBuilder(SITETRANSLATOR_GOOGLETRANSLATOR)
				.append("?")
				.append("sl=auto")
				.append("&")
				.append("tl=").append(message.getDestinationLocale())
				.append("&js=y&prev=_t&hl=en&ie=UTF-8")
				.append("&")
				.append("u=").append(sourceURL)
				.toString();

		return formURL;
	}
}
