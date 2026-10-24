package pl.dudios.planner.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import pl.dudios.planner.member.service.MemberService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityFilterChainConfig {

  private static final String LOGIN_PATH = "/api/auth/login";
  private static final String REGISTER_PATH = "/api/auth/register";

  private final AuthenticationEntryPoint authenticationEntryPoint;
  private final MemberService memberService;

  @Bean
  public SecurityContextRepository securityContextRepository() {
    return new DelegatingSecurityContextRepository(
      new RequestAttributeSecurityContextRepository(),
      new HttpSessionSecurityContextRepository()
    );
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) {
    http
      .csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
        .ignoringRequestMatchers(LOGIN_PATH, REGISTER_PATH)
      )
      .cors(Customizer.withDefaults())
      .authorizeHttpRequests(request -> request
        .requestMatchers(HttpMethod.POST, LOGIN_PATH, REGISTER_PATH).permitAll()
        .requestMatchers(HttpMethod.GET, "/api/auth/me").permitAll()
        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
        .requestMatchers("/actuator/**").denyAll()
        .requestMatchers("/api/**").authenticated()
        .anyRequest().permitAll()
      )
      .sessionManagement(sessionManagement -> sessionManagement
        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
      )
      .securityContext(securityContext -> securityContext
        .securityContextRepository(securityContextRepository)
      )
      .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
      .addFilterAfter(new ActiveMemberFilter(memberService), BasicAuthenticationFilter.class)
      .logout(logout -> logout
        .logoutUrl("/api/auth/logout")
        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value()))
        .invalidateHttpSession(true)
        .clearAuthentication(true)
        .deleteCookies("JSESSIONID")
      )
      .headers(headers -> headers
        .httpStrictTransportSecurity(hsts -> hsts
          .includeSubDomains(true)
          .maxAgeInSeconds(31_536_000))
        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
        .contentTypeOptions(Customizer.withDefaults())
        .referrerPolicy(referrer -> referrer.policy(
          ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
      )
      .exceptionHandling(exceptionHandling -> exceptionHandling
        .authenticationEntryPoint(authenticationEntryPoint)
      );

    return http.build();
  }

}
