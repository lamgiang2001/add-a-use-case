package team.projectpulse.system.exception;

/**
 * Thrown when a student who is already deactivated is deactivated again (UC-STU-deactivate-student, extension 3a).
 * Mapped to 409 so a stale page knows to reload rather than treating the request as done.
 */
public class StudentAlreadyDeactivatedException extends RuntimeException {

    public StudentAlreadyDeactivatedException() {
        super("The student is already deactivated.");
    }

}
