package de.student.grademanager.service;

import de.student.grademanager.model.Grade;
import de.student.grademanager.model.Student;
import java.util.ArrayList;
import java.util.Collections;
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
    assert students.containsKey(studentId) && grades.containsKey(studentId)
        : "addStudent left maps inconsistent for id " + studentId;
  }

  @Override
  public void addGrade(int studentId, String subject, double value) {
    if (!students.containsKey(studentId)) {
      throw new IllegalArgumentException("Student not found: " + studentId);
    }
    List<Grade> gradeList = grades.get(studentId);
    assert gradeList != null : "grades map out of sync with students map for id " + studentId;
    gradeList.add(new Grade(subject, value));
  }

  @Override
  public double calculateAverage(int studentId) {
    List<Grade> studentGrades = grades.get(studentId);
    if (studentGrades == null || studentGrades.isEmpty()) {
      return 0.0;
    }
    double result = studentGrades.stream()
        .mapToDouble(Grade::getValue)
        .average()
        .orElse(0.0);
    assert result >= 0.0 && result <= 5.0
        : "calculateAverage produced out-of-range result: " + result;
    return result;
  }

  @Override
  public boolean hasStudent(int studentId) {
    return students.containsKey(studentId);
  }

  @Override
  public Student getStudent(int studentId) {
    return students.get(studentId);
  }

  @Override
  public List<Grade> getGrades(int studentId) {
    List<Grade> list = grades.get(studentId);
    return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
  }
}
