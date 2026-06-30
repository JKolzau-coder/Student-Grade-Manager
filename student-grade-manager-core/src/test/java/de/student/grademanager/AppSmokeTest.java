package de.student.grademanager;

import static org.junit.Assert.assertNotNull;

import de.student.grademanager.service.GradeService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Spring Boot smoke tests for {@link App}.
 *
 * <p>Verifies that the application context starts successfully and that
 * {@link de.student.grademanager.service.GradeService} is wired into the context.
 * These tests catch wiring or configuration regressions that unit tests cannot detect.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class AppSmokeTest {

  @Autowired
  private GradeService gradeService;

  @Test
  public void contextLoadsAndGradeServiceIsWired() {
    assertNotNull(gradeService);
  }

  @Test
  public void mainMethodStartsWithoutException() {
    App.main(new String[]{});
    assertNotNull(App.class.getName());
  }
}
