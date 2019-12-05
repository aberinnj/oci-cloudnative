package mushop.carts.test;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.helidon.webserver.WebServer;
import mushop.carts.Cart;
import mushop.carts.Item;
import mushop.carts.Main;

public class TestCartService {
    
    WebServer server;
    
    @BeforeEach
    public void setUp() throws Exception {
        server = Main.createWebServer();
        server.start().toCompletableFuture().join();
    }

    @AfterEach
    public void tearDown() throws Exception {
        server.shutdown().toCompletableFuture().join();
        server = null;
    }

    @Test
    public void testHealthCheck() throws Exception {
        JsonValue result = get(baseUrl() + "/health");
        assertEquals("UP", result.asJsonObject().getString("status"));
    }

    @Test
    public void testStoreCart() throws Exception {
        Item i = new Item();
        i.setUnitPrice(BigDecimal.valueOf(123));
        i.setQuantity(47);
        i.setItemId("I123");
        
        Cart c = new Cart();
        c.setCustomerId("c1");
        c.getItems().add(i);
        
        Jsonb jsonb = JsonbBuilder.create();
        JsonObject cValue = parse(jsonb.toJson(c)).asJsonObject();
        int res = post(baseUrl() + "/carts/" + c.getId(), cValue);
        assertEquals(HTTP_CREATED, res);
        
        JsonArray arr = get(baseUrl() + "/carts/" + c.getId()  + "/items").asJsonArray();
        JsonValue iValue = arr.get(0);
        assertEquals(cValue.get("items").asJsonArray().get(0), iValue);
        
        res = delete(baseUrl() + "/carts/" + c.getId() + "/items/" + i.getItemId());
        assertEquals(HTTP_OK, res);
        
        arr =  get(baseUrl() + "/carts/" + c.getId()  + "/items").asJsonArray();
        assertEquals(0, arr.size());
    }
    
    
    private JsonValue parse(String json) {
        JsonReader reader = Json.createReader(new StringReader(json));
        JsonValue result = reader.readValue();
        reader.close();
        return result;
    }

    private int post(String urlStr, JsonValue body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        try (JsonWriter writer = Json.createWriter(con.getOutputStream())) {
            writer.write(body);
        }
        return con.getResponseCode();
    }
    
    private int delete(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("DELETE");
        return con.getResponseCode();
    }
    
    private JsonValue get(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        try (JsonReader reader = Json.createReader(con.getInputStream())) {
            return reader.readValue();
        }
    }
    
    private String baseUrl() throws UnknownHostException {
        InetAddress host = InetAddress.getLocalHost();
        return "http://" + host.getHostName() + 
                ":" + server.port();
    }
}
