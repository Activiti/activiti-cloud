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
package org.activiti.cloud.services.notifications.qraphql.ws.security;

import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.services.identity.keycloak.KeycloakProperties;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.representations.AccessToken;

public class KeycloakAccessTokenVerifier {

    private final KeycloakProperties config;
    private final ConcurrentHashMap<String, PublicKey> publicKeys = new ConcurrentHashMap<>();
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public KeycloakAccessTokenVerifier(KeycloakProperties config) {
        this.config = config;
    }

    /**
     * Verifies a token against a keycloak instance
     * @param tokenString the string representation of the jws token
     * @return a validated keycloak AccessToken
     * @throws VerificationException when the token is not valid
     */
    @SuppressWarnings("deprecation")
    public AccessToken verifyToken(String tokenString) throws VerificationException {

        TokenVerifier<AccessToken> tokenVerifier = TokenVerifier.create(tokenString, AccessToken.class);

        PublicKey pk = getPublicKey(tokenVerifier.getHeader());

        return tokenVerifier.withDefaultChecks()
                            .realmUrl(getRealmUrl())
                            .publicKey(pk)
                            .verify()
                            .getToken();
    }
    
    protected PublicKey getPublicKey(JWSHeader jwsHeader) {
        return publicKeys.computeIfAbsent(getRealmCertsUrl(),
                                          (url) -> retrievePublicKeyFromCertsEndpoint(url, jwsHeader));
    }

    @SuppressWarnings("unchecked")
    protected PublicKey retrievePublicKeyFromCertsEndpoint(String realmCertsUrl, JWSHeader jwsHeader) {
        try {
            Map<String, Object> certInfos = objectMapper.readValue(new URL(realmCertsUrl).openStream(), Map.class);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) certInfos.get("keys");

            Map<String, Object> keyInfo = null;
            for (Map<String, Object> key : keys) {
                String kid = (String) key.get("kid");
                if (jwsHeader.getKeyId().equals(kid)) {
                    keyInfo = key;
                    break;
                }
            }

            if (keyInfo == null) {
                return null;
            }

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            String modulusBase64 = (String) keyInfo.get("n");
            String exponentBase64 = (String) keyInfo.get("e");
            Decoder urlDecoder = Base64.getUrlDecoder();
            BigInteger modulus = new BigInteger(1, urlDecoder.decode(modulusBase64));
            BigInteger publicExponent = new BigInteger(1, urlDecoder.decode(exponentBase64));

            return keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getRealmUrl() {
        return String.format("%s/realms/%s", config.getAuthServerUrl(), config.getRealm());
    }

    public String getRealmCertsUrl() {
        return getRealmUrl() + "/protocol/openid-connect/certs";
    }

}