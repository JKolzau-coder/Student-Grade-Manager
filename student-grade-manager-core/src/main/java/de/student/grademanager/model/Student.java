package de.student.grademanager.model;

public final class Student {
  private final int studentId;
  private final String name;

  // Validation of student names and IDs according to the defined data type
  public Student(String name, int studentId) {
    if (name == null) {
      throw new IllegalArgumentException("name must not be null");
    }
    this.name = name;
    this.studentId = studentId;
  }

// Pass student ID and names
  public int getStudentId() { return studentId; }
  public String getName() { return name; }
}
