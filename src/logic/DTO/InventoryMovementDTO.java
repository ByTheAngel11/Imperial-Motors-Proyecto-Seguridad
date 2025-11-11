package logic.DTO;

import java.time.LocalDateTime;

public class InventoryMovementDTO {

    private Long movementId;
    private Long vehicleId;
    private InventoryMovementType type;
    private String refTable;
    private Long refId;
    private String note;
    private Long accountId;
    private LocalDateTime createdAt;

    public Long getMovementId() { return movementId; }

    public void setMovementId(Long movementId) { this.movementId = movementId; }

    public Long getVehicleId() { return vehicleId; }

    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public InventoryMovementType getType() { return type; }

    public void setType(InventoryMovementType type) { this.type = type; }

    public String getRefTable() { return refTable; }

    public void setRefTable(String refTable) { this.refTable = refTable; }

    public Long getRefId() { return refId; }

    public void setRefId(Long refId) { this.refId = refId; }

    public String getNote() { return note; }

    public void setNote(String note) { this.note = note; }

    public Long getAccountId() { return accountId; }

    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

