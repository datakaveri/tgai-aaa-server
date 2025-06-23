import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.cdpg.dx.aaa.organization.dao.OrganizationDAO;
import org.cdpg.dx.aaa.organization.models.Organization;
import org.cdpg.dx.aaa.organization.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class OrganizationServiceTest {

  @Mock
  private OrganizationDAO organizationDAO;

  @InjectMocks
  private OrganizationService organizationService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGetOrganizationById() throws ExecutionException, InterruptedException {
    String orgId = "org-123";
    Organization mockOrg = new Organization(orgId, "Test Org");

    when(organizationDAO.get(orgId))
      .thenReturn(CompletableFuture.completedFuture(mockOrg));

    Organization result = organizationService.getOrganizationById(orgId).get();

    assertNotNull(result);
    assertEquals(orgId, result.getId());
    assertEquals("Test Org", result.getName());
    verify(organizationDAO, times(1)).getOrganizationById(orgId);
  }
}
