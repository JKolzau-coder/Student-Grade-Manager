package de.student.grademanager.model;

/**
 * Immutable entity representing a student.
 *
 * <p>Once created, a student's name and ID cannot change. The student ID
 * must be assigned by the caller and is expected to be unique within a
 * {@link de.student.grademanager.service.GradeService} instance.
 */
public final class Student {
  private final int studentId;
  private final String name;

  /**
   * Creates a new Student with the given name and ID.
   *
   * @param name      the student's full name; must not be {@code null}
   * @param studentId a caller-assigned numeric identifier
   * @throws IllegalArgumentException if {@code name} is {@code null}
   */
  public Student(String name, int studentId) {
    if (name == null) {
      throw new IllegalArgumentException("name must not be null");
    }
    this.name = name;
    this.studentId = studentId;
  }

  /**
   * Returns the student's numeric ID.
   *
   * @return the student ID
   */
  public int getStudentId() { return studentId; }

  /**
   * Returns the student's full name.
   *
   * @return the name; never {@code null}
   */
  public String getName() { return name; }
}
