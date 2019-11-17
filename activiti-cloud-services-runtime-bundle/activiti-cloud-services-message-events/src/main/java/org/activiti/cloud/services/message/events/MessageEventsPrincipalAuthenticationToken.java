package org.activiti.cloud.services.message.events;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class MessageEventsPrincipalAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private static final long serialVersionUID = 1L;

    public MessageEventsPrincipalAuthenticationToken(Object principal,
                                           Object credentials,
                                           Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }

    public MessageEventsPrincipalAuthenticationToken() {
        this("messageEvents",
             null,
             Arrays.asList(new SimpleGrantedAuthority("ROLE_ACTIVITI_ADMIN")));
    }

}
