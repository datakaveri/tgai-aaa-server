package org.cdpg.dx.aaa.report.model;

import java.util.List;
import org.cdpg.dx.aaa.organization.models.OrganizationCreateRequest;

public record ReportMetaData(List<OrganizationCreateRequest> activityLogList, long count) {}
