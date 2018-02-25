package com.sapisoft.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.stream.IntStream;

//Taken from https://github.com/akimateras/shorten-url-expander-java.git

public class ShortenUrlExpander {
    private static class RecursiveTrialExceededException extends RuntimeException {
        private static final long serialVersionUID = 3574365693364595932L;
    }

    private static final Proxy DEFAULT_PROXY = Proxy.NO_PROXY;
    private static final int DEFAULT_ACCEPTABLE_DEPTH = 3;
    private static final boolean DEFAULT_USE_HEAD_METHOD = true;
    private static final String[] HEX_TABLE = IntStream.concat(IntStream.rangeClosed(0x80, 0xFF), IntStream.rangeClosed(0x00, 0x7F))
        .mapToObj(Integer::toHexString)
        .map(String::toUpperCase)
        .toArray(String[]::new);

    private final Proxy proxy;

    public ShortenUrlExpander(Proxy proxy) {
        this.proxy = proxy;
    }

    public ShortenUrlExpander() {
        this(DEFAULT_PROXY);
    }

    public String expand(String url, int depth, boolean useHeadMethod) throws IOException {
        if (depth <= 0) {
            String message = MessageFormat.format("Argument ''depth'' must be more than 0. Given value is ''{0}''.", depth);
            throw new IllegalArgumentException(message);
        }

        try {
            // To ensure terminal of the redirect chain, required to try to resolve `depth + 1` times.
            return expandInternal(url, depth + 1, useHeadMethod);
        } catch (RecursiveTrialExceededException e) {
            String message = generateErrorMessage(url, "Recursive resolving got exceeded to the given limit: " + depth);
            throw new IOException(message);
        }
    }

    public String expand(String url, int depth) throws IOException {
        return expand(url, depth, DEFAULT_USE_HEAD_METHOD);
    }

    public String expand(String url, boolean useHeadMethod) throws IOException {
        return expand(url, DEFAULT_ACCEPTABLE_DEPTH, useHeadMethod);
    }

    public String expand(String url) throws IOException {
        return expand(url, DEFAULT_ACCEPTABLE_DEPTH, DEFAULT_USE_HEAD_METHOD);
    }

    private String expandInternal(String url, int depth, boolean useHeadMethod) throws IOException {
        URLConnection urlConnection;
        try {
            urlConnection = new URL(url).openConnection(proxy);
        } catch (Exception e) {
            String message = generateErrorMessage(url, "Got error while preparing to connect");
            throw new IOException(message, e);
        }
        if (!(urlConnection instanceof HttpURLConnection)) {
            return url;
        }

        HttpURLConnection httpUrlConnection;
        try {
            httpUrlConnection = (HttpURLConnection) urlConnection;
            httpUrlConnection.setInstanceFollowRedirects(false);
            if (useHeadMethod) {
                httpUrlConnection.setRequestMethod("HEAD");
            }
            httpUrlConnection.connect();
        } catch (Exception e) {
            String message = generateErrorMessage(url, "Could not connect to given URL");
            throw new IOException(message, e);
        }

        String location = httpUrlConnection.getHeaderField("Location");
        if (location == null || !location.startsWith("http")) {
            return httpUrlConnection.getURL().toExternalForm();
        }

        int statusCode;
        try {
            statusCode = httpUrlConnection.getResponseCode();
        } catch (Exception e) {
            String message = generateErrorMessage(url, "Could not get status code");
            throw new IOException(message, e);
        }

        if (statusCode == HttpURLConnection.HTTP_MOVED_PERM || statusCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String result;
            try {
                result = encode(location);
            } catch (Exception e) {
                String message = generateErrorMessage(url, "Got encoding error");
                throw new IOException(message, e);
            }

            try {
                if (depth < 0) {
                    throw new RecursiveTrialExceededException();
                }
                return expandInternal(result, depth - 1, useHeadMethod);
            } catch (IOException e) {
                // It is okay that expanded URL may be expired or invalid.
                return result;
            }
        }

        return location;
    }

    private static String encode(String url) throws UnsupportedEncodingException {
        byte[] bytes = url.getBytes("ISO-8859-1");
        StringBuilder result = new StringBuilder(bytes.length * 3);
        for (int i = 0; i < bytes.length; i++) {
            int character = bytes[i];
            if (character < 0) {
                result.append('%');
                result.append(HEX_TABLE[character + 128]);
            } else {
                result.append((char) character);
            }
        }
        return result.toString();
    }

    private static String generateErrorMessage(String url, String reasonText) {
        return MessageFormat.format("Failed to expand url ''{0}''. {1}.", url, reasonText);
    }
}
