package logic.DTO;

import java.time.LocalDateTime;

public class VehiclePhotoDTO {

    private Long photoId;
    private Long vehicleId;
    private String uri;
    private String caption;
    private LocalDateTime createdAt;

    public Long getPhotoId() { return photoId; }

    public void setPhotoId(Long photoId) { this.photoId = photoId; }

    public Long getVehicleId() { return vehicleId; }

    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public String getUri() { return uri; }

    public void setUri(String uri) { this.uri = uri; }

    public String getCaption() { return caption; }

    public void setCaption(String caption) { this.caption = caption; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

