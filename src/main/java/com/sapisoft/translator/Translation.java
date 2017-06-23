package com.sapisoft.translator;

import java.util.Locale;

/**
 * Created by rettpop on 2017-06-20.
 */
public class Translation
{
    private Locale sourceLocale;
    private Locale destinationLocale;
    private String sourceText;
    private String resultText;

    public static Translation SourceTranslation(Locale sourceLocale, Locale destinationLocale, String sourceText)
    {
        Translation sourceTranslation = new Translation();
        sourceTranslation.sourceLocale = sourceLocale;
        sourceTranslation.destinationLocale = destinationLocale;
        sourceTranslation.sourceText = sourceText;

        return sourceTranslation;
    }

    public static Translation FullTranslation(Locale sourceLocale, Locale destinationLocale, String sourceText, String resultText)
    {
        Translation sourceTranslation = new Translation();
        sourceTranslation.sourceLocale = sourceLocale;
        sourceTranslation.destinationLocale = destinationLocale;
        sourceTranslation.sourceText = sourceText;
        sourceTranslation.resultText= resultText;

        return sourceTranslation;
    }

    public static Translation SimpleTranslation(String sourceText)
    {
        Translation sourceTranslation = new Translation();
        sourceTranslation.sourceText = sourceText;

        return sourceTranslation;
    }

    public static TranslationBuilder getBuilder()
    {
        return new TranslationBuilder();
    }

    public Locale getSourceLocale() {
        if (sourceText == null) {
            return Locale.forLanguageTag("");
        }
        return sourceLocale;
    }

    public Locale getDestinationLocale() {
        if (destinationLocale == null) {
            return Locale.forLanguageTag("");
        }
        return destinationLocale;
    }

    public String getSourceText() {
        return sourceText;
    }

    public String getResultText() {
        return resultText;
    }

    public static class TranslationBuilder
    {
        private Locale sourceLocale;
        private Locale destinationLocale;
        private String sourceText;
        private String resultText;

        public TranslationBuilder from(Translation source)
        {
            sourceLocale = source.getSourceLocale();
            destinationLocale = source.getDestinationLocale();
            sourceText = source.getSourceText();
            resultText = source.resultText;

            return this;
        }

        public TranslationBuilder()
        {

        }

        public Translation build()
        {
            return Translation.FullTranslation(sourceLocale, destinationLocale, sourceText, resultText);
        }

        public TranslationBuilder sourceLocale(Locale locale)
        {
            sourceLocale = locale;
            return this;
        }

        public TranslationBuilder destinationLocale(Locale locale)
        {
            destinationLocale = locale;
            return this;
        }

        public TranslationBuilder sourceText(String text)
        {
            sourceText = text;
            return this;
        }

        public TranslationBuilder resultText(String text)
        {
            resultText = text;
            return this;
        }

    }
}
