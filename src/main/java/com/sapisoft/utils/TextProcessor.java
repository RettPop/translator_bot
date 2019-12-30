package com.sapisoft.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextProcessor
{
	static public List<String> splitByParagraphs(String sourceText)
	{
		Pattern pattern = Pattern.compile("^.*$", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(sourceText);
		List<String> paragraphs = new ArrayList<>();
		while(matcher.find())
		{
			paragraphs.add(matcher.group(0));
		}

		return paragraphs;
	}
	static public List<String> splitBySentences(String sourceText)
	{
		Pattern pattern = Pattern.compile("\\.\\s+\\S+");
		Matcher matcher = pattern.matcher(sourceText);
		if(!matcher.find())
		{
			return Collections.singletonList(sourceText);
		}

		ArrayList<Integer> startIndexes = new ArrayList<>();
		do
		{
			startIndexes.add(matcher.start());
		}
		while(matcher.find());

		List<String> sentences = new ArrayList<>();
		int prevPosition = 0;
		for(int idx = 0; idx < startIndexes.size(); idx++)
		{
			sentences.add(sourceText.substring(prevPosition, startIndexes.get(idx) + 1));
			prevPosition = startIndexes.get(idx) + 2;
		}
		sentences.add(sourceText.substring(prevPosition));

		return sentences;
	}
}
