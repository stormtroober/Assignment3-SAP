package domain.model;

public class StationFactory {
  private static final StationFactory INSTANCE = new StationFactory();

  private StationFactory() {}

  public static StationFactory getInstance() {
    return INSTANCE;
  }

  public Station createStation(String id, float x, float y) {
    return new Station(id, new P2d(x, y));
  }
}