package uk.gov.homeoffice.borders.workflow.process;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.camunda.bpm.engine.rest.dto.runtime.ProcessInstanceWithVariablesDto;
import org.camunda.bpm.engine.rest.dto.task.TaskDto;

import java.util.List;

@Data
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
class ProcessInstanceResponse {

    private ProcessInstanceWithVariablesDto processInstance;
    private List<TaskDto> tasks;
}
