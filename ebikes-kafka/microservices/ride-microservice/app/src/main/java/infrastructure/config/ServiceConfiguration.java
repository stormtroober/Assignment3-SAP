package infrastructure.config;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ServiceConfiguration {

    private static ServiceConfiguration instance;
    private final Vertx vertx;
    private JsonObject config;
    private final ConfigRetriever retriever;

    private ServiceConfiguration(Vertx vertx) {
        this.vertx = vertx;
        this.retriever = initializeRetriever();
    }

    public static synchronized ServiceConfiguration getInstance(Vertx vertx) {
        if (instance == null) {
            instance = new ServiceConfiguration(vertx);
        }
        return instance;
    }

    private ConfigRetriever initializeRetriever() {
        ConfigStoreOptions envStore = new ConfigStoreOptions()
                .setType("env")
                .setConfig(new JsonObject()
                        .put("keys", new JsonArray()
                                .add("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE")
                                .add("EUREKA_HOST")
                                .add("EUREKA_PORT")
                                .add("SERVICE_NAME")
                                .add("SERVICE_PORT")
                                .add("MAP_HOST")
                                .add("MAP_PORT")
                                .add("EBIKE_HOST")
                                .add("EBIKE_PORT")
                                .add("USER_HOST")
                                .add("USER_PORT")

                        )
                );
        return ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(envStore));
    }

    public Future<JsonObject> load() {
        return retriever.getConfig()
                .onSuccess(conf -> {
                    this.config = conf;
                    // Listen for changes
                    retriever.listen(change -> {
                        this.config = change.getNewConfiguration();
                        System.out.println("Configuration updated: " + this.config.encodePrettily());
                    });
                });
    }

    public JsonObject getEurekaConfig() {
        return new JsonObject()
                .put("serviceUrl", config.getString("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", "http://eureka-server:8761/eureka/"))
                .put("host", config.getString("EUREKA_HOST", "eureka-server"))
                .put("port", config.getInteger("EUREKA_PORT", 8761));
    }

    public JsonObject getServiceConfig() {
        return new JsonObject()
                .put("name", config.getString("SERVICE_NAME", "ride-microservice"))
                .put("port", config.getInteger("SERVICE_PORT", 8080));
    }

    public JsonObject getMapAdapterAddress() {
        return new JsonObject()
                .put("name", config.getString("MAP_HOST", "map-microservice"))
                .put("port", config.getInteger("MAP_PORT", 8081));
    }

    public JsonObject getEBikeAdapterAddress() {
        return new JsonObject()
                .put("name", config.getString("EBIKE_HOST", "ebike-microservice"))
                .put("port", config.getInteger("EBIKE_PORT", 8081));
    }

    public JsonObject getUserAdapterAddress() {
        return new JsonObject()
                .put("name", config.getString("USER_HOST", "user-microservice"))
                .put("port", config.getInteger("USER_PORT", 8081));
    }


}