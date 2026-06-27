package de.student.grademanager;

import de.student.grademanager.model.Grade;
import de.student.grademanager.model.Student;
import de.student.grademanager.service.GradeService;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
  public void runInteractiveHandlesInvalidInputWithoutPropagating() {
    Scanner scanner = new Scanner("Bob\nnot-a-number\nJava\n2.0\n");
    new App(mockService).runInteractive(scanner);
    verify(mockService, never()).addStudent(anyString(), anyInt());
  }

  @Test
  public void printComparisonLogsWarningWhenNoInteractiveStudent() {
    new App(mockService).printComparison();
    verify(mockService, never()).getStudent(anyInt());
  }

  @Test
  public void printComparisonRendersTableWithSingleAndEmptyGradeRows() {
    when(mockService.calculateAverage(2001)).thenReturn(2.0);
    when(mockService.getStudent(1001)).thenReturn(new Student("Alice", 1001));
    when(mockService.getStudent(2001)).thenReturn(new Student("Bob", 2001));
    when(mockService.getGrades(1001)).thenReturn(List.of(new Grade("Math", 1.3)));
    when(mockService.getGrades(2001)).thenReturn(Collections.emptyList());
    when(mockService.calculateAverage(1001)).thenReturn(1.3);

    App app = new App(mockService);
    app.runInteractive(new Scanner("Bob\n2001\nJava\n2.0\n"));
    app.printComparison();

    verify(mockService).getStudent(1001);
    verify(mockService).getStudent(2001);
  }

  @Test
  public void printComparisonRendersMultipleGradeRows() {
    when(mockService.calculateAverage(2001)).thenReturn(2.0);
    when(mockService.getStudent(1001)).thenReturn(new Student("Alice", 1001));
    when(mockService.getStudent(2001)).thenReturn(new Student("Bob", 2001));
    when(mockService.getGrades(1001)).thenReturn(
        List.of(new Grade("Math", 1.3), new Grade("Physics", 2.0)));
    when(mockService.getGrades(2001)).thenReturn(Collections.emptyList());
    when(mockService.calculateAverage(1001)).thenReturn(1.65);

    App app = new App(mockService);
    app.runInteractive(new Scanner("Bob\n2001\nJava\n2.0\n"));
    app.printComparison();

    verify(mockService).getStudent(1001);
    verify(mockService).getStudent(2001);
  }

  @Test
  public void printComparisonLogsWarningWhenStudentNotFound() {
    when(mockService.calculateAverage(2001)).thenReturn(2.0);
    when(mockService.getStudent(anyInt())).thenReturn(null);

    App app = new App(mockService);
    app.runInteractive(new Scanner("Bob\n2001\nJava\n2.0\n"));
    app.printComparison();

    verify(mockService).getStudent(1001);
  }
}
