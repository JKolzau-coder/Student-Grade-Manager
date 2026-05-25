package de.student.grademanager.service;

public interface GradeService {
  void addStudent(String name, int studentId);
  void addGrade(int studentId, String subject, double value);
  double calculateAverage(int studentId);
  boolean hasStudent(int studentId);
}
