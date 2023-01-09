/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.identity.keycloak.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.crypto.impl.RSASSAProvider;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.activiti.cloud.services.common.security.jwt.validator.ValidationCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;

public class PublicKeyValidationCheck implements ValidationCheck {

    protected static final Logger LOGGER = LoggerFactory.getLogger(PublicKeyValidationCheck.class);
    private final String authServerUrl;
    private final String realm;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, PublicKey> publicKeys = new ConcurrentHashMap<>();

    public PublicKeyValidationCheck(String authServerUrl,
                                    String realm,
                                    ObjectMapper objectMapper) {
        this.authServerUrl = authServerUrl;
        this.realm = realm;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isValid(Jwt accessToken) {

        boolean result = false;

        JWSObject jwsObject = getJwsObject(accessToken);

        PublicKey publicKey = getPublicKey(jwsObject.getHeader());
        JWSAlgorithm algorithm = jwsObject.getHeader().getAlgorithm();

        if(isAlgorithmsSupported(algorithm)) {
            try {
                RSASSAVerifier verifier = new RSASSAVerifier((RSAPublicKey) publicKey);
                result = jwsObject.verify(verifier);
            } catch (JOSEException e) {
                LOGGER.error("Cannot verify RSA public key", e);
            }
        } else {
            result = true;
            LOGGER.error("Unsupported JWS algorithm " + algorithm + ", must be " + RSASSAProvider.SUPPORTED_ALGORITHMS);
        }

        return result;
    }

    private boolean isAlgorithmsSupported(JWSAlgorithm algorithm) {
        return RSASSAProvider.SUPPORTED_ALGORITHMS.contains(algorithm);
    }

    private JWSObject getJwsObject(Jwt accessToken) {
        JWSObject jwsObject = null;
        try {
            jwsObject = JWSObject.parse(accessToken.getTokenValue());
        } catch (java.text.ParseException e) {
            LOGGER.error("Cannot parse token", e);
        }
        return jwsObject;
    }

    private PublicKey getPublicKey(JWSHeader jwsHeader) {
        return publicKeys.computeIfAbsent(getRealmCertsUrl(),
                                          (url) -> retrievePublicKeyFromCertsEndpoint(url, jwsHeader));
    }

    private PublicKey retrievePublicKeyFromCertsEndpoint(String realmCertsUrl, JWSHeader jwsHeader) {
        try {
            Map<String, Object> certInfos = objectMapper.readValue(new URL(realmCertsUrl).openStream(), Map.class);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) certInfos.get("keys");

            Map<String, Object> keyInfo = null;
            for (Map<String, Object> key : keys) {
                String kid = (String) key.get("kid");
                if (jwsHeader.getKeyID().equals(kid)) {
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
            LOGGER.error("Cannot retrieve public key", e);
        }
        return null;
    }

    private String getRealmCertsUrl() {
        return getRealmUrl() + "/protocol/openid-connect/certs";
    }

    private String getRealmUrl() {
        return String.format("%s/realms/%s", authServerUrl, realm);
    }



}
