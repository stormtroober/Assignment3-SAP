package domain.model;

import ddd.ValueObject;

import java.io.Serializable;

public class V2d implements ValueObject, Serializable{

    private double x,y;

    public V2d(double x,double y){
        this.x = x;
        this.y = y;
    }


    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public V2d sum(V2d v){
        return new V2d(x+v.x,y+v.y);
    }

    public V2d rotate(double degree) {
        var rad = degree * Math.PI/180;
        var cs = Math.cos(rad);
        var sn = Math.sin(rad);
        var x1 = x * cs - y * sn;
        var y1 = x * sn + y * cs;
        var v = new V2d(x1, y1).getNormalized();
        return v;
    }

    public double abs(){
        return (double)Math.sqrt(x*x+y*y);
    }

    public V2d getNormalized(){
        double module=(double)Math.sqrt(x*x+y*y);
        return new V2d(x/module,y/module);
    }

    public V2d mul(double fact){
        return new V2d(x*fact,y*fact);
    }

    public String toString(){
        return "V2d("+x+","+y+")";
    }
}
