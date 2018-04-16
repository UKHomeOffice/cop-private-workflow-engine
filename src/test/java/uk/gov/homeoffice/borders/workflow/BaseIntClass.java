package uk.gov.homeoffice.borders.workflow;


import com.tngtech.jgiven.integration.spring.EnableJGiven;
import com.tngtech.jgiven.integration.spring.SimpleSpringScenarioTest;
import org.camunda.bpm.engine.IdentityService;
import org.junit.runner.RunWith;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.homeoffice.borders.workflow.identity.UserService;
import uk.gov.service.notify.NotificationClient;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"security.basic.enabled=false",
                "keycloak.enabled=false"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(properties = {"GOV_NOTIFY_CLIENT_ID = XXXX", "GOV_NOTIFY_NOTIFICATION_EMAIL_TEMPLATE_ID = XXXX", "" +
        "GOV_NOTIFY_NOTIFICATION_SMS_TEMPLATE_ID = XXXX"})
public abstract class BaseIntClass {

    @Autowired
    protected TestRestTemplate testRestTemplate;

    @MockBean
    protected AccessToken accessToken;

    @MockBean
    protected KeycloakSecurityContext keycloakSecurityContext;


    @MockBean
    protected UserService userService;

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected IdentityService identityService;

    @MockBean
    protected NotificationClient notificationClient;

}