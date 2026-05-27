package de.student.grademanager.model;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class StudentTest {

  @Test
  public void constructorStoresNameAndStudentId() {
    Student student = new Student("Alice", 1001);
    assertEquals("Alice", student.getName());
    assertEquals(1001, student.getStudentId());
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullNameThrowsException() {
    new Student(null, 1001);
  }
}
