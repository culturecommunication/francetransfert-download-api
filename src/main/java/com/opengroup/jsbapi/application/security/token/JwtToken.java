package com.opengroup.jsbapi.application.security.token;

import lombok.Data;

/**
 * Jwt Token
 */
@Data
public class JwtToken {

    /**
     * Login of the user
     */
    private String login;
    /**
     * Scopes or roles of the user
     * example : Admin, Reader....
     */
    private String[] scopes;


    /**
     * Variables used as a complements to store other values needed
     */
    private String[] variables;
 }
