package de.student.grademanager.model;

public class Student {
  private final int studentId;
  private final String name;

  public Student(String name, int studentId) {
    this.name = name;
    this.studentId = studentId;
  }

  public int getStudentId() { return studentId; }
  public String getName() { return name; }
}
