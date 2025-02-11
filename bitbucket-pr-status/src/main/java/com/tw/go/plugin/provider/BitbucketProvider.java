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

package com.tw.go.plugin.provider;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.tw.go.plugin.setting.DefaultPluginConfigurationView;
import com.tw.go.plugin.setting.PluginSettings;
import com.tw.go.plugin.util.AuthenticationType;
import com.tw.go.plugin.util.HTTPClient;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BitbucketProvider extends DefaultProvider {
    public static final String PLUGIN_ID = "bitbucket.pr.status";
    public static final String BITBUCKET_PR_POLLER_PLUGIN_ID = "git.fb";

    public static final String IN_PROGRESS_STATE = "INPROGRESS";
    public static final String SUCCESSFUL_STATE = "SUCCESSFUL";
    public static final String FAILED_STATE = "FAILED";
    public static final String CANCELED_STATE = "STOPPED";
    private static Logger LOGGER = Logger.getLoggerFor(BitbucketProvider.class);

    private HTTPClient httpClient;

    public BitbucketProvider() {
        super(new DefaultPluginConfigurationView());
        httpClient = new HTTPClient();
    }

    public BitbucketProvider(HTTPClient httpClient) {
        super(new DefaultPluginConfigurationView());
        this.httpClient = httpClient;
    }

    @Override
    public String pluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String pollerPluginId() {
        return BITBUCKET_PR_POLLER_PLUGIN_ID;
    }

    @Override
    public void updateStatus(String url, PluginSettings pluginSettings, String branch, String revision,
                             String pipelineStage, String result, String trackbackURL) throws Exception {
        String endPointToUse = pluginSettings.getEndPoint();
        String usernameToUse = pluginSettings.getUsername();
        String passwordToUse = pluginSettings.getPassword();
        String authURL = "https://bitbucket.org/site/oauth2/access_token";

        if (StringUtils.isEmpty(endPointToUse)) {
            endPointToUse = System.getProperty("go.plugin.build.status.bitbucket.endpoint");
        }
        if (StringUtils.isEmpty(usernameToUse)) {
            usernameToUse = System.getProperty("go.plugin.build.status.bitbucket.username");
        }
        if (StringUtils.isEmpty(passwordToUse)) {
            passwordToUse = System.getProperty("go.plugin.build.status.bitnucket.password");
        }

        String updateURL = String.format("%s/2.0/repositories/%s/commit/%s/statuses/build", endPointToUse,
                parseRepositoryName(url), revision);

        String description;
        switch(getState(result)) {
            case IN_PROGRESS_STATE:
                description = "The build is in progress.";
                break;
            case SUCCESSFUL_STATE:
                description = "This commit looks good.";
                break;
            case FAILED_STATE:
                description = "This commit has failed.";
                break;
            case CANCELED_STATE:
                description = "The build was canceled.";
                break;
            default:
                description = "We don't know about the statuses.";
        }

        String name = pipelineStage +  " \u00BB " + branch;

        String key;
        if (pipelineStage.length() > 40) {
            key = pipelineStage.substring(0, 40);
        } else {
            key = pipelineStage;
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("state", getState(result));
        params.put("key", key);
        params.put("name", name);
        params.put("url", trackbackURL);
        params.put("description", description);
        String requestBody = new GsonBuilder().create().toJson(params);
        LOGGER.info("Body: " + requestBody);

        String accessToken = httpClient.getBitBucketToken(authURL, AuthenticationType.BASIC, usernameToUse, passwordToUse);

        if (accessToken.isEmpty()) {
            LOGGER.error("It is not possible to get access token.");
        } else {
            httpClient.postBitbucketRequest(updateURL, accessToken, requestBody);
        }
    }

    public String parseRepositoryName(String repository) {
        String[] tempArray;
        String tempSlug;
        String owner;
        String repoSlug;
        String delimiter = "/";
        String slugDelimiter = "\\.";

        tempArray = repository.split(delimiter);
        tempSlug = tempArray[tempArray.length -1].split(slugDelimiter)[0];

        owner = tempArray[3];
        repoSlug = tempSlug;

        return owner + "/" + repoSlug;
    }

    @Override
    public List<Map<String, Object>> validateConfig(Map<String, Object> fields) {
        return new ArrayList<Map<String, Object>>();
    }

    public String getState(String result) {
        result = result == null ? "" : result;
        String state = IN_PROGRESS_STATE;
        if (result.equalsIgnoreCase("Passed")) {
            state = SUCCESSFUL_STATE;
        } else if (result.equalsIgnoreCase("Failed")) {
            state = FAILED_STATE;
        } else if (result.equalsIgnoreCase("Cancelled")) {
            state = CANCELED_STATE;
        }
        return state;
    }
}
