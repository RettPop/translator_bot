package com.sapisoft.secrets;

import com.google.gson.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;

/**
 * Created by rettpop on 2017-06-21.
 */
public class ResourcesSecretsManager implements SecretsManager<SimpleSecret>
{
    private String _fileName;

    public ResourcesSecretsManager(String fileName)
    {
        _fileName = fileName;
    }

    @Override
    public SimpleSecret getSecret(String secretName, String secretsGroup)
    {
        SimpleSecret secret;
        InputStream in = getClass().getResourceAsStream(_fileName);

        JsonParser parser = new JsonParser();
        JsonObject obj = parser.parse(new BufferedReader(new InputStreamReader(in))).getAsJsonObject();
        JsonObject group = obj.get(secretsGroup).getAsJsonObject();
        String secretValue = group.get(secretName).getAsString();

        secret = SimpleSecret.getBuilder()
                .secretName(secretName)
                .secretValue(secretValue)
                .build();

        return secret;
    }

    @Override
    public void saveSecret(String secretName, String secretGroup, SimpleSecret secret)
    {
        throw new NotImplementedException();
    }
}
