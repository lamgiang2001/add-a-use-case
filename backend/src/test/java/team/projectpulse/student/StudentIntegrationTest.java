package team.projectpulse.student;

import tools.jackson.databind.json.JsonMapper;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.activity.ActivityRepository;
import team.projectpulse.evaluation.PeerEvaluationRepository;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.StatusCode;
import team.projectpulse.user.UserRepository;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Integration tests for Student API endpoints")
class StudentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    PeerEvaluationRepository peerEvaluationRepository;

    @Autowired
    ActivityRepository activityRepository;

    String adminToken;

    String instructorBillToken;

    String studentJohnToken;

    String studentEricToken;

    String adminTimToken;

    @Value("${api.endpoint.base-url}")
    String baseUrl;


    @BeforeEach
    void setUp() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("b.wei@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        MvcResult mvcResult = resultActions.andDo(print()).andReturn();
        String contentAsString = mvcResult.getResponse().getContentAsString();
        JSONObject json = new JSONObject(contentAsString);
        this.adminToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("b.gates@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.instructorBillToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.studentJohnToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("e.hudson@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.studentEricToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("t.cook@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.adminTimToken = "Bearer " + json.getJSONObject("data").getString("token");
    }

    @Test
    void testFindStudentsByCriteria() throws Exception {
        // Given
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("teamName", "Team1");
        String json = this.jsonMapper.writeValueAsString(searchCriteria);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("page", "0");
        requestParams.add("size", "30");
        requestParams.add("sort", "lastName,asc");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students/search").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find students successfully"))
                .andExpect(jsonPath("$.data.content", Matchers.hasSize(3)));
    }

    @Test
    void testInstructorBillFindStudentInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"));
    }

    @Test
    void testAdminTimFindStudentInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminTimToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    @Test
    void testStudentJohnFindOwnInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"));
    }

    @Test
    void testStudentJohnFindAnotherStudentsInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/5").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    @Test
    void testAddStudent() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "lucas");
        studentDto.put("firstName", "Lucas");
        studentDto.put("lastName", "Santos");
        studentDto.put("email", "l.santos@abc.edu");
        studentDto.put("password", "Abc123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "registrationToken");
        requestParams.add("role", "student");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Add student successfully"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.username").value("lucas"))
                .andExpect(jsonPath("$.data.firstName").value("Lucas"))
                .andExpect(jsonPath("$.data.lastName").value("Santos"))
                .andExpect(jsonPath("$.data.email").value("l.santos@abc.edu"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void testAddStudentWithInvalidUserInvitation() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "marcos");
        studentDto.put("firstName", "Marcos");
        studentDto.put("lastName", "Grant");
        studentDto.put("email", "m.grant@abc.edu");
        studentDto.put("password", "123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "invalidRegistrationToken");
        requestParams.add("role", "student");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("m.grant@abc.edu is not invited to register. Please contact the course admin."));
    }

    @Test
    void testAddStudentWithNoWrongUserInvitation() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "lucas");
        studentDto.put("firstName", "Lucas");
        studentDto.put("lastName", "Santos");
        studentDto.put("email", "l.santos@abc.edu");
        studentDto.put("password", "123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "registrationToken");
        requestParams.add("role", "instructor"); // This is wrong, Lucas should be a student.

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("You are not allowed to register as instructor for email: l.santos@abc.edu"));
    }

    @Test
    void testStudentJohnUpdatesOwnInfo() throws Exception {
        Map<String, Object> studentDto = new HashMap<>();
        studentDto.put("username", "john");
        studentDto.put("firstName", "John (updated)");
        studentDto.put("lastName", "Smith");
        studentDto.put("email", "j.smith@abc.edu");
        studentDto.put("enabled", false); // Student cannot change their enabled status.
        studentDto.put("roles", "instructor"); // Student cannot change their role.

        String json = this.jsonMapper.writeValueAsString(studentDto);

        // When and then
        this.mockMvc.perform(put(this.baseUrl + "/students/4").contentType(MediaType.APPLICATION_JSON).content(json).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Update student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("john"))
                .andExpect(jsonPath("$.data.firstName").value("John (updated)"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.enabled").value(true)) // The enabled status should not be changed.
                .andExpect(jsonPath("$.data.roles").value("student")); // The role should not be changed.
    }

    @Test
    void testStudentJohnUpdatesAnotherStudentsInfo() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "eric");
        studentDto.put("firstName", "Eric (updated)");
        studentDto.put("lastName", "Hudson");
        studentDto.put("email", "e.hudson@abc.edu");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        // When and then
        this.mockMvc.perform(put(this.baseUrl + "/students/5").contentType(MediaType.APPLICATION_JSON).content(json).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

//    @Test
//    void testAdminDeletesStudent() throws Exception {
//        this.mockMvc.perform(delete(this.baseUrl + "/students/3").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
//                .andExpect(jsonPath("$.flag").value(true))
//                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
//                .andExpect(jsonPath("$.message").value("Delete instructor successfully"));
//    }
//
//    @Test
//    void testStudentDeletesAnotherStudent() throws Exception {
//        this.mockMvc.perform(delete(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentToken))
//                .andExpect(jsonPath("$.flag").value(false))
//                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
//                .andExpect(jsonPath("$.message").value("No permission."))
//                .andExpect(jsonPath("$.data").value("Access Denied"));
//    }

    @Test
    void testStudentJohnFindStudentsInOwnTeam() throws Exception {
        // When and then
        this.mockMvc.perform(get(this.baseUrl + "/students/teams/1").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find students by team id successfully"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(3)));
    }

    @Test
    void testStudentJohnFindStudentsInAnotherTeam() throws Exception {
        // When and then
        this.mockMvc.perform(get(this.baseUrl + "/students/teams/2").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    // UC-STU-deactivate-student. Every test rolls back (AbstractIntegrationTest is @Transactional), so the
    // deactivations here never leak into another test's tokens.

    private Student jerry() {
        return (Student) this.userRepository.findByUsername("j.moon@abc.edu").orElseThrow();
    }

    private long jerrysActivityCount(Integer jerryId) {
        return this.activityRepository.findAll().stream().filter(activity -> activity.getStudent().getId().equals(jerryId)).count();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student main: an instructor of her course section deactivates her; team and history are kept")
    void testInstructorBillDeactivatesStudentJerry() throws Exception {
        Student jerry = jerry();
        Integer teamIdBefore = jerry.getTeam().getTeamId();
        int evaluationsOfHerBefore = this.peerEvaluationRepository.findByWeekAndEvaluateeId("2023-W31", jerry.getId()).size();
        long activitiesBefore = jerrysActivityCount(jerry.getId());
        assertThat(evaluationsOfHerBefore).isPositive();
        assertThat(activitiesBefore).isPositive();

        this.mockMvc.perform(put(this.baseUrl + "/students/" + jerry.getId() + "/deactivate").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Deactivate student successfully"))
                .andExpect(jsonPath("$.data.id").value(jerry.getId()))
                .andExpect(jsonPath("$.data.firstName").value("Jerry"))
                .andExpect(jsonPath("$.data.lastName").value("Moon"))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.teamId").value(teamIdBefore));

        Student after = this.studentRepository.findById(jerry.getId()).orElseThrow();
        assertThat(after.isEnabled()).isFalse();
        assertThat(after.getTeam().getTeamId()).isEqualTo(teamIdBefore);
        assertThat(this.peerEvaluationRepository.findByWeekAndEvaluateeId("2023-W31", jerry.getId())).hasSize(evaluationsOfHerBefore);
        assertThat(jerrysActivityCount(jerry.getId())).isEqualTo(activitiesBefore);
    }

    @Test
    @DisplayName("UC-STU-deactivate-student main: the course admin of her course, not assigned to her course section, deactivates her")
    void testCourseAdminNotAssignedToTheSectionDeactivatesStudentJerry() throws Exception {
        Student jerry = jerry();
        Section section = jerry.getSection();
        Instructor bingyang = (Instructor) this.userRepository.findByUsername("b.wei@abc.edu").orElseThrow();
        section.removeInstructor(bingyang);
        this.studentRepository.flush();
        assertThat(section.getInstructors()).doesNotContain(bingyang);
        assertThat(section.getCourse().getCourseAdmin().getId()).isEqualTo(bingyang.getId());

        this.mockMvc.perform(put(this.baseUrl + "/students/" + jerry.getId() + "/deactivate").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @DisplayName("UC-STU-deactivate-student 3a: an already deactivated student answers 409 and nothing changes")
    void testDeactivateAlreadyDeactivatedStudent() throws Exception {
        Student jerry = jerry();
        jerry.setEnabled(false);
        this.studentRepository.saveAndFlush(jerry);

        this.mockMvc.perform(put(this.baseUrl + "/students/" + jerry.getId() + "/deactivate").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.CONFLICT))
                .andExpect(jsonPath("$.message").value("The student is already deactivated."));

        assertThat(this.studentRepository.findById(jerry.getId()).orElseThrow().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student 3b: a student cannot deactivate herself")
    void testStudentJohnCannotDeactivateHerself() throws Exception {
        this.mockMvc.perform(put(this.baseUrl + "/students/4/deactivate").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."));

        assertThat(this.studentRepository.findById(4).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student 3b: a student cannot deactivate another student")
    void testStudentJohnCannotDeactivateAnotherStudent() throws Exception {
        Student jerry = jerry();

        this.mockMvc.perform(put(this.baseUrl + "/students/" + jerry.getId() + "/deactivate").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));

        assertThat(this.studentRepository.findById(jerry.getId()).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student 3b: an instructor of another course section, course admin of another course, is denied")
    void testAdminTimOfAnotherCourseCannotDeactivateStudent() throws Exception {
        Student jerry = jerry();

        this.mockMvc.perform(put(this.baseUrl + "/students/" + jerry.getId() + "/deactivate").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminTimToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));

        assertThat(this.studentRepository.findById(jerry.getId()).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student 6a: a student id that names nobody is denied without revealing that")
    void testDeactivateStudentThatDoesNotExist() throws Exception {
        long enabledBefore = this.studentRepository.findAll().stream().filter(Student::isEnabled).count();

        this.mockMvc.perform(put(this.baseUrl + "/students/999999/deactivate").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."));

        assertThat(this.studentRepository.findAll().stream().filter(Student::isEnabled).count()).isEqualTo(enabledBefore);
    }

}
