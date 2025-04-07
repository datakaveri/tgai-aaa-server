package org.cdpg.dx.aaa.organization.util;

public enum Status {
  PENDING("pending"),
  USER("user");

  private final String status;

  Status(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  private static Status temp;
  public static Status fromString(String statusStr) {
    for (Status status: Status.values()) {
      if (status.getStatus().equalsIgnoreCase(statusStr))
        temp=status;
    }

    return temp;

  }

}
