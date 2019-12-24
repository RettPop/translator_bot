package com.sapisoft.azuretranslator;

import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sapisoft.secrets.ResourcesSecretsManager;
import com.sapisoft.secrets.SimpleSecret;
import com.sapisoft.translator.Translation;
import com.sapisoft.translator.Translator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;

/**
 * Created by eviazhe on 2017-05-29.
 */
public class AzureTranslator implements Translator
{
	private static final Logger LOG = LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static final String SECRETS_GROUP = "com.sapisoft.azuretranslator";
	private static final Duration SUPPORTED_LANGUAGES_UPDATE_DURATION = Duration.ofHours(24);
	private static final String SITETRANSLATOR_MICROSOFTTRANSLATOR = "https://www.microsofttranslator.com/bv.aspx";
	private static final String TRANSLATE_SERVICE_BASE_URL = "https://api.cognitive.microsofttranslator.com/";

	private String _subscription;
    private List<Locale> _supportedLaguages;
    private Instant _lastSupportedLanguagesFetchTime;

    @Override
    public List<Locale> supportedLanguages()
    {
//	    String token = new Authorizator().GetAuthToken(getSubscription());
//	    LOG.debug("Token received: {}*", token.substring(1, 5));

	    if(_supportedLaguages != null &&
			    (Duration.between(Instant.now(), _lastSupportedLanguagesFetchTime)
					    .compareTo(SUPPORTED_LANGUAGES_UPDATE_DURATION) > 0))
	    {
	    	return _supportedLaguages;
	    }

	    List<Locale> languages = null;

	    try
	    {
		    languages = sendLanguagesToTranslateRequest();
		    if(languages != null && !languages.isEmpty())
		    {
			    _supportedLaguages = languages;
			    _lastSupportedLanguagesFetchTime = Instant.now();
		    }
	    }
	    catch (URISyntaxException | IOException e)
	    {
		    LOG.debug("Error requesting languages list: ", e);
	    }

	    return _supportedLaguages;
    }

    @Override
    public Map<Locale, List<Locale>> supportedDirections()
    {
        return null;
    }

    @Override
    public Translation translate(Translation message)
    {
        String token = new Authorizator().GetAuthToken(getSubscription());
        LOG.debug("Token received: {}", token);
        Translation resultTranslation = message;
        if(token != null)
        {
            try
            {
                resultTranslation = sendTranslationRequest(message, token);
            }
            catch (Exception e)
            {
                LOG.debug("Error translating: ", e);
            }
        }

        return resultTranslation;
    }

	@Override
	public String pageTranslationURL(String sourceURL, Translation message)
	{
		String encodedURL;
		try
		{
			encodedURL = URLEncoder.encode(sourceURL, StandardCharsets.UTF_8.toString());
		}
		catch (UnsupportedEncodingException e)
		{
			encodedURL = sourceURL;
		}

		String formURL = new StringBuilder(SITETRANSLATOR_MICROSOFTTRANSLATOR)
				.append("?")
				.append("from=").append(message.getSourceLocale())
				.append("&")
				.append("to=").append(message.getDestinationLocale())
				.append("&")
				.append("a=").append(encodedURL)
				.toString();

		return formURL;
	}

	private String getSubscription()
    {
        if(_subscription == null)
        {
            ResourcesSecretsManager secretsManager = new ResourcesSecretsManager("/secrets/keys.json");
            SimpleSecret simpleSecret = secretsManager.getSecret("subscription", SECRETS_GROUP);
            if(simpleSecret != null)
            {
                _subscription = simpleSecret.getSecret();
            }
        }

        return _subscription;
    }

	public static class RequestBody {
		String Text;

		public RequestBody(String text) {
			this.Text = text;
		}
	}

