package com.sapisoft.googletranslator;

import com.sapisoft.translator.Translation;
import com.sapisoft.translator.Translator;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
		String sourceLocale = "auto";
		if(null != message.getSourceLocale()) {
			sourceLocale = message.getSourceLocale().toString();
		}

		String encodedURL;
		try
		{
			encodedURL = URLEncoder.encode(sourceURL, StandardCharsets.UTF_8.toString());
		}
		catch (UnsupportedEncodingException e)
		{
			encodedURL = sourceURL;
		}

		String formURL = new StringBuilder(SITETRANSLATOR_GOOGLETRANSLATOR)
				.append("?")
				.append("sl=")
				.append(sourceLocale)
				.append("&")
				.append("tl=").append(message.getDestinationLocale())
				.append("&js=y&prev=_t&hl=en&ie=UTF-8")
				.append("&")
				.append("u=").append(encodedURL)
				.toString();

		return formURL;
	}
}
