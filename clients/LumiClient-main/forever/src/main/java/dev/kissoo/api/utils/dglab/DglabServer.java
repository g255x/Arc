package dev.kissoo.api.utils.dglab;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

public class DglabServer extends WebSocketServer {

    private boolean isConnected = false;
    private WebSocket client;
    private Timer heartbeatTimer;
    private final String clientId = "1234-123456789-12345-12345-00";
    private final String targetId = "targetId";

    public DglabServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (!isConnected) {
            isConnected = true;
            client = conn;
            sendJson("bind", "200");
            heartbeatTimer = new Timer();
            heartbeatTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (isConnected && client != null) {
                        sendJson("heartbeat", "200");
                    }
                }
            }, 0, 60000);
        } else {
            conn.send("{\"type\":\"error\",\"message\":\"400\"}");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (conn != null && conn.equals(client)) {
            isConnected = false;
            client = null;
            if (heartbeatTimer != null) {
                heartbeatTimer.cancel();
                heartbeatTimer = null;
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.contains("\"type\":\"bind\"") && message.contains("DGLAB")) {
            sendJson("bind", "200");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (!ex.getMessage().contains("Interrupted")) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        System.out.println("[BetterHurt] 郊狼 WebSocket Server 已启动，端口: " + getPort());
    }


    public void sendStrength(int channel, int strength) {
        if (isConnected && client != null) {
            String msg = "strength-" + channel + "+2+" + strength;
            sendJson("msg", msg);
        }
    }

    public void sendWaveform(int channel, String waveformHex) {
        if (isConnected && client != null) {
            sendJson("msg", "clear-" + channel);
            String prefix = (channel == 1) ? "pulse-A:[" : "pulse-B:[";
            sendJson("msg", prefix + waveformHex + "]");
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void sendJson(String type, String message) {
        if (client != null && client.isOpen()) {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            json.addProperty("clientId", clientId);
            json.addProperty("targetId", targetId);
            json.addProperty("message", message);
            client.send(json.toString());
        }
    }
}