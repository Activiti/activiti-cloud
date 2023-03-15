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
package org.activiti.cloud.services.identity.basic;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
public class BasicAuthenticationProviderTest {

    @InjectMocks
    private BasicAuthenticationProvider basicAuthenticationProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Test
    public void testAuthenticate() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("testrole"));
        User user = new User("test", "pass", authorities);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "test",
            "pass",
            authorities
        );

        when(userDetailsService.loadUserByUsername("test")).thenReturn(user);

        assertThat(basicAuthenticationProvider.authenticate(authentication)).isNotNull();
    }

    @Test
    public void testAuthenticationFailure() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("testrole"));
        User user = new User("test", "pass", authorities);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "differentuser",
            "differentpass",
            authorities
        );

        when(userDetailsService.loadUserByUsername("differentuser")).thenReturn(user);

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> basicAuthenticationProvider.authenticate(authentication));
    }

    @Test
    public void testSupports() {
        assertThat(basicAuthenticationProvider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
        assertThat(basicAuthenticationProvider.supports(Integer.class)).isFalse();
    }
}
