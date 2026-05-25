package de.student.grademanager;

import de.student.grademanager.service.GradeServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
  private static final Logger logger = LoggerFactory.getLogger(App.class);

  public static void main(String[] args) {
    logger.info("Student Grade Manager started");
    GradeServiceImpl service = new GradeServiceImpl();
    service.addStudent("Alice", 1001);
    service.addGrade(1001, "Math", 1.3);
    service.addGrade(1001, "Physics", 2.0);
    double avg = service.calculateAverage(1001);
    logger.info("Average grade for Alice: {}", avg);
  }
}
