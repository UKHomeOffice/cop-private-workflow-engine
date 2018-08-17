package uk.gov.homeoffice.borders.workflow.process;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.rest.dto.VariableValueDto;
import org.camunda.bpm.engine.rest.dto.runtime.ProcessInstanceDto;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.VariableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.homeoffice.borders.workflow.identity.ShiftUser;

import java.util.Map;

import static uk.gov.homeoffice.borders.workflow.process.ProcessApiPaths.PROCESS_INSTANCE_ROOT_API;

/**
 * REST API for creating a process instance and deleting
 * More endpoints will be exposed over time.
 */

@RestController
@RequestMapping(path = PROCESS_INSTANCE_ROOT_API)
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class ProcessInstanceApiController {

    private ProcessApplicationService processApplicationService;

    @DeleteMapping("/{processInstanceId}")
    public ResponseEntity delete(@PathVariable String processInstanceId, @RequestParam String reason) {
        processApplicationService.delete(processInstanceId, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<ProcessInstanceDto> createInstance(@RequestBody ProcessStartDto processStartDto, ShiftUser shiftUser) {

        ProcessInstance processInstance = processApplicationService.createInstance(processStartDto, shiftUser);
        ProcessInstanceDto processInstanceDto = ProcessInstanceDto.fromProcessInstance(processInstance);

        return ResponseEntity.ok(processInstanceDto);

    }

    @GetMapping(value = "/{processInstanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    private ProcessInstanceDto processInstance(@PathVariable String processInstanceId, ShiftUser shiftUser) {
        ProcessInstance processInstance = processApplicationService.getProcessInstance(processInstanceId, shiftUser);
        return ProcessInstanceDto.fromProcessInstance(processInstance);
    }
    @GetMapping(value = "/{processInstanceId}/variables", produces = MediaType.APPLICATION_JSON_VALUE)
    private Map<String, VariableValueDto> variables(@PathVariable String processInstanceId, ShiftUser shiftUser) {
        VariableMap variables = processApplicationService.variables(processInstanceId, shiftUser);
        return VariableValueDto.fromVariableMap(variables);
    }
}
