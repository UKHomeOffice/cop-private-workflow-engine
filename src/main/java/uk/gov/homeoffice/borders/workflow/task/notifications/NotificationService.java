package uk.gov.homeoffice.borders.workflow.task.notifications;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.spin.Spin;
import org.camunda.spin.impl.json.jackson.format.JacksonJsonDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.homeoffice.borders.workflow.PageHelper;
import uk.gov.homeoffice.borders.workflow.exception.InternalWorkflowException;
import uk.gov.homeoffice.borders.workflow.identity.PlatformUser;
import uk.gov.homeoffice.borders.workflow.identity.UserQuery;
import uk.gov.homeoffice.borders.workflow.identity.UserService;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class NotificationService {

    private static final String NOTIFICATIONS = "notifications";

    private TaskService taskService;
    private RuntimeService runtimeService;
    private UserService userService;
    private JacksonJsonDataFormat formatter;
    private static final PageHelper PAGE_HELPER = new PageHelper();

    Page<Task> getNotifications(@NotNull PlatformUser user, Pageable pageable, boolean countOnly) {
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(NOTIFICATIONS)
                .processVariableValueEquals("type", NOTIFICATIONS)
                .taskAssignee(user.getEmail())
                .orderByTaskCreateTime()
                .desc();
        long totalCount = query.count();
        if (countOnly) {
            return new PageImpl<>(new ArrayList<>(),
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                    totalCount);
        }
        int pageNumber = PAGE_HELPER.calculatePageNumber(pageable);

        List<Task> tasks = query
                .listPage(pageNumber, pageable.getPageSize());

        return new PageImpl<>(tasks, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()), totalCount);
    }


    public ProcessInstance create(Notification notification) {
        if (notification.getTeamId() == null && notification.getLocationId() == null) {
            throw new IllegalArgumentException("No team or location defined for notification");
        }
        UserQuery userQuery = new UserQuery();


        if (notification.getTeamId() != null) {
            userQuery.memberOfGroup(notification.getTeamId());
        } else {
            userQuery.location(notification.getLocationId());
        }

        List<PlatformUser> candidateUsers = userService.findByQuery(userQuery);

        List<Notification> notifications = candidateUsers.stream().map(u -> {
            Notification updated = new Notification();
            updated.setPayload(notification.getPayload());
            updated.setSubject(notification.getSubject());
            updated.setPriority(notification.getPriority());
            updated.setEmail(u.getEmail());
            updated.setMobile(u.getShiftDetails().getPhone());
            return updated;

        }).collect(toList());

        if (notifications.isEmpty()) {
            throw new IllegalStateException("Unable to find any people to notify. Please check command/location/team");
        }

        Spin<?> notificationObjectValue =
                Spin.S(notifications, formatter);

        Map<String, Object> variables = new HashMap<>();
        variables.put(NOTIFICATIONS, notificationObjectValue);
        variables.put("type", NOTIFICATIONS);

        return runtimeService.startProcessInstanceByKey(NOTIFICATIONS,
                variables);

    }

    void cancel(String processInstanceId, String reason) {
        if (runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count() != 0) {
            runtimeService.deleteProcessInstance(processInstanceId, reason);
        }
    }

    String acknowledge(@NotNull PlatformUser user, String taskId) {
        taskService.claim(taskId, user.getEmail());
        taskService.complete(taskId);
        log.info("Notification acknowledged.");
        return taskId;
    }
}
