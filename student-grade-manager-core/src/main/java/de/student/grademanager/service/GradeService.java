package de.student.grademanager.service;

import de.student.grademanager.model.Grade;
import de.student.grademanager.model.Student;
import java.util.List;

/**
 * Service interface for managing students and their grades.
 *
 * <p>All mutating operations validate their arguments and throw
 * {@link IllegalArgumentException} on invalid input. Collection-returning
 * methods never return {@code null}; they return an empty collection instead.
 */
public interface GradeService {

  /**
   * Registers a new student.
   *
   * @param name      the student's full name; must not be {@code null}
   * @param studentId a unique numeric identifier for the student
   * @throws IllegalArgumentException if {@code name} is {@code null}
   */
  void addStudent(String name, int studentId);

  /**
   * Adds a grade for an already-registered student.
   *
   * @param studentId the ID of an existing student
   * @param subject   the name of the subject; must not be {@code null}
   * @param value     the numeric grade value in the range {@code [1.0, 5.0]}
   * @throws IllegalArgumentException if the student does not exist, {@code subject}
   *                                  is {@code null}, or {@code value} is out of range
   */
  void addGrade(int studentId, String subject, double value);

  /**
   * Calculates the arithmetic mean of all grades for a student.
   *
   * @param studentId the ID of an existing student
   * @return the average grade, or {@code 0.0} if the student has no grades
   */
  double calculateAverage(int studentId);

  /**
   * Returns whether a student with the given ID is registered.
   *
   * @param studentId the ID to look up
   * @return {@code true} if the student exists; {@code false} otherwise
   */
  boolean hasStudent(int studentId);

  /**
   * Returns the student with the given ID.
   *
   * @param studentId the ID to look up
   * @return the {@link Student}, or {@code null} if not found
   */
  Student getStudent(int studentId);

  /**
   * Returns all grades recorded for a student.
   *
   * @param studentId the ID of the student
   * @return an unmodifiable list of grades; empty if none recorded or student unknown
   */
  List<Grade> getGrades(int studentId);

  /**
   * Returns all registered students.
   *
   * @return an unmodifiable list of all students; empty if none registered
   */
  List<Student> getAllStudents();
}