	private Translation sendTranslationRequest(Translation sourceMessage, String token) throws URISyntaxException, IOException
	{
		URIBuilder uriBuilder = new URIBuilder(TRANSLATE_SERVICE_BASE_URL + "translate");

		Locale reqSourceLocale = Locale.forLanguageTag(sourceMessage.getSourceLocale() == null ? "" :
				sourceMessage.getSourceLocale().getLanguage());
		Locale reqDestLocale = Locale.forLanguageTag(sourceMessage.getDestinationLocale() == null ? "en" :
				sourceMessage.getDestinationLocale().getLanguage());

		uriBuilder.addParameter("api-version", "3.0");
		uriBuilder.addParameter("from", reqSourceLocale.getLanguage());
		uriBuilder.addParameter("to", reqDestLocale.getLanguage());
		URI serviceUri = uriBuilder.build();

		List<RequestBody> objList = new ArrayList<>();
		objList.add(new RequestBody(sourceMessage.getSourceText()));
		String content = new Gson().toJson(objList);

		HttpPost request = new HttpPost(serviceUri);
		request.setHeader(CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
		request.setHeader(AUTHORIZATION, "Bearer " + token);
		request.setHeader("X-ClientTraceId", java.util.UUID.randomUUID().toString());
		// set request body with source text
		HttpEntity entity = new ByteArrayEntity(content.getBytes("UTF-8"));
		request.setEntity(entity);

		// sending request
		HttpClient client = HttpClients.createDefault();
		HttpResponse response = client.execute(request);

		LOG.debug("Received translation response: {}", response.toString());

		// parsing response
		String respEntity = EntityUtils.toString(response.getEntity());
		LOG.debug("Translation response body: {}", respEntity);
		JsonObject respJsonBody = new JsonParser().parse(respEntity)
				.getAsJsonArray()
				.get(0)
				.getAsJsonObject();

		JsonObject respTranslation = respJsonBody.getAsJsonArray("translations")
				.get(0)
				.getAsJsonObject();

		String respResultText = respTranslation.get("text").getAsString();
		Locale respDestLocale = Locale.forLanguageTag(respTranslation.get("to").getAsString());

		Locale respSrcLocale = reqSourceLocale;
		// trying to get retreive source locale from the response
		JsonObject respDetects = respJsonBody.getAsJsonObject("detectedLanguage");
		if( respDetects != null )
		{
			respSrcLocale = Locale.forLanguageTag(respDetects.get("language").getAsString());
		}

		Translation resultTranslation = Translation.FullTranslation(respSrcLocale,
				respDestLocale,
				sourceMessage.getSourceText(),
				respResultText);

		return resultTranslation;
	}

	private List<Locale> sendLanguagesToTranslateRequest() throws URISyntaxException, IOException
	{
		URIBuilder uriBuilder = new URIBuilder("https://api.cognitive.microsofttranslator.com/languages?api-version=3.0");
		uriBuilder.addParameter("scope", "translation");
		URI serviceUri = uriBuilder.build();

		HttpGet request = new HttpGet(serviceUri);
		request.setHeader(CONTENT_TYPE, MediaType.JSON_UTF_8.toString());

		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(request);

		List<Locale> languages = null;

		if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
		{
			HttpEntity entity = response.getEntity();
			String responseBody = EntityUtils.toString(entity);
			LOG.debug("Received response: {}", responseBody);
			languages = new ArrayList<>();
			try
			{
				JsonObject translations = new JsonParser().parse(responseBody)
						.getAsJsonObject()
						.getAsJsonObject("translation");

				for(Map.Entry<String, JsonElement> oneTranslation : translations.entrySet())
				{
					String langCode = oneTranslation.getKey();
					String langName = oneTranslation.getValue().getAsJsonObject().get("name").getAsString();
					Locale locale = new Locale(langName, langCode, langCode);
					languages.add(locale);
				}
			}
			catch (Exception e)
			{
				LOG.error("Error parsing translation engine answer: ", e);
			}
		}

		return languages;
	}

	public static void main(String[] args)
    {
        AzureTranslator translator = new AzureTranslator();
        Translation text = translator.translate(Translation.SimpleTranslation("15:e juni, 2017 Bolotto med snabb inflytt Nytt koncept för dig om vill flytta in snabbt."));
	    System.out.println(text.getResultText());
//	    List<Locale> languages = translator.supportedLanguages();
//	    System.out.println(languages.toString());

//	    languages = translator.supportedLanguages();
//	    System.out.println(languages.toString());
    }
}
