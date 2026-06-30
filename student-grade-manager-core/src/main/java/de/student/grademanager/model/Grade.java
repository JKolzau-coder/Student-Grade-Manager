package de.student.grademanager.model;

/**
 * Immutable value object representing a single grade for one subject.
 *
 * <p>Valid grade values are between {@code 1.0} (best) and {@code 5.0} (worst),
 * matching the German academic grading scale.
 */
public final class Grade {
  private static final double MIN_GRADE = 1.0;
  private static final double MAX_GRADE = 5.0;

  private final String subject;
  private final double value;

  /**
   * Creates a new Grade for the given subject and numeric value.
   *
   * @param subject the name of the subject; must not be {@code null}
   * @param value   the numeric grade; must be between {@code 1.0} and {@code 5.0} inclusive
   * @throws IllegalArgumentException if {@code subject} is {@code null} or {@code value}
   *                                  is outside the valid range
   */
  public Grade(String subject, double value) {
    if (subject == null) {
      throw new IllegalArgumentException("subject must not be null");
    }
    if (value < MIN_GRADE || value > MAX_GRADE) {
      throw new IllegalArgumentException("Grade must be between 1.0 and 5.0");
    }
    this.subject = subject;
    this.value = value;
  }

  /**
   * Returns the subject name.
   *
   * @return the subject name; never {@code null}
   */
  public String getSubject() { return subject; }

  /**
   * Returns the numeric grade value.
   *
   * @return a value in the range {@code [1.0, 5.0]}
   */
  public double getValue() { return value; }
}
