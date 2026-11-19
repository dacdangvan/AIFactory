package com.vib.cms.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import java.util.Arrays;

@Configuration
public class SecurityConfig {
 @Bean @Profile("local") FilterRegistrationBean<LocalDevHeaderFilter> disableLocalFilterServletRegistration(LocalDevHeaderFilter filter) {
  FilterRegistrationBean<LocalDevHeaderFilter> registration = new FilterRegistrationBean<>(filter);
  registration.setEnabled(false);
  return registration;
 }
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, Environment env,
        ObjectProvider<LocalDevHeaderFilter> localFilter,
        @Value("${cms.security.local-permit-all:false}") boolean localPermitAll) throws Exception {
  boolean local=Arrays.asList(env.getActiveProfiles()).contains("local");
  http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
  http.authorizeHttpRequests(a->{a.requestMatchers("/actuator/health","/actuator/info").permitAll();if(local&&localPermitAll)a.anyRequest().permitAll();else a.anyRequest().authenticated();});
  if(local&&localPermitAll) {
   LocalDevHeaderFilter filter=localFilter.getIfAvailable();
   if(filter!=null) http.addFilterBefore(filter, AnonymousAuthenticationFilter.class);
  } else {
   http.oauth2ResourceServer(o->o.jwt(Customizer.withDefaults()));
  }
  return http.build();
 }
}