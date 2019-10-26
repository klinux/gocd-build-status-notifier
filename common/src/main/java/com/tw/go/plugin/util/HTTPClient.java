/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tw.go.plugin.util;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.net.URI;

public class HTTPClient {
    public String getRequest(String getURL, String accessToken) throws Exception {
        CloseableHttpClient httpClient = null;
        try {
            HttpGet request = new HttpGet(getURL);
            HttpClientContext localContext = HttpClientContext.create();
            httpClient = HttpClients.custom().build();

            HttpResponse response = httpClient.execute(request, localContext);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode > 204) {
                throw new RuntimeException("Error occurred. Status Code: " + statusCode);
            }
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    public void postRequest(String updateURL, String accessToken, String requestBody) throws Exception {
        CloseableHttpClient httpClient = null;
        try {
            HttpPost request = new HttpPost(updateURL);
            request.addHeader("content-type", "application/json");
            request.setEntity(new StringEntity(requestBody));
            HttpClientContext localContext = HttpClientContext.create();
            httpClient = HttpClients.custom().build();

            HttpResponse response = httpClient.execute(request, localContext);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode > 204) {
                throw new RuntimeException("Error occurred. Status Code: " + statusCode);
            }
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    public String getToken(AuthenticationType authenticationType, String accessKey, String secretKey) throws Exception {
        CloseableHttpClient httpClient = null;

        String accessToken;
        String requestBody = "grant_type=client_credentials";
        String updateURL = "https://bitbucket.org/site/oauth2/access_token";

        try {
            HttpPost request = new HttpPost(updateURL);
            request.addHeader("content-type", "application/x-www-form-urlencoded");
            request.setEntity(new StringEntity(requestBody));

            HttpHost target = getHttpHost(updateURL);
            AuthCache authCache = getAuthCache(authenticationType, target);
            HttpClientContext localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);

            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(target), new UsernamePasswordCredentials(accessKey, secretKey));
            httpClient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();

            HttpResponse response = httpClient.execute(request, localContext);

            JSONObject jsonObject = new JSONObject(response.getEntity().toString());
            Object obj = jsonObject.get("access_token");

            accessToken = obj.toString();

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode > 204) {
                throw new RuntimeException("Error occurred. Status Code: " + statusCode);
            }
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return accessToken;
    }

    private HttpHost getHttpHost(String url) throws Exception {
        URI uri = new URI(url);
        return new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
    }

    private AuthCache getAuthCache(AuthenticationType authenticationType, HttpHost target) {
        AuthCache authCache = new BasicAuthCache();
        if (authenticationType == AuthenticationType.BASIC) {
            authCache.put(target, new BasicScheme());
        } else {
            authCache.put(target, new DigestScheme());
        }
        return authCache;
    }
}
