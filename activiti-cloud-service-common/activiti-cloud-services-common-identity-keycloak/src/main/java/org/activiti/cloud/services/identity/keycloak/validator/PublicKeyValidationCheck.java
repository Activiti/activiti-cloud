package org.activiti.cloud.services.identity.keycloak.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
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

        if(publicKey != null) {
            try {
                result = jwsObject.verify(new RSASSAVerifier((RSAPublicKey) publicKey));
            } catch (JOSEException e) {
                LOGGER.error("Cannot verify RSA public key", e);
            }
        }

        return result;
    }

    private static JWSObject getJwsObject(Jwt accessToken) {
        JWSObject jwsObject = null;
        try {
            jwsObject = JWSObject.parse(accessToken.getTokenValue());
        } catch (java.text.ParseException e) {
            LOGGER.error("Cannot parse token", e);
        }
        return jwsObject;
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

    public String getRealmCertsUrl() {
        return getRealmUrl() + "/protocol/openid-connect/certs";
    }

    public String getRealmUrl() {
        return String.format("%s/realms/%s", authServerUrl, realm);
    }



}
