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
import com.tw.go.plugin.provider.DefaultProvider;
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

        if (StringUtils.isEmpty(endPointToUse)) {
            endPointToUse = System.getProperty("go.plugin.build.status.bitbucket.endpoint");
        }
        if (StringUtils.isEmpty(usernameToUse)) {
            usernameToUse = System.getProperty("go.plugin.build.status.bitbucket.username");
        }
        if (StringUtils.isEmpty(passwordToUse)) {
            passwordToUse = System.getProperty("go.plugin.build.status.bitnucket.password");
        }

        String updateURL = String.format("%s/2.0/repositories/%s/1.0/commit/%s/statuses/build", endPointToUse, revision);

        Map<String, String> params = new HashMap<String, String>();
        params.put("state", getState(result));
        params.put("key", branch);
        params.put("name", branch);
        params.put("url", trackbackURL);
        params.put("description", "");
        String requestBody = new GsonBuilder().create().toJson(params);
        String accessToken = httpClient.getToken(AuthenticationType.BASIC, usernameToUse, passwordToUse);

        httpClient.postRequest(updateURL, accessToken, requestBody);
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
            state = FAILED_STATE;
        }
        return state;
    }
}
