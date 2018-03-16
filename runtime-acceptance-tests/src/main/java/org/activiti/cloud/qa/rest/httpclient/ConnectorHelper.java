/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.qa.rest.httpclient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

import org.activiti.cloud.qa.model.AuthToken;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import static org.assertj.core.api.Assertions.*;

public class ConnectorHelper {

    private static CloseableHttpClient connect() {
        return HttpClients.createDefault();
    }

    private static Header[] headers(String authToken) {

        return new Header[]{new BasicHeader("Content-Type",
                                            "application/json"), new BasicHeader("Authorization",
                                                                                 "Bearer " + authToken)};
    }

    public static String get(String url, Map<String, String> params, AuthToken authToken) throws URISyntaxException, IOException{
        CloseableHttpClient client = ConnectorHelper.connect();

        URIBuilder builder = new URIBuilder(url);
        if(params != null){
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.setParameter(entry.getKey(), entry.getValue());

            }
        }

        HttpGet httpGet = new HttpGet(builder.build());

        httpGet.setHeaders(headers(authToken.getAccess_token()));

        CloseableHttpResponse httpResponse = client.execute(httpGet);

        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);

        String responseBody = EntityUtils.toString(httpResponse.getEntity());

        ConnectorHelper.close(client,
                              httpResponse);

        return responseBody;
    }

    public static String postJson(String url,
                                  Object object,
                                  AuthToken authToken) throws IOException {
        CloseableHttpClient client = ConnectorHelper.connect();
        HttpPost httpPost = new HttpPost(url);

        StringEntity entity = new StringEntity(Serializer.toJsonString(object));
        httpPost.setEntity(entity);
        httpPost.setHeaders(headers(authToken.getAccess_token()));

        CloseableHttpResponse httpResponse = client.execute(httpPost);

        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);

        String responseBody = EntityUtils.toString(httpResponse.getEntity());

        ConnectorHelper.close(client,
                              httpResponse);

        return responseBody;
    }

    public static String postForm(String url,
                                  Map<String, String> form,
                                  AuthToken authToken) throws IOException {
        CloseableHttpClient client = ConnectorHelper.connect();
        ArrayList<NameValuePair> postParameters;
        HttpPost httpPost = new HttpPost(url);

        postParameters = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            postParameters.add(new BasicNameValuePair(entry.getKey(),
                                                      entry.getValue()));
        }

        httpPost.setEntity(new UrlEncodedFormEntity(postParameters,
                                                    "UTF-8"));
        //if it is calling the auth service, there is not token yet
        if(authToken != null){
            httpPost.setHeaders(headers(authToken.getAccess_token()));
        }

        CloseableHttpResponse httpResponse = client.execute(httpPost);

        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);

        String responseBody = EntityUtils.toString(httpResponse.getEntity());

        ConnectorHelper.close(client,
                              httpResponse);

        return responseBody;
    }

    private static void close(CloseableHttpClient closeableHttpClient,
                             CloseableHttpResponse closeableHttpResponse) {
        if (closeableHttpResponse != null) {
            try {
                closeableHttpResponse.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            closeableHttpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
