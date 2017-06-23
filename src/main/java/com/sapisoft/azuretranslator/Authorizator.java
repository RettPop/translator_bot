package com.sapisoft.azuretranslator;

/**
 * Created by rettpop on 2017-06-18.
 */


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

//import static com.sapisoft.azuretranslator.AzureTranslator.SUBSCRIPTION;

public class Authorizator
{

    public String GetAuthToken(String subscription)
    {
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
                return EntityUtils.toString(entity);
            }
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
