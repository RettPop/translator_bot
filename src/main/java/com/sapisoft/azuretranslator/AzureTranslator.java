package com.sapisoft.azuretranslator;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

/**
 * Created by eviazhe on 2017-05-29.
 */
public class AzureTranslator implements Translator
{
	private static final Logger LOG = LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static final String SECRETS_GROUP = "com.sapisoft.azuretranslator";
    private String _subscription;

    @Override
    public List<Locale> supportedLanguages()
    {
	    String token = new Authorizator().GetAuthToken(getSubscription());
	    LOG.debug("Token received: {}", token);
	    if(token != null)
	    {
		    try
		    {
			    List<Locale> languages = sendLanguagesToTranslateRequest(token);
			    return languages;
		    }
		    catch (URISyntaxException | IOException e)
		    {
			    LOG.debug("Error requesting languages list: ", e);
		    }
	    }

	    return null;
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
//        URIBuilder builder = new URIBuilder("https://api.microsofttranslator.com/V2/Http.svc/GetLanguagesForTranslate");
	    URIBuilder builder = new URIBuilder("https://api.microsofttranslator.com/V2/Http.svc/GetLanguageNames?locale=en");
//        Locale sourceLocale = Locale.forLanguageTag(sourceMessage.getSourceLocale() == null ? "" : sourceMessage.getSourceLocale().getLanguage());
//        Locale destLocale = Locale.forLanguageTag(sourceMessage.getDestinationLocale() == null ? "en" : sourceMessage.getDestinationLocale().getLanguage());
//
//        builder.addParameter("text", sourceMessage.getSourceText());
//        builder.addParameter("from", sourceLocale.getLanguage());
//        builder.addParameter("to", destLocale.getLanguage());
        builder.addParameter("contentType", "text/xml");
        builder.addParameter("Accept", "text/text");
//	    builder.addParameter("localeCode", "en");
//	    builder.addParameter("locale", "en-US");
        URI uri = builder.build();

        HttpGet request = new HttpGet(uri);
        request.setHeader("Authorization", "Bearer " + token);
//        request.setHeader("locale" ,"en-US");
//	    request.setHeader("localeCode" ,"en");

        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();

        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        {
            String result = EntityUtils.toString(entity);
            LOG.debug("Reseived response: {}", result);

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

    public static void main(String[] args)
    {
        AzureTranslator translator = new AzureTranslator();
//        Translation text = translator.translate(Translation.SimpleTranslation("15:e juni, 2017 Bolotto med snabb inflytt Nytt koncept för dig om vill flytta in snabbt."));
        List<Locale> languages = translator.supportedLanguages();

        System.out.printf(languages.toString());
    }
}
