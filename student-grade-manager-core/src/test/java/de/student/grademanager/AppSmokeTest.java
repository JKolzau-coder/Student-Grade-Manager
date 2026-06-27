package de.student.grademanager;

import static org.junit.Assert.assertNotNull;

import de.student.grademanager.service.GradeService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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
