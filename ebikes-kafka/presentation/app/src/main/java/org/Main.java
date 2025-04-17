package org;

import io.vertx.core.Vertx;
import org.views.MainView;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        new MainView(vertx).display();
    }
}