package de.student.grademanager.service;

import java.util.List;

import de.student.grademanager.model.Grade;
import de.student.grademanager.model.Student;

public interface GradeService {
  void addStudent(String name, int studentId);
  void addGrade(int studentId, String subject, double value);
  double calculateAverage(int studentId);
  boolean hasStudent(int studentId);
  Student getStudent(int studentId);
  List<Grade> getGrades(int studentId);
}
