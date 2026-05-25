package de.student.grademanager.service;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GradeServiceTest {

  @Test
  public void addStudentThenHasStudentReturnsTrue() {
    GradeServiceImpl service = new GradeServiceImpl();
    service.addStudent("Bob", 2001);
    assertTrue(service.hasStudent(2001));
  }

  @Test
  public void calculateAverageWithNoGradesReturnsZero() {
    GradeServiceImpl service = new GradeServiceImpl();
    service.addStudent("Carol", 2002);
    assertEquals(0.0, service.calculateAverage(2002), 0.001);
  }

  @Test
  public void calculateAverageWithTwoGradesReturnsCorrectAverage() {
    GradeServiceImpl service = new GradeServiceImpl();
    service.addStudent("Dave", 2003);
    service.addGrade(2003, "Math", 2.0);
    service.addGrade(2003, "Physics", 3.0);
    assertEquals(2.5, service.calculateAverage(2003), 0.001);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addGradeForUnknownStudentThrowsException() {
    GradeServiceImpl service = new GradeServiceImpl();
    service.addGrade(9999, "Math", 1.5);
  }

  @Test
  public void multipleSubjectsAverageCorrectly() {
    GradeServiceImpl service = new GradeServiceImpl();
    service.addStudent("Eve", 2004);
    service.addGrade(2004, "Math", 1.0);
    service.addGrade(2004, "Physics", 2.0);
    service.addGrade(2004, "English", 3.0);
    assertEquals(2.0, service.calculateAverage(2004), 0.001);
  }
}
