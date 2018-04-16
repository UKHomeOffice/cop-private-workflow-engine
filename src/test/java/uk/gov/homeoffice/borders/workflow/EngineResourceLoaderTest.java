package uk.gov.homeoffice.borders.workflow;

import com.tngtech.jgiven.annotation.As;
import org.junit.Test;
import uk.gov.homeoffice.borders.workflow.stage.EngineResourceStage;


public class EngineResourceLoaderTest extends JGivenBaseTestClass<EngineResourceStage> {


    @Test
    @As("workflow engine has correct process definitions loaded")
    public void canLoadResource() {
        when().listingAllProcessDefinitions()
                .then()
                .numberOfProcessDefinitionsShouldBe(2)
                .and()
                .hasDefinitions("test");
    }


}
