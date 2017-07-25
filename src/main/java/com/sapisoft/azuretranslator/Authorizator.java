package com.sapisoft.azuretranslator;

/**
 * Created by rettpop on 2017-06-18.
 */


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

class Authorizator
{
    private static final Logger LOG = LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static final int TOKEN_VALIDITY_PERIOD_MS = 10000;

	private String _authToken;
	private long lastTokenRequest;

    String GetAuthToken(String subscription)
    {
        // kinda cache
        if(System.currentTimeMillis() - lastTokenRequest < TOKEN_VALIDITY_PERIOD_MS && _authToken != null)
        {
            return _authToken;
        }

        HttpClient httpClient = HttpClients.createDefault();

        try
        {
            URIBuilder builder = new URIBuilder("https://api.cognitive.microsoft.com/sts/v1.0/issueToken");
            URI uri = builder.build();
            HttpPost request = new HttpPost(uri);
            request.setHeader("Ocp-Apim-Subscription-Key", subscription);

            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
            {
                lastTokenRequest = System.currentTimeMillis();
                _authToken = EntityUtils.toString(entity);
                return _authToken;
            }
        }
        catch (URISyntaxException | IOException e)
        {
            LOG.error("Error requesting authorization token", e);
        }

        return null;
    }

//    public static void main(String[] args)
//    {
//        Authorizator auth = new Authorizator();
//        String token = auth.GetAuthToken(SUBSCRIPTION);
//        System.out.printf(token);
//    }
}
