package domain.model;

import ddd.ValueObject;
import java.io.Serializable;

public class P2d implements ValueObject, Serializable {

  private final double x;
  private final double y;

  public P2d(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public String toString() {
    return "P2d(" + x + "," + y + ")";
  }
}
