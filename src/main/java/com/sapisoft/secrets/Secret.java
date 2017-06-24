package com.sapisoft.secrets;

/**
 * Created by rettpop on 2017-06-21.
 */
public interface Secret<T>
{
    String getSecretName();
    T getSecret();
    void setSecret(T secret);
}
