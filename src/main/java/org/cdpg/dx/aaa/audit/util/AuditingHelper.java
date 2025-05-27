package org.cdpg.dx.aaa.audit.util;

import org.cdpg.dx.aaa.audit.model.AAAAuditlog;
import org.cdpg.dx.auditing.model.AuditLog;

import java.util.UUID;
//TODO update these
public class AuditingHelper {
  private AuditingHelper() {
  }

  public static AuditLog createAuditLog(UUID id) {
    return new AAAAuditlog(id);
  }

}
