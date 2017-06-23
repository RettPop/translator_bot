package com.sapisoft.translator;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import sun.util.locale.LanguageTag;

/**
 * Created by eviazhe on 2017-05-29.
 */
public interface Translator
{
    List<Locale> supportedLanguages();
    Map<Locale, List<Locale>> supportedDirections();
    Translation translate(Translation message);
}
