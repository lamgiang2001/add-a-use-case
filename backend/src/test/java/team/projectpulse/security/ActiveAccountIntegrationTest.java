package team.projectpulse.security;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.instructor.InstructorRepository;
import team.projectpulse.student.Student;
import team.projectpulse.student.StudentRepository;
import team.projectpulse.system.StatusCode;
import team.projectpulse.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-SEC-active-account: a deactivated account can neither log in nor keep using a token it was issued before it
 * was deactivated. Each test rolls back, so the accounts it deactivates are active again for the next test.
 */
@DisplayName("Integration tests for FR-SEC-active-account")
class ActiveAccountIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    InstructorRepository instructorRepository;

    @Value("${api.endpoint.base-url}")
    String baseUrl;


    private String login(String email) throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic(email, "123456"))).andReturn();
        JSONObject json = new JSONObject(mvcResult.getResponse().getContentAsString());
        return "Bearer " + json.getJSONObject("data").getString("token");
    }

    private Student jerry() {
        return (Student) this.userRepository.findByUsername("j.moon@abc.edu").orElseThrow();
    }

    @Test
    @DisplayName("An active user with a valid token reaches a protected route")
    void activeStudentWithValidTokenIsAllowed() throws Exception {
        String token = login("j.moon@abc.edu");

        this.mockMvc.perform(get(this.baseUrl + "/students/" + jerry().getId()).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true));
    }

    @Test
    @DisplayName("A deactivated student cannot log in")
    void deactivatedStudentCannotLogIn() throws Exception {
        Student jerry = jerry();
        jerry.setEnabled(false);
        this.studentRepository.saveAndFlush(jerry);

        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.moon@abc.edu", "123456")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("A token issued before the student was deactivated stops working at her next request")
    void deactivatedStudentTokenIssuedBeforeDeactivationIsRefused() throws Exception {
        String token = login("j.moon@abc.edu");
        Student jerry = jerry();
        jerry.setEnabled(false);
        this.studentRepository.saveAndFlush(jerry);

        this.mockMvc.perform(get(this.baseUrl + "/students/" + jerry.getId()).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("User account is abnormal."));
    }

    @Test
    @DisplayName("A token issued before the instructor was deactivated stops working at her next request")
    void deactivatedInstructorTokenIssuedBeforeDeactivationIsRefused() throws Exception {
        // UC-INS-deactivate-instructor has no endpoint yet, so the flag is set directly
        String token = login("b.gates@abc.edu");
        Instructor bill = (Instructor) this.userRepository.findByUsername("b.gates@abc.edu").orElseThrow();
        bill.setEnabled(false);
        this.instructorRepository.saveAndFlush(bill);

        this.mockMvc.perform(get(this.baseUrl + "/students/" + jerry().getId()).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

}
