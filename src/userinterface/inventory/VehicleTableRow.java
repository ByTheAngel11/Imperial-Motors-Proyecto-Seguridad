package userinterface.inventory;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import logic.DTO.VehicleDTO;

import java.math.BigDecimal;

public class VehicleTableRow {

    private final VehicleDTO vehicle;

    private final StringProperty id     = new SimpleStringProperty();
    private final StringProperty make   = new SimpleStringProperty();
    private final StringProperty model  = new SimpleStringProperty();
    private final StringProperty year   = new SimpleStringProperty();
    private final StringProperty vin    = new SimpleStringProperty();
    private final StringProperty price  = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();

    public VehicleTableRow(VehicleDTO v) {
        this.vehicle = v;

        this.id.set(v.getVehicleId() != null ? String.valueOf(v.getVehicleId()) : "-");
        this.make.set(safe(v.getMake()));
        this.model.set(safe(v.getModel()));
        this.year.set(v.getModelYear() != null ? Short.toString(v.getModelYear()) : "-");
        this.vin.set(safe(v.getVin()));
        this.price.set(formatPrice(v.getPrice()));
        this.status.set(v.getStatus() != null ? v.getStatus().name() : "-");
    }

    private String safe(String s) {
        return s != null ? s : "-";
    }

    private String formatPrice(BigDecimal p) {
        if (p == null) return "-";
        return "$" + p.toPlainString();
    }

    public VehicleDTO getVehicle() { return vehicle; }

    public String getId()     { return id.get(); }
    public String getMake()   { return make.get(); }
    public String getModel()  { return model.get(); }
    public String getYear()   { return year.get(); }
    public String getVin()    { return vin.get(); }
    public String getPrice()  { return price.get(); }
    public String getStatus() { return status.get(); }

    public StringProperty idProperty()     { return id; }
    public StringProperty makeProperty()   { return make; }
    public StringProperty modelProperty()  { return model; }
    public StringProperty yearProperty()   { return year; }
    public StringProperty vinProperty()    { return vin; }
    public StringProperty priceProperty()  { return price; }
    public StringProperty statusProperty() { return status; }
}
