package de.student.grademanager.model;

public class Grade {
  private static final double MIN_GRADE = 1.0;
  private static final double MAX_GRADE = 5.0;

  private final String subject;
  private final double value;

  public Grade(String subject, double value) {
    if (value < MIN_GRADE || value > MAX_GRADE) {
      throw new IllegalArgumentException("Grade must be between 1.0 and 5.0");
    }
    this.subject = subject;
    this.value = value;
  }

  public String getSubject() { return subject; }
  public double getValue() { return value; }
}
