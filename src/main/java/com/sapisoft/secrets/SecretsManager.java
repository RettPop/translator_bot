package com.sapisoft.secrets;

/**
 * Created by rettpop on 2017-06-21.
 */
public interface SecretsManager<T>
{
    T getSecret(String secretName, String secretsGroup);
    default void saveSecret(String secretName, String secretGroup, T secret)
    {
        return;
    }
}
