package de.student.grademanager.model;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link Student}.
 *
 * <p>Tests the constructor directly — no mocks needed because {@code Student} has no
 * external dependencies. Covers the happy path and null-name validation.
 */
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
