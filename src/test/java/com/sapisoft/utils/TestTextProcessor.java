package com.sapisoft.utils;

import org.junit.Test;
import static org.fest.assertions.api.Assertions.assertThat;

import java.util.List;

import static org.junit.Assert.*;

public class TestTextProcessor
{
	@Test
	public void testShouldReturnParagraphsList()
	{
		String srcText =
				"" +
				"\nSentence 1.1." +
				"\nSentence 2.1. Sentence 2.2." +
				"\nSentence 3.1. Sentence 3.2. Sentence 3.3.";
		List<String> sentences = TextProcessor.splitByParagraphs(srcText);
		assertThat(sentences).hasSize(4);
		assertThat(TextProcessor.splitBySentences(sentences.get(0)))
				.hasSize(1);
		for(int idx = 1; idx < sentences.size(); idx++)
		{
			assertThat(TextProcessor.splitBySentences(sentences.get(idx)))
					.hasSize(idx);
		}
	}

	@Test
	public void testShouldReturnSingleParagraphAndSetOfSentences()
	{
		String srcText = "Sentence 3.1. Sentence 3.2. Sentence 3.3.";
		List<String> sentences = TextProcessor.splitByParagraphs(srcText);
		assertThat(sentences).hasSize(1);
		assertThat(TextProcessor.splitBySentences(sentences.get(0)))
				.hasSize(3);
	}

	@Test
	public void testShouldReturnSingleParagraphAndSingleSentence()
	{
		String srcText = "Sentence 1.1";
		List<String> sentences = TextProcessor.splitByParagraphs(srcText);
		assertThat(sentences).hasSize(1);
		assertThat(TextProcessor.splitBySentences(sentences.get(0)))
				.hasSize(1);
	}

	@Test
	public void testShouldReturnFullSentences()
	{
		String srcText = "Sentence 3.1. Sentence 3.2. Sentence 3.3.";
		List<String> sentences = TextProcessor.splitBySentences(srcText);
		assertThat(sentences.get(0)).isEqualTo("Sentence 3.1.");
		assertThat(sentences.get(1)).isEqualTo("Sentence 3.2.");
		assertThat(sentences.get(2)).isEqualTo("Sentence 3.3.");
	}
}