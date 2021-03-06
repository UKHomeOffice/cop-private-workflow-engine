package uk.gov.homeoffice.borders.workflow.process

import org.camunda.bpm.engine.AuthorizationService
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.authorization.Authorization
import org.camunda.bpm.engine.authorization.Permissions
import org.camunda.bpm.engine.authorization.Resources
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import uk.gov.homeoffice.borders.workflow.BaseSpec
import uk.gov.homeoffice.borders.workflow.exception.DuplicateBusinessKeyException
import uk.gov.homeoffice.borders.workflow.identity.PlatformUser
import uk.gov.homeoffice.borders.workflow.identity.Team
import uk.gov.homeoffice.borders.workflow.security.WorkflowAuthentication

import javax.crypto.SealedObject

class ProcessApplicationServiceSpec extends BaseSpec {

    @Autowired
    ProcessApplicationService applicationService

    @Autowired
    HistoryService historyService

    @Autowired
    AuthorizationService authorizationService

    def 'returns process definition if startableInTaskList'() {
        given:
        Authorization newAuthorization = authorizationService
                .createNewAuthorization(Authorization.AUTH_TYPE_GRANT)
        newAuthorization.setGroupId('testcandidate')
        newAuthorization.setResource(Resources.PROCESS_DEFINITION)
        newAuthorization.setResourceId('testRoleAndStartable')
        newAuthorization.addPermission(Permissions.CREATE_INSTANCE)
        authorizationService.saveAuthorization(newAuthorization)

        newAuthorization = authorizationService
                .createNewAuthorization(Authorization.AUTH_TYPE_GRANT)
        newAuthorization.setGroupId('testcandidate')
        newAuthorization.setResource(Resources.PROCESS_DEFINITION)
        newAuthorization.setResourceId('testRoleAndStartable2')
        newAuthorization.addPermission(Permissions.CREATE_INSTANCE)
        authorizationService.saveAuthorization(newAuthorization)

        when:
        def user = new PlatformUser()
        user.id = 'assigneeOneTwoThree'
        user.email = 'assigneeOneTwoThree'

        def shift = new PlatformUser.ShiftDetails()
        shift.roles = ['testcandidate']
        user.shiftDetails = shift
        def team = new Team()
        user.teams = []
        team.code = 'teamA'
        user.teams << team
        user.roles = ['testcandidate']
        identityService.getCurrentAuthentication() >> new WorkflowAuthentication(user)
        def result = applicationService.processDefinitions(user, PageRequest.of(0, 10))

        then:
        result.totalElements == 1
    }

    def 'prevent duplicate process started for business key, user and process key'() {
        given:
        def processStartDto = new ProcessStartDto()
        processStartDto.processKey = 'encryption'
        processStartDto.variableName = 'collectionOfData'
        processStartDto.setBusinessKey('businessKey')
        def data = new Data()
        data.candidateGroup = "teamA"
        data.name = "test 0"
        data.description = "test 0"
        processStartDto.data = [data]
        processStartDto

        and:
        def user = new PlatformUser()
        user.id = 'assigneeOneTwoThree'
        user.email = 'assigneeOneTwoThree'

        def shift = new PlatformUser.ShiftDetails()
        shift.roles = ['custom_role']
        user.shiftDetails = shift

        def team = new Team()
        user.teams = []
        team.code = 'teamA'
        user.teams << team
        user.roles = ['custom_role']
        identityService.getCurrentAuthentication() >> new WorkflowAuthentication(user)
        user

        when:
        applicationService.createInstance(processStartDto, user)
        applicationService.createInstance(processStartDto, user)

        then:
        thrown(DuplicateBusinessKeyException)

    }

    def 'can encrypt variables'() {
        given:
        def processStartDto = new ProcessStartDto()
        processStartDto.processKey = 'encryption'
        processStartDto.variableName = 'collectionOfData'
        def data = new Data()
        data.candidateGroup = "teamA"
        data.name = "test 0"
        data.description = "test 0"
        processStartDto.data = [data]
        processStartDto

        and:
        def user = new PlatformUser()
        user.id = 'assigneeOneTwoThree'
        user.email = 'assigneeOneTwoThree'

        def shift = new PlatformUser.ShiftDetails()
        shift.roles = ['custom_role']
        user.shiftDetails = shift

        def team = new Team()
        user.teams = []
        team.code = 'teamA'
        user.teams << team
        user.roles = ['custom_role']
        identityService.getCurrentAuthentication() >> new WorkflowAuthentication(user)
        user

        when:
        def response = applicationService.createInstance(processStartDto, user)
        def processInstanceId = response._1().id
        def result = runtimeService.createVariableInstanceQuery()
                .processInstanceIdIn(processInstanceId)
                .variableName('collectionOfData').singleResult()


        then:
        result.value instanceof SealedObject


        when:
        team = new Team()
        team.code = WorkflowAuthentication.SERVICE_ROLE
        user.teams = [team]
        def variables = applicationService.variables(processInstanceId, user)

        then:
        !(variables.get('collectionOfData') instanceof SealedObject)
        def history = historyService.createHistoricVariableInstanceQuery().list()
        history.size() != 0

    }

    def 'can get tasks after creating a process instance'() {
        given:
        def processStartDto = createProcessStartDto()
        and:
        def user = new PlatformUser()
        user.id = 'assigneeOneTwoThree'
        user.email = 'assigneeOneTwoThree'

        def shift = new PlatformUser.ShiftDetails()
        shift.roles = ['custom_role']
        user.shiftDetails = shift

        def team = new Team()
        user.teams = []
        team.code = 'teamA'
        user.teams << team
        user.roles = ['custom_role']
        identityService.getCurrentAuthentication() >> new WorkflowAuthentication(user)

        when:
        def result = applicationService.createInstance(processStartDto, user)

        then:
        result._2().size() == 1
    }

    def 'can get task of sub process'() {
        given:
        def processStartDto = new ProcessStartDto()
        processStartDto.processKey = 'testSubProcess'
        processStartDto.variableName ='testVariable'
        def data = new Data()
        data.candidateGroup = "teamA"
        data.name = "test 0"
        data.description = "test 0"
        data.assignee = "user"
        processStartDto.data = [data]

        and:
        def user = new PlatformUser()
        user.id = 'user'
        user.email = 'user'

        def shift = new PlatformUser.ShiftDetails()
        shift.roles = ['custom_role']
        user.shiftDetails = shift

        def team = new Team()
        user.teams = []
        team.code = 'teamA'
        user.teams << team
        user.roles = ['custom_role']
        identityService.getCurrentAuthentication() >> new WorkflowAuthentication(user)

        when:
        def result = applicationService.createInstance(processStartDto, user)

        then:
        result._2().size() == 1
    }

    ProcessStartDto createProcessStartDto() {
        def processStartDto = new ProcessStartDto()
        processStartDto.processKey = 'test'
        processStartDto.variableName = 'collectionOfData'
        def data = new Data()
        data.candidateGroup = "teamA"
        data.name = "test 0"
        data.description = "test 0"
        data.assignee = "assigneeOneTwoThree"
        processStartDto.data = [data]
        processStartDto
    }

}
