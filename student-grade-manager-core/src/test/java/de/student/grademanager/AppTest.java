package de.student.grademanager;

import de.student.grademanager.model.Student;
import de.student.grademanager.service.GradeService;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertTrue;

public class AppTest {

  private GradeService mockService;

  @BeforeClass
  public static void verifyAppIsFinal() {
    assertTrue(
        "App muss final sein um EI_EXPOSE_REP2 zu verhindern",
        Modifier.isFinal(App.class.getModifiers())
    );
  }

  @Before
  public void setUp() {
    mockService = mock(GradeService.class);
  }

  @Test
  public void runInvokesServiceMethodsWithCorrectArguments() {
    when(mockService.calculateAverage(1001)).thenReturn(1.65);

    new App(mockService).run();

    verify(mockService).addStudent("Alice", 1001);
    verify(mockService).addGrade(1001, "Math", 1.3);
    verify(mockService).addGrade(1001, "Physics", 2.0);
    verify(mockService).calculateAverage(1001);
  }

  @Test
  public void runHandlesIllegalArgumentExceptionWithoutPropagating() {
    doThrow(new IllegalArgumentException("Student not found: 1001"))
        .when(mockService).addGrade(1001, "Math", 1.3);

    new App(mockService).run();

    verify(mockService).addStudent("Alice", 1001);
    verify(mockService).addGrade(1001, "Math", 1.3);
  }

  @Test(timeout = 1000)
  public void runCompletesWithinOneSecond() {
    when(mockService.calculateAverage(1001)).thenReturn(1.65);

    new App(mockService).run();

    verify(mockService).addStudent("Alice", 1001);
    verify(mockService).calculateAverage(1001);
  }

  @Test
  public void runInteractiveAddsStudentAndGradeFromScanner() {
    when(mockService.calculateAverage(2001)).thenReturn(2.0);
    Scanner scanner = new Scanner("Bob\n2001\nJava\n2.0\n");

    new App(mockService).runInteractive(scanner);

    verify(mockService).addStudent("Bob", 2001);
    verify(mockService).addGrade(2001, "Java", 2.0);
    verify(mockService).calculateAverage(2001);
  }

  @Test
  public void printComparisonLogsWarningWhenNoInteractiveStudent() {
    new App(mockService).printComparison();
    verify(mockService, never()).getStudent(anyInt());
  }

  @Test
  public void runInputLoopAddsNewStudentAndGrade() {
    when(mockService.hasStudent(3001)).thenReturn(false);
    Scanner scanner = new Scanner("Carol\n3001\nChemistry\n1.7\n\n");

    new App(mockService).runInputLoop(scanner);

    verify(mockService).addStudent("Carol", 3001);
    verify(mockService).addGrade(3001, "Chemistry", 1.7);
  }

  @Test
  public void runInputLoopSkipsAddStudentForExistingId() {
    when(mockService.hasStudent(3002)).thenReturn(true);
    Scanner scanner = new Scanner("Dave\n3002\nHistory\n2.3\n\n");

    new App(mockService).runInputLoop(scanner);

    verify(mockService, never()).addStudent(anyString(), anyInt());
    verify(mockService).addGrade(3002, "History", 2.3);
  }

  @Test
  public void runInputLoopStopsOnEmptyName() {
    Scanner scanner = new Scanner("\n");

    new App(mockService).runInputLoop(scanner);

    verify(mockService, never()).addStudent(anyString(), anyInt());
    verify(mockService, never()).addGrade(anyInt(), anyString(), anyDouble());
  }

  @Test
  public void printTableCallsGetAllStudents() {
    when(mockService.getAllStudents()).thenReturn(Collections.emptyList());

    new App(mockService).printTable();

    verify(mockService).getAllStudents();
    verify(mockService, never()).getGrades(anyInt());
  }

  @Test
  public void printTableRendersRowsForEachStudent() {
    Student alice = new Student("Alice", 1001);
    List<Student> students = Arrays.asList(alice);
    when(mockService.getAllStudents()).thenReturn(students);
    when(mockService.getGrades(1001)).thenReturn(Collections.emptyList());
    when(mockService.calculateAverage(1001)).thenReturn(0.0);

    new App(mockService).printTable();

    verify(mockService).getGrades(1001);
    verify(mockService).calculateAverage(1001);
  }
}
