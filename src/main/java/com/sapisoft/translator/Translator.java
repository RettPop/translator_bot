package com.sapisoft.translator;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by eviazhe on 2017-05-29.
 */
public interface Translator
{
    static public final Locale SWEDEN = Locale.forLanguageTag("sv");

    List<Locale> supportedLanguages();
    Map<Locale, List<Locale>> supportedDirections();
    Translation translate(Translation message);
}
