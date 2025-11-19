package logic.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleDTO {

    private Long saleId;
    private String folio;
    private Long vehicleId;
    private String costumerNumber;
    private Long sellerAccountId;
    private SaleStatus status;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal taxes;
    private BigDecimal total;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private LocalDateTime annulledAt;
    private String annulReason;

    public Long getSaleId() {
        return saleId;
    }

    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getCostumerNumber() {
        return costumerNumber;
    }

    public void setCostumerNumber(String costumerNumber) {
        this.costumerNumber = costumerNumber;
    }

    public Long getSellerAccountId() {
        return sellerAccountId;
    }

    public void setSellerAccountId(Long sellerAccountId) {
        this.sellerAccountId = sellerAccountId;
    }

    public SaleStatus getStatus() {
        return status;
    }

    public void setStatus(SaleStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTaxes() {
        return taxes;
    }

    public void setTaxes(BigDecimal taxes) {
        this.taxes = taxes;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getAnnulledAt() {
        return annulledAt;
    }

    public void setAnnulledAt(LocalDateTime annulledAt) {
        this.annulledAt = annulledAt;
    }

    public String getAnnulReason() {
        return annulReason;
    }

    public void setAnnulReason(String annulReason) {
        this.annulReason = annulReason;
    }
}
