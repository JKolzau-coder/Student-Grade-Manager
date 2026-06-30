package de.student.grademanager.model;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link Grade}.
 *
 * <p>Tests the constructor directly — no mocks needed because {@code Grade} has no
 * external dependencies. Covers valid values, lower and upper boundaries, and all
 * invalid-input paths that must throw {@link IllegalArgumentException}.
 */
public class GradeTest {

  @Test
  public void validGradeStoresCorrectly() {
    Grade grade = new Grade("Math", 3.0);
    assertEquals("Math", grade.getSubject());
    assertEquals(3.0, grade.getValue(), 0.001);
  }

  @Test
  public void lowerBoundaryGradeIsValid() {
    Grade grade = new Grade("Math", 1.0);
    assertEquals(1.0, grade.getValue(), 0.001);
  }

  @Test
  public void upperBoundaryGradeIsValid() {
    Grade grade = new Grade("Math", 5.0);
    assertEquals(5.0, grade.getValue(), 0.001);
  }

  @Test(expected = IllegalArgumentException.class)
  public void gradeBelowMinimumThrowsException() {
    new Grade("Math", 0.9);
  }

  @Test(expected = IllegalArgumentException.class)
  public void gradeAboveMaximumThrowsException() {
    new Grade("Math", 5.1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullSubjectThrowsException() {
    new Grade(null, 2.0);
  }
}
