package com.sapisoft.secrets;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Created by rettpop on 2017-06-21.
 */
public class ResourcesSecretsManager implements SecretsManager<SimpleSecret>
{
    private static final Logger LOG = LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private String _fileName;

    public ResourcesSecretsManager(String fileName)
    {
        _fileName = fileName;
    }

    @Override
    public SimpleSecret getSecret(String secretName, String secretsGroup)
    {
        SimpleSecret secret = null;
        try
        {
            InputStream in = Files.exists(FileSystems.getDefault().getPath(_fileName)) ?
                        new FileInputStream(new File(_fileName))
                        : getClass().getResourceAsStream(_fileName);

            JsonParser parser = new JsonParser();
            JsonObject obj = parser.parse(new BufferedReader(new InputStreamReader(in)))
                    .getAsJsonObject();
            JsonObject group = obj.get(secretsGroup)
                    .getAsJsonObject();
            String secretValue = group.get(secretName)
                    .getAsString();

            secret = SimpleSecret.getBuilder()
                    .secretName(secretName)
                    .secretValue(secretValue)
                    .build();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            LOG.debug("Error reading secrets file: {}", e);
        }


        return secret;
    }

    @Override
    public void saveSecret(String secretName, String secretGroup, SimpleSecret secret)
    {
        throw new NotImplementedException();
    }
}
