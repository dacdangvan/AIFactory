package com.vib.cms.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import java.util.Arrays;
@Configuration
public class SecurityConfig {
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,Environment env,@Value("${cms.security.local-permit-all:false}") boolean localPermitAll)throws Exception{
  boolean local=Arrays.asList(env.getActiveProfiles()).contains("local");
  http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
  http.authorizeHttpRequests(a->{a.requestMatchers("/actuator/health","/actuator/info").permitAll();if(local&&localPermitAll)a.anyRequest().permitAll();else a.anyRequest().authenticated();});
  if(!(local&&localPermitAll))http.oauth2ResourceServer(o->o.jwt(Customizer.withDefaults()));
  return http.build();
 }
}
