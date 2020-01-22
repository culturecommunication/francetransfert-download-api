package fr.gouv.culture.francetransfert.application.security.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.gouv.culture.francetransfert.application.security.token.JwtRequest;
import fr.gouv.culture.francetransfert.application.security.token.JwtToken;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.util.Date;


@Service
public class TokenService {

    @Autowired
    private Environment env;

    @Value("${security.jwt.secret.path}")
    private String keyPath;

    @Value("${security.jwt.secret.alias}")
    private String alias;

    @Value("${security.jwt.secret.storepass}")
    private String storePass;

    @Value("${security.jwt.secret.keypass}")
    private String keyPass;

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

    /**
     * validate Token: Download FranceTransfert
     * @param token
     * @return
     */
    public JwtRequest validateTokenDownload(String token) {
        Claims claims = getAllClaimsFromToken(token);
        if (isTokenExpired(claims.getExpiration())) {
            throw new DownloadException("Token expiré");
        }
        JwtRequest jwtToken = new JwtRequest();
        jwtToken.setMailRecipient(claims.get("mailRecipient", String.class));
        jwtToken.setEnclosureId(claims.get("enclosureId", String.class));
        jwtToken.setWithPassword(claims.get("withPassword",Boolean.class));
        return jwtToken;
    }

    //for retrieveing any information from token we will need the secret key
    private Claims getAllClaimsFromToken(String token) {
        try {
			return Jwts.parser().setSigningKey(getKey()).parseClaimsJws(token).getBody();
		} catch (SignatureException | ExpiredJwtException | UnsupportedJwtException | MalformedJwtException
				| IllegalArgumentException | IOException e) {
			throw new DownloadException("Erreur Token");
		}
    }

    /**
     * Create Key
     * @param
     * @return
     * @throws IOException 
     */
    public Key getKey() throws IOException {
    	FileInputStream in = null;
    	try {
            in = new FileInputStream(keyPath);
            KeyStore ks = KeyStore.getInstance("jceks");
            ks.load(in, (storePass).toCharArray());
            return ks.getKey(alias, keyPass.toCharArray());
        } catch (Exception var5) {
            throw new DownloadException("access denied");
        }finally {
			in.close();
		}
    }

    //check if the token has expired
    private Boolean isTokenExpired(Date expiration) {
        return expiration.before(new Date());
    }
}
