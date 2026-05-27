package de.student.grademanager.model;

public final class Student {
  private final int studentId;
  private final String name;

  public Student(String name, int studentId) {
    if (name == null) {
      throw new IllegalArgumentException("name must not be null");
    }
    this.name = name;
    this.studentId = studentId;
  }

  public int getStudentId() { return studentId; }
  public String getName() { return name; }
}
