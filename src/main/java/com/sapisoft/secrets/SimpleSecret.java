package com.sapisoft.secrets;

import com.sapisoft.Secret;

/**
 * Created by rettpop on 2017-06-21.
 */
public class SimpleSecret implements Secret<String>
{
    private String secretName;
    private String secret;

    @Override
    public String getSecretName()
    {
        return secretName;
    }

    @Override
    public String getSecret()
    {
        return secret;
    }

    @Override
    public void setSecret(String secret) {

    }

    public SimpleSecret(String secretName, String secret) {
        this.secretName = secretName;
        this.secret = secret;
    }

    public static SimpleSecretBuilder getBuilder()
    {
        return new SimpleSecretBuilder();
    }

    public static class SimpleSecretBuilder {
        private String secretName;
        private String secret;

        public SimpleSecretBuilder secretName(String secretName) {
            this.secretName = secretName;
            return this;
        }

        public SimpleSecretBuilder secretValue(String secret) {
            this.secret = secret;
            return this;
        }

        public SimpleSecret build() {
            return new SimpleSecret(secretName, secret);
        }
    }
}
