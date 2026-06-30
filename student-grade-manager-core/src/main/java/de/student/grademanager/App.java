package de.student.grademanager;

import de.student.grademanager.model.Grade;
import de.student.grademanager.model.Student;
import de.student.grademanager.service.GradeService;
import java.util.List;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Student Grade Manager application.
 *
 * <p>On startup, a demo student is registered and two grades are recorded to
 * verify that the service layer is wired correctly. An optional interactive
 * session can be started via {@link #runInteractive(Scanner)} to capture
 * real student data from the console.
 */
@SpringBootApplication
public final class App implements CommandLineRunner {
  private static final Logger logger = LoggerFactory.getLogger(App.class);
  private static final int DEMO_STUDENT_ID = 1001;
  private final GradeService service;
  private int interactiveStudentId = -1;

  /**
   * Creates the application with the given grade service.
   *
   * @param service the grade service to use; injected by Spring
   */
  public App(GradeService service) {
    this.service = service;
  }

  /**
   * Application entry point; delegates startup to Spring Boot.
   *
   * @param args command-line arguments passed through to Spring Boot
   */
  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }

  /**
   * Runs the demo scenario after the Spring context has started.
   *
   * @param args command-line arguments (not evaluated)
   */
  @Override
  public void run(String... args) {
    logger.info("Student Grade Manager started");
    try {
      service.addStudent("Alice", DEMO_STUDENT_ID);
      service.addGrade(DEMO_STUDENT_ID, "Math", 1.3);
      service.addGrade(DEMO_STUDENT_ID, "Physics", 2.0);
      double avg = service.calculateAverage(DEMO_STUDENT_ID);
      logger.info("Average grade for Alice: {}", avg);
    } catch (IllegalArgumentException e) {
      logger.error("Operation failed: {}", e.getMessage());
    }
  }

  /**
   * Reads a student name, ID, subject, and grade value from the given scanner
   * and registers the data via the grade service.
   *
   * @param scanner the input source to read from
   */
  @SuppressWarnings("PMD.SystemPrintln")
  public void runInteractive(Scanner scanner) {
    try {
      System.out.print("Name: ");
      String name = scanner.nextLine().trim();
      System.out.print("Student-ID: ");
      int studentId = Integer.parseInt(scanner.nextLine().trim());
      System.out.print("Fach: ");
      String subject = scanner.nextLine().trim();
      System.out.print("Note (1.0 - 5.0): ");
      double gradeValue = Double.parseDouble(scanner.nextLine().trim());
      service.addStudent(name, studentId);
      service.addGrade(studentId, subject, gradeValue);
      interactiveStudentId = studentId;
      logger.info("Student {} erfasst. Durchschnitt: {}", name,
          service.calculateAverage(studentId));
    } catch (IllegalArgumentException e) {
      logger.error("Eingabe ungueltig: {}", e.getMessage());
    }
  }

  /**
   * Prints a side-by-side comparison of the demo student and the student
   * entered via {@link #runInteractive(Scanner)}.
   *
   * <p>Does nothing if no interactive student has been registered yet.
   */
  public void printComparison() {
    if (interactiveStudentId == -1) {
      logger.warn("Kein interaktiver Student fuer Vergleich vorhanden");
      return;
    }
    printComparisonTable(DEMO_STUDENT_ID, interactiveStudentId);
  }

  @SuppressWarnings("PMD.SystemPrintln")
  private void printComparisonTable(int id1, int id2) {
    Student s1 = service.getStudent(id1);
    Student s2 = service.getStudent(id2);
    if (s1 == null || s2 == null) {
      logger.warn("Einer der Studenten existiert nicht");
      return;
    }
    String sep = "-".repeat(62);
    System.out.println(sep);
    System.out.printf("%-15s %-8s %-14s %-7s %s%n",
        "Name", "ID", "Fach", "Note", "Schnitt");
    System.out.println(sep);
    printStudentRows(s1, service.getGrades(id1));
    System.out.println(sep);
    printStudentRows(s2, service.getGrades(id2));
    System.out.println(sep);
  }

  @SuppressWarnings("PMD.SystemPrintln")
  private void printStudentRows(Student student, List<Grade> studentGrades) {
    double avg = service.calculateAverage(student.getStudentId());
    if (studentGrades.isEmpty()) {
      System.out.printf("%-15s %-8d %-14s %-7s %.2f%n",
          student.getName(), student.getStudentId(), "-", "-", avg);
      return;
    }
    for (int i = 0; i < studentGrades.size(); i++) {
      Grade grade = studentGrades.get(i);
      if (i == 0) {
        System.out.printf("%-15s %-8d %-14s %-7.1f %.2f%n",
            student.getName(), student.getStudentId(),
            grade.getSubject(), grade.getValue(), avg);
      } else {
        System.out.printf("%-15s %-8s %-14s %-7.1f%n",
            "", "", grade.getSubject(), grade.getValue());
      }
    }
  }
}
