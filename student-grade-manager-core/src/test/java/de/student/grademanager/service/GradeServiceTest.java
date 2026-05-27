package de.student.grademanager.service;

import de.student.grademanager.model.Student;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GradeServiceTest {

  private GradeServiceImpl service;

  @BeforeClass
  public static void assertionsEnabledForAllTests() {
    assertTrue(
        "Tests muessen mit -ea laufen damit Assertions aktiv sind",
        GradeServiceImpl.class.desiredAssertionStatus()
    );
  }

  @Before
  public void setUp() {
    service = new GradeServiceImpl();
  }

  @Test
  public void addStudentThenHasStudentReturnsTrue() {
    service.addStudent("Bob", 2001);
    assertTrue(service.hasStudent(2001));
  }

  @Test
  public void calculateAverageWithNoGradesReturnsZero() {
    service.addStudent("Carol", 2002);
    assertEquals(0.0, service.calculateAverage(2002), 0.001);
  }

  @Test
  public void calculateAverageWithTwoGradesReturnsCorrectAverage() {
    service.addStudent("Dave", 2003);
    service.addGrade(2003, "Math", 2.0);
    service.addGrade(2003, "Physics", 3.0);
    assertEquals(2.5, service.calculateAverage(2003), 0.001);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addGradeForUnknownStudentThrowsException() {
    service.addGrade(9999, "Math", 1.5);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void addGradeThrowsAssertionErrorWhenMapsOutOfSync() throws Exception {
    Field studentsField = GradeServiceImpl.class.getDeclaredField("students");
    studentsField.setAccessible(true);
    Map<Integer, Student> students = (Map<Integer, Student>) studentsField.get(service);
    students.put(9001, new Student("Ghost", 9001));

    try {
      service.addGrade(9001, "Math", 2.0);
      fail("AssertionError erwartet aber nicht geworfen");
    } catch (AssertionError e) {
      assertNotNull("AssertionError muss eine Message haben", e.getMessage());
      assertEquals(
          "grades map out of sync with students map for id 9001",
          e.getMessage()
      );
    }
  }

  @Test
  public void getStudentReturnsCorrectStudent() {
    service.addStudent("Alice", 3001);
    assertEquals("Alice", service.getStudent(3001).getName());
    assertEquals(3001, service.getStudent(3001).getStudentId());
  }

  @Test
  public void getGradesReturnsAddedGrades() {
    service.addStudent("Alice", 3002);
    service.addGrade(3002, "Math", 1.3);
    assertEquals(1, service.getGrades(3002).size());
    assertEquals("Math", service.getGrades(3002).get(0).getSubject());
  }

  @Test
  public void getGradesReturnsEmptyListForUnknownStudent() {
    assertTrue(service.getGrades(9999).isEmpty());
  }

  @Test
  public void multipleSubjectsAverageCorrectly() {
    service.addStudent("Eve", 2004);
    service.addGrade(2004, "Math", 1.0);
    service.addGrade(2004, "Physics", 2.0);
    service.addGrade(2004, "English", 3.0);
    assertEquals(2.0, service.calculateAverage(2004), 0.001);
  }

  @Test
  public void getAllStudentsReturnsEmptyListWhenNoStudents() {
    assertTrue(service.getAllStudents().isEmpty());
  }

  @Test
  public void getAllStudentsReturnsSortedById() {
    service.addStudent("Zara", 5002);
    service.addStudent("Aaron", 5001);
    List<Student> all = service.getAllStudents();
    assertEquals(2, all.size());
    assertEquals(5001, all.get(0).getStudentId());
    assertEquals(5002, all.get(1).getStudentId());
  }
}
