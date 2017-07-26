package com.sapisoft.azuretranslator;

import ch.qos.logback.core.util.TimeUtil;
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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;

/**
 * Created by eviazhe on 2017-05-29.
 */
public class AzureTranslator implements Translator
{
	private static final Logger LOG = LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static final String SECRETS_GROUP = "com.sapisoft.azuretranslator";
	private static final long SUPPORTED_LANGUAGES_UPDATE_PERIOD_MILLIS = TimeUnit.HOURS.toMillis(24); // 24 hours
	private String _subscription;
    private List<Locale> _supportedLaguages;
    private long _lastSupportedLanguagesFetchTime;

    @Override
    public List<Locale> supportedLanguages()
    {
	    String token = new Authorizator().GetAuthToken(getSubscription());
	    LOG.debug("Token received: {}", token);

	    if(_supportedLaguages != null && (System.currentTimeMillis() - _lastSupportedLanguagesFetchTime) < SUPPORTED_LANGUAGES_UPDATE_PERIOD_MILLIS)
	    {
	    	return _supportedLaguages;
	    }

		List<Locale> languages = null;

		if(token != null)
	    {
		    try
		    {
			    languages = sendLanguagesToTranslateRequest(token);
			    if(languages != null && !languages.isEmpty())
			    {
				    _supportedLaguages = sendLanguagesNamesRequest(token, languages);
				    _lastSupportedLanguagesFetchTime = System.currentTimeMillis();
			    }
		    }
		    catch (URISyntaxException | IOException e)
		    {
			    LOG.debug("Error requesting languages list: ", e);
		    }
	    }

	    return languages;
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
        String translation = message.getSourceText();
        if(token != null)
        {
            try
            {
                translation = sendTranslationRequest(message, token);
            }
            catch (URISyntaxException | IOException e)
            {
                LOG.debug("Error translating: ", e);
            }
        }

        return Translation.getBuilder()
                .from(message)
                .resultText(translation)
                .build();
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

	private String sendTranslationRequest(Translation sourceMessage, String token) throws URISyntaxException, IOException
	{
		URIBuilder builder = new URIBuilder("https://api.microsofttranslator.com/V2/Http.svc/Translate");
		Locale sourceLocale = Locale.forLanguageTag(sourceMessage.getSourceLocale() == null ? "" : sourceMessage.getSourceLocale().getLanguage());
		Locale destLocale = Locale.forLanguageTag(sourceMessage.getDestinationLocale() == null ? "en" : sourceMessage.getDestinationLocale().getLanguage());

		builder.addParameter("text", sourceMessage.getSourceText());
		builder.addParameter("from", sourceLocale.getLanguage());
		builder.addParameter("to", destLocale.getLanguage());
		builder.addParameter("contentType", "text/plain");
		builder.addParameter("Accept", "text/plain");
		URI uri = builder.build();

		HttpGet request = new HttpGet(uri);
		request.setHeader("Authorization", "Bearer " + token);

		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(request);
		HttpEntity entity = response.getEntity();

		if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
		{
			String result = EntityUtils.toString(entity);

			try
			{
				DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = docBuilder.parse(new InputSource(new StringReader(result)));
				XPath path = XPathFactory.newInstance().newXPath();
				XPathExpression expr = path.compile("/string");
				NodeList nodeList = (NodeList) expr.evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
				for (int idx = 0; idx < nodeList.getLength(); idx++)
				{
					Node node = nodeList.item(idx);
					if("string".equals(node.getNodeName()))
					{
						return node.getTextContent();
					}
				}
			}
			catch (XPathExpressionException | SAXException | ParserConfigurationException e)
			{
				LOG.debug("Error parsing translation engine answer: ", e);
			}
		}

		return null;
	}

	private List<Locale> sendLanguagesToTranslateRequest(String token) throws URISyntaxException, IOException
	{
		URIBuilder builder = new URIBuilder("https://api.microsofttranslator.com/v2/http.svc/GetLanguagesForTranslate");
		builder.addParameter(ACCEPT, APPLICATION_XML);
		URI uri = builder.build();

		HttpGet request = new HttpGet(uri);
		request.setHeader(AUTHORIZATION, "Bearer " + token);
		request.setHeader(CONTENT_TYPE, TEXT_XML);

		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(request);
		HttpEntity entity = response.getEntity();

		if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
		{
			String result = EntityUtils.toString(entity);
			LOG.debug("Received response: {}", result);

			try
			{
				DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = docBuilder.parse(new InputSource(new StringReader(result)));
				XPath path = XPathFactory.newInstance().newXPath();
				XPathExpression expr = path.compile("string");
				NodeList nodeList = (NodeList) expr.evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
				List<Locale> languages = new ArrayList<>();
				for (int idx = 0; idx < nodeList.getLength(); idx++)
				{
					Node node = nodeList.item(idx);
					if("string".equals(node.getNodeName()))
					{
						languages.add(Locale.forLanguageTag(node.getTextContent()));
					}
				}

				return languages;
			}
			catch (XPathExpressionException | SAXException | ParserConfigurationException e)
			{
				LOG.debug("Error parsing translation engine answer: ", e);
			}
		}

		return null;
	}

    private List<Locale> sendLanguagesNamesRequest(String token, List<Locale> locales) throws URISyntaxException, IOException
    {
	    URIBuilder builder = new URIBuilder("https://api.microsofttranslator.com/V2/Http.svc/GetLanguageNames?");
        builder.addParameter("contentType", "text/xml");
        builder.addParameter("Accept", "text/text");
	    builder.addParameter("locale", "en-US");
        URI uri = builder.build();

        HttpPost request = new HttpPost(uri);
        request.setHeader("Authorization", "Bearer " + token);
	    request.setHeader("Content-Type", "text/xml");
        StringBuilder xml = new StringBuilder("<ArrayOfstring xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" name=\"languageCodes\">\n");
	    for (Locale oneLocale : locales)
	    {
			xml.append("<string>")
					.append(oneLocale.getLanguage())
					.append("</string>");
	    }
	    xml.append("</ArrayOfstring>");

        HttpEntity postEntity = new ByteArrayEntity(xml.toString().getBytes("UTF-8"));
        request.setEntity(postEntity);
//        request.setHeader("locale" ,"en-US");
//	    request.setHeader("localeCode" ,"en");

        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();

        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        {
            String result = EntityUtils.toString(entity);
            LOG.debug("Received response: {}", result);

            try
            {
            	// parsing languages names from the response
                DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = docBuilder.parse(new InputSource(new StringReader(result)));
                XPath path = XPathFactory.newInstance().newXPath();
                XPathExpression expr = path.compile("string");
                NodeList nodeList = (NodeList) expr.evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
                List<String> names = new ArrayList<>();
                for (int idx = 0; idx < nodeList.getLength(); idx++)
                {
                    Node node = nodeList.item(idx);
                    if("string".equals(node.getNodeName()))
                    {
                    	String languageName = node.getTextContent();
                    	names.add(languageName);
                    }
                }

                List<Locale> languagesWithNames = new ArrayList<>();

                // if by some reason we have received not synchronized list of names, return original locales list as is
                if(names.size() != locales.size())
                {
                	return locales;
                }

	            for (int idx = 0; idx < locales.size(); idx++)
	            {
	            	Locale namedLocale = new Locale(locales.get(idx).getLanguage(), "", names.get(idx));
					languagesWithNames.add(namedLocale);
	            }

                return languagesWithNames;
            }
            catch (XPathExpressionException | SAXException | ParserConfigurationException e)
            {
            	LOG.debug("Error parsing translation engine answer: ", e);
            }
        }

        return null;
    }

    public static void main(String[] args)
    {
        AzureTranslator translator = new AzureTranslator();
//        Translation text = translator.translate(Translation.SimpleTranslation("15:e juni, 2017 Bolotto med snabb inflytt Nytt koncept för dig om vill flytta in snabbt."));
	    List<Locale> languages = translator.supportedLanguages();

	    System.out.printf(languages.toString());
    }
}
