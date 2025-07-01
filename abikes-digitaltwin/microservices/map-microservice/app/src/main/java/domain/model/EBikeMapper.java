package domain.model;

import domain.events.EBikeUpdate;
import domain.events.Location;

public class EBikeMapper {

  public static EBike fromAvro(EBikeUpdate update) {
    String id = update.getId();
    Location avroLoc = update.getLocation();
    P2d location = avroLoc != null ? new P2d((float) avroLoc.getX(), (float) avroLoc.getY()) : null;
    EBikeState state = EBikeState.valueOf(update.getState());
    Integer batteryLevel = update.getBatteryLevel();
    int battery = batteryLevel != null ? batteryLevel : 0;
    BikeType type = BikeType.valueOf(update.getType());
    return new EBike(id, location, state, battery, type);
  }
}
