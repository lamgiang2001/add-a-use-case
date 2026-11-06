package team.projectpulse.security.authorizationmanagers;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import team.projectpulse.student.StudentSecurityService;

import java.util.function.Supplier;

/**
 * Guards changing a student's account status (BR-student-lifecycle), admitting an instructor of that student's
 * course section or the course admin of the course that owns it. The student herself is not admitted, which is why
 * {@code UserOwnershipAuthorizationManager} is not reused.
 *
 * <p>Reads {@code studentId}. Both checks fail closed, so an id that names no student is refused here (403) and
 * the caller cannot tell which ids exist (UC-STU-deactivate-student, extension 6a).
 */
@Component
public class StudentLifecycleAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final StudentSecurityService studentSecurityService;


    public StudentLifecycleAuthorizationManager(StudentSecurityService studentSecurityService) {
        this.studentSecurityService = studentSecurityService;
    }

    @Override
    public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext context) {
        Integer studentId = PathVariables.readId(context, "studentId");
        if (studentId == null) {
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(
                this.studentSecurityService.isCurrentUserInstructorOfStudentSection(studentId)
                        || this.studentSecurityService.isCurrentUserAdminOfStudentCourse(studentId));
    }

}
