package com.opengroup.jsbapi.application.security.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.opengroup.jsbapi.application.security.token.JwtToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;


@Service
public class TokenService {

    @Autowired
    private Environment env;

    /**
     * Create Token
     * @param jwtToken
     * @return
     */
    public String assignToken(JwtToken jwtToken) {

        return JWT.create()
                .withClaim("login", jwtToken.getLogin())
                .withArrayClaim("scopes", jwtToken.getScopes())
                .withArrayClaim("variables", jwtToken.getVariables())
                .sign(Algorithm.HMAC256(env.getRequiredProperty("security.jwt.secret.path")));


    }

    /**
     * Access Token
     * @param token
     * @return
     */
    public JwtToken accessToken(String token) {
        DecodedJWT decodeToken = JWT.decode(token);
        JwtToken jwtToken = new JwtToken();
        jwtToken.setLogin(decodeToken.getClaim("login").asString());
        jwtToken.setScopes(decodeToken.getClaim("scopes").asArray(String.class));
        jwtToken.setVariables(decodeToken.getClaim("variables").asArray(String.class));
        return jwtToken;
    }
}
