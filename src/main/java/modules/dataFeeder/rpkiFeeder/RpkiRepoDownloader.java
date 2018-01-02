package modules.dataFeeder.rpkiFeeder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class RpkiRepoDownloader implements Runnable {

    private Vertx vertx;

    public RpkiRepoDownloader() {
    }

    @Override
    public void run() {
        WebClient client = WebClient.create(vertx);

        client
                .get(9176, "localhost", "/export.json")
                .send(ar -> {
                    if (ar.succeeded()) {
                        // Obtain response
                        HttpResponse<Buffer> response = ar.result();
                        System.out.println("Got HTTP response body");
                        System.out.println(response.body().encodePrettily());
                    } else {
                        System.out.println("Something went wrong " + ar.cause().getMessage());
                    }
                });
    }
}
