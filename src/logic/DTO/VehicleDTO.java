package logic.DTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class VehicleDTO {

    private static final long NOT_FOUND_ID = -1L;
    private static final short NOT_FOUND_MODEL_YEAR = -1;
    private static final int NOT_FOUND_MILEAGE = -100;
    private static final BigDecimal NOT_FOUND_PRICE = new BigDecimal("-1000.00");
    private static final long NOT_FOUND_SUPPLIER_ID = -1L;
    private static final LocalDate NOT_FOUND_DATE = LocalDate.of(3000, 1, 1);
    private static final LocalDateTime NOT_FOUND_DATETIME = LocalDateTime.of(3000, 1, 1, 0, 0);

    private Long vehicleId;
    private String vin;
    private String make;
    private String model;
    private Short modelYear;
    private String color;
    private Integer mileageKm;
    private BigDecimal price;
    private VehicleStatus status;
    private Long supplierId;
    private LocalDate acquisitionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public VehicleDTO(){
    }

    public VehicleDTO(Long vehicleId, String vin, String make, String model, Short modelYear, String color,
                      Integer mileageKm, BigDecimal price, VehicleStatus status, Long supplierId,
                      LocalDate acquisitionDate, LocalDateTime createdAt, LocalDateTime updatedAt,
                      LocalDateTime deletedAt) {
        this.vehicleId = vehicleId;
        this.vin = vin;
        this.make = make;
        this.model = model;
        this.modelYear = modelYear;
        this.color = color;
        this.mileageKm = mileageKm;
        this.price = price;
        this.status = status;
        this.supplierId = supplierId;
        this.acquisitionDate = acquisitionDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public Long getVehicleId() { return vehicleId; }

    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public String getVin() { return vin; }

    public void setVin(String vin) { this.vin = vin; }

    public String getMake() { return make; }

    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }

    public void setModel(String model) { this.model = model; }

    public Short getModelYear() { return modelYear; }

    public void setModelYear(Short modelYear) { this.modelYear = modelYear; }

    public String getColor() { return color; }

    public void setColor(String color) { this.color = color; }

    public Integer getMileageKm() { return mileageKm; }

    public void setMileageKm(Integer mileageKm) { this.mileageKm = mileageKm; }

    public BigDecimal getPrice() { return price; }

    public void setPrice(BigDecimal price) { this.price = price; }

    public VehicleStatus getStatus() { return status; }

    public void setStatus(VehicleStatus status) { this.status = status; }

    public Long getSupplierId() { return supplierId; }

    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public LocalDate getAcquisitionDate() { return acquisitionDate; }

    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }

    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public static VehicleDTO createNotFoundVehicle() {
        return new VehicleDTO(NOT_FOUND_ID, "", "", "", NOT_FOUND_MODEL_YEAR, "", NOT_FOUND_MILEAGE, NOT_FOUND_PRICE,
                null, NOT_FOUND_SUPPLIER_ID, NOT_FOUND_DATE, NOT_FOUND_DATETIME, NOT_FOUND_DATETIME, null);
    }
}

