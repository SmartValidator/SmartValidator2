package modules.webServer;

import com.alibaba.fastjson.JSON;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class backendServer {
    public static void backendServer(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(6662), 0);
        server.createContext("/modeChange", new MainHandler());
        server.createContext("/shutdown", new MainHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {

            String response = "";
//            JSON.parse(t.getRequestBody());
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
