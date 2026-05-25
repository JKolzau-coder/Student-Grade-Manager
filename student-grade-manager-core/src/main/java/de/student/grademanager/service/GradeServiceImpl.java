package de.student.grademanager.service;

import de.student.grademanager.model.Grade;
import de.student.grademanager.model.Student;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GradeServiceImpl implements GradeService {
  private final Map<Integer, Student> students = new HashMap<>();
  private final Map<Integer, List<Grade>> grades = new HashMap<>();

  @Override
  public void addStudent(String name, int studentId) {
    students.put(studentId, new Student(name, studentId));
    grades.put(studentId, new ArrayList<>());
  }

  @Override
  public void addGrade(int studentId, String subject, double value) {
    if (!students.containsKey(studentId)) {
      throw new IllegalArgumentException("Student not found: " + studentId);
    }
    grades.get(studentId).add(new Grade(subject, value));
  }

  @Override
  public double calculateAverage(int studentId) {
    List<Grade> studentGrades = grades.get(studentId);
    if (studentGrades == null || studentGrades.isEmpty()) {
      return 0.0;
    }
    return studentGrades.stream()
        .mapToDouble(Grade::getValue)
        .average()
        .orElse(0.0);
  }

  @Override
  public boolean hasStudent(int studentId) {
    return students.containsKey(studentId);
  }
}
