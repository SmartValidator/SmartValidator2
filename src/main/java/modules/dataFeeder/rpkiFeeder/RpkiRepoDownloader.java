package modules.dataFeeder.rpkiFeeder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class RpkiRepoDownloader implements Runnable {

    private Vertx vertx;

    public RpkiRepoDownloader() {
    }

    @Override
    public void run() {
        URL url = null;
        HttpURLConnection rpkiValidatorRestApiConnection = null;

        try {
            url = new URL("http://localhost:9176/export.json");

            rpkiValidatorRestApiConnection = (HttpURLConnection) url.openConnection();
            rpkiValidatorRestApiConnection.setRequestMethod("GET");
            rpkiValidatorRestApiConnection.setUseCaches(false);
            rpkiValidatorRestApiConnection.setAllowUserInteraction(false);

            rpkiValidatorRestApiConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:57.0) Gecko/20100101 Firefox/57.0");
            rpkiValidatorRestApiConnection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            int status = rpkiValidatorRestApiConnection.getResponseCode();
            switch(status){
                case 200:
                case 201:
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(rpkiValidatorRestApiConnection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    System.out.println("Got response " + content);
                    in.close();

                default:
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            assert rpkiValidatorRestApiConnection != null;
            rpkiValidatorRestApiConnection.disconnect();

        }

    }
}
