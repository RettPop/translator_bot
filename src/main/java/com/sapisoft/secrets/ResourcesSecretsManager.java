package com.sapisoft.secrets;

import com.google.gson.*;
import com.sapisoft.Secret;
import com.sapisoft.SecretsManager;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

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
        SimpleSecret secret = null;
        File file = new File(getClass().getClassLoader().getResource(_fileName).getFile());
        JsonParser parser = new JsonParser();
        try
        {
            JsonObject obj = parser.parse(new FileReader(file)).getAsJsonObject();
            JsonObject group = obj.get(secretsGroup).getAsJsonObject();
            String secretValue = group.get(secretName).getAsString();

            secret = SimpleSecret.getBuilder()
                    .secretName(secretName)
                    .secretValue(secretValue)
                    .build();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        return secret;
    }

    @Override
    public void saveSecret(String secretName, String secretGroup, SimpleSecret secret)
    {
        throw new NotImplementedException();
    }
}
