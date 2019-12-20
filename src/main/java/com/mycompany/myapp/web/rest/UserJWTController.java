package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.config.Constants;
import com.mycompany.myapp.security.jwt.JWTFilter;
import com.mycompany.myapp.security.jwt.TokenProvider;
import com.mycompany.myapp.service.AuditEventService;
import com.mycompany.myapp.web.rest.vm.LoginVM;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
public class UserJWTController {

    private final TokenProvider tokenProvider;

    private final ReactiveAuthenticationManager authenticationManager;

    private final AuditEventService auditEventService;

    public UserJWTController(TokenProvider tokenProvider, ReactiveAuthenticationManager authenticationManager, AuditEventService auditEventService) {
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.auditEventService = auditEventService;
    }

    @PostMapping("/authenticate")
    public Mono<ResponseEntity<JWTToken>> authorize(@Valid @RequestBody Mono<LoginVM> loginVM) {
        return loginVM
            .flatMap(login -> authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword()))
                .onErrorResume(throwable -> onAuthenticationError(login, throwable))
                .flatMap(auth -> onAuthenticationSuccess(login, auth))
                .map(auth -> tokenProvider.createToken(auth, Boolean.TRUE.equals(login.isRememberMe())))
            )
            .map(jwt -> {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
                return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
            });
    }

    private Mono<? extends Authentication> onAuthenticationSuccess(LoginVM login, Authentication auth) {
        return Mono.just(login)
            .map(LoginVM::getUsername)
            .filter(username -> !Constants.ANONYMOUS_USER.equals(username))
            .flatMap(auditEventService::saveAuthenticationSuccess)
            .thenReturn(auth);
    }

    private Mono<? extends Authentication> onAuthenticationError(LoginVM login, Throwable throwable) {
        return Mono.just(login)
                .map(LoginVM::getUsername)
                .filter(username -> !Constants.ANONYMOUS_USER.equals(username))
                .flatMap(username -> auditEventService.saveAuthenticationError(username, throwable))
                .then(Mono.error(throwable));
    }


    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}
