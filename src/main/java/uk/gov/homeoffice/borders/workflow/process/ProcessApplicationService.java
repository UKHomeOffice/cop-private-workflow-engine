package uk.gov.homeoffice.borders.workflow.process;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ResourceDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.gov.homeoffice.borders.workflow.ResourceNotFound;
import uk.gov.homeoffice.borders.workflow.identity.User;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class ProcessApplicationService {

    private RepositoryService repositoryService;
    private RuntimeService runtimeService;
    private FormService formService;

    /**
     * Returns the process definitions based on user qualifications and grades.
     *
     * @param user
     * @param pageable
     * @return paged result
     */
    public Page<ProcessDefinition> processDefinitions(User user, Pageable pageable) {
        log.debug("Loading process definitions for '{}'", user.getEmail());
        List<ProcessDefinition> processDefinitions = repositoryService
                .createProcessDefinitionQuery()
                .latestVersion()
                .list();
        List<ProcessDefinition> definitions = processDefinitions.stream()
                .filter(p -> !p.getKey().equalsIgnoreCase("activate-shift")
                        && !p.getKey().equalsIgnoreCase("notifications"))
                .filter(ProcessDefinition::hasStartFormKey)
                .sorted(Comparator.comparing(ResourceDefinition::getName))
                .collect(Collectors.toList());

        return new PageImpl<>(definitions, new PageRequest(pageable.getPageNumber(), pageable.getPageSize()), definitions.size());


    }

    /**
     * Get form key for given process definition key
     * @param processDefinitionId
     * @return
     */
    public String formKey(String processDefinitionId) {
        String startFormKey = formService.getStartFormKey(processDefinitionId);
        if (startFormKey == null) {
            throw new ResourceNotFound(String.format("Process definition %s does not have a start form", processDefinitionId));
        }
        return startFormKey;
    }

    public void delete(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
        log.info("Process instance '{}' deleted", processInstanceId);
    }


    public ProcessInstance createInstance(ProcessStartDto processStartDto, User user) {
        ProcessDefinition processDefinition = getDefinition(processStartDto.getProcessKey());
        ObjectValue dataObject =
                Variables.objectValue(processStartDto.getData())
                        .serializationDataFormat(MediaType.APPLICATION_JSON_VALUE)
                        .create();

        Map<String, Object> variables = new HashMap<>();
        variables.put(processStartDto.getVariableName(), dataObject);
        variables.put("type", "non-notifications");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinition.getKey(),
                variables);
        log.info("'{}' was successfully started with id '{}' by '{}'", processStartDto.getProcessKey(),
                processInstance.getProcessInstanceId(), user.getEmail());

        return processInstance;

    }

    public ProcessInstance getProcessInstance(String processInstanceId, User user) {
        log.info("User '{}' requested process instance '{}'", user.getEmail(), processInstanceId);
        return runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
    }

    public VariableMap variables(String processInstanceId, User user) {
        log.info("User '{}' requested process instance variables for '{}'", user.getEmail(), processInstanceId);
        return runtimeService.getVariablesTyped(processInstanceId, false);
    }

    public ProcessDefinition getDefinition(String processKey) {
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .latestVersion()
                .processDefinitionKey(processKey).singleResult();
        if (processDefinition == null) {
            throw new ResourceNotFound("Definition does nto exist");
        }
        return processDefinition;
    }
}
