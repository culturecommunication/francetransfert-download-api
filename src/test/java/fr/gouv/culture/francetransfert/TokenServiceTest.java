package fr.gouv.culture.francetransfert;

import fr.gouv.culture.francetransfert.application.security.services.TokenService;
import fr.gouv.culture.francetransfert.application.security.token.JwtRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FranceTransfertDownloadStarter.class)
public class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

    private String token = null;
    private JwtRequest initialJwtRequest = null;

    @Before
    public void setUp() throws Exception {
        String enclosureId = "8ffd72f0-4432-4e07-b247-362b1eb4edfb";
        String mailRecipient = "louay@live.fr";
        boolean withPassword = false;
        initialJwtRequest = new JwtRequest(enclosureId, mailRecipient, withPassword);
        token = "eyJhbGciOiJIUzUxMiJ9.eyJtYWlsUmVjaXBpZW50IjoibG91YXloYWRkZWQyMDEyQGdtYWlsLmNvbSIsImVuY2xvc3VyZUlkIjoiZW5jbG9zdXJlSWQiLCJ3aXRoUGFzc3dvcmQiOmZhbHNlLCJpYXQiOjE1NzgwNjcwOTYsImV4cCI6MTczNTcyMzI1Nn0.pjjjVHsVM6T_pmDZGhRZEBccHMTohVQy3ynfBzoKf9FWYyNMRQ0DIvb8KmRiKD6dILB7CuZdzTEUZK4Um1U6Sg";
    }

    @Test
    public void validateTokenFranceTransfert() throws Exception {
        //given
        String enclosureId = "enclosureId";
        String mailRecipient = "louayhadded2012@gmail.com";
        boolean withPassword = false;
        //when
        JwtRequest jwtRequest = tokenService.validateTokenDownload(token);
        //then
        Assert.assertNotNull(jwtRequest);
        Assert.assertEquals(jwtRequest.getEnclosureId(), enclosureId);
        Assert.assertEquals(jwtRequest.getMailRecipient(), mailRecipient);
        Assert.assertEquals(jwtRequest.isWithPassword(), withPassword);
    }




    @After
    public void tearDown() throws Exception {
        token = null;
        initialJwtRequest = null;
    }
}
