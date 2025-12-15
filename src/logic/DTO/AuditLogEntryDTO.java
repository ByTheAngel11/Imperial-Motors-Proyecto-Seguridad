package logic.DTO;

import java.time.LocalDateTime;

public class AuditLogEntryDTO {

    private Long auditId;

    private String actorPersonnelNumber;

    private String action;
    private String entity;
    private String entityName;

    private String beforeData;
    private String afterData;
    private String ipAddress;
    private LocalDateTime createdAt;

    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }

    public String getActorPersonnelNumber() { return actorPersonnelNumber; }
    public void setActorPersonnelNumber(String actorPersonnelNumber) { this.actorPersonnelNumber = actorPersonnelNumber; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getBeforeData() { return beforeData; }
    public void setBeforeData(String beforeData) { this.beforeData = beforeData; }

    public String getAfterData() { return afterData; }
    public void setAfterData(String afterData) { this.afterData = afterData; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
