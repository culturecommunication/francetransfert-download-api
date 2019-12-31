package fr.gouv.culture.francetransfert;

import fr.gouv.culture.francetransfert.application.resources.model.DownloadRepresentation;
import fr.gouv.culture.francetransfert.application.services.DownloadServices;
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
public class DownloadServiceTest {

    @Autowired
    private DownloadServices downloadServices;


    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void shouldSendMailToRecipient() throws Exception {
        //given
        String enclosureId = "8ffd72f0-4432-4e07-b247-362b1eb4edfb";
        String mailRecipient = "louay@live.fr";
        String password = "";
        //when
        DownloadRepresentation downloadRepresentation = downloadServices.processDownload(mailRecipient, enclosureId, password);
        //then
        Assert.assertTrue(downloadRepresentation != null);
    }



    @After
    public void tearDown() throws Exception {

    }

}