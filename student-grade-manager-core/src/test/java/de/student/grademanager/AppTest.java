package de.student.grademanager;

import de.student.grademanager.service.GradeService;
import java.util.Scanner;
import org.junit.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AppTest {

  @Test
  public void runInvokesServiceMethodsWithCorrectArguments() {
    GradeService mockService = mock(GradeService.class);
    when(mockService.calculateAverage(1001)).thenReturn(1.65);

    new App(mockService).run();

    verify(mockService).addStudent("Alice", 1001);
    verify(mockService).addGrade(1001, "Math", 1.3);
    verify(mockService).addGrade(1001, "Physics", 2.0);
    verify(mockService).calculateAverage(1001);
  }

  @Test
  public void runHandlesIllegalArgumentExceptionWithoutPropagating() {
    GradeService mockService = mock(GradeService.class);
    doThrow(new IllegalArgumentException("Student not found: 1001"))
        .when(mockService).addGrade(1001, "Math", 1.3);

    new App(mockService).run();

    verify(mockService).addStudent("Alice", 1001);
    verify(mockService).addGrade(1001, "Math", 1.3);
  }

  @Test(timeout = 1000)
  public void runCompletesWithinOneSecond() {
    GradeService mockService = mock(GradeService.class);
    when(mockService.calculateAverage(1001)).thenReturn(1.65);

    new App(mockService).run();

    verify(mockService).addStudent("Alice", 1001);
    verify(mockService).calculateAverage(1001);
  }

  @Test
  public void runInteractiveAddsStudentAndGradeFromScanner() {
    GradeService mockService = mock(GradeService.class);
    when(mockService.calculateAverage(2001)).thenReturn(2.0);
    Scanner scanner = new Scanner("Bob\n2001\nJava\n2.0\n");

    new App(mockService).runInteractive(scanner);

    verify(mockService).addStudent("Bob", 2001);
    verify(mockService).addGrade(2001, "Java", 2.0);
    verify(mockService).calculateAverage(2001);
  }

  @Test
  public void printComparisonLogsWarningWhenNoInteractiveStudent() {
    GradeService mockService = mock(GradeService.class);
    new App(mockService).printComparison();
    verify(mockService, org.mockito.Mockito.never()).getStudent(org.mockito.ArgumentMatchers.anyInt());
  }
}
