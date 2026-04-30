package de.caco3.remotekeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class WebKeyboardServer {

    public static final String TAG = "WebKeyboardServer";
    public static final String PREF_PASSWORD = "pref_password";
    public static final int PORT = 4430;

    private static final int TOKEN_BYTES = 32;

    private SSLServerSocket serverSocket;
    private ServerSocket httpRedirectSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;
    public static final int HTTP_REDIRECT_PORT = 4431;
    private final Set<String> validTokens = Collections.synchronizedSet(new HashSet<String>());
    private static final SecureRandom random = new SecureRandom();
    private String htmlContent = "";

    private static final Map<String, Integer> SPECIAL_KEY_MAP;
    private static final Map<String, Integer> CTRL_KEY_MAP;

    static {
        Map<String, Integer> m = new HashMap<>();
        m.put("Backspace", KeyConstants.BACKSPACE);
        m.put("Delete",    Decoder.SYM_DELETE);
        m.put("Enter",     KeyConstants.ENTER);
        m.put("Tab",       KeyConstants.TABULATOR);
        m.put("ArrowLeft",  Decoder.SYM_CURSOR_LEFT);
        m.put("ArrowRight", Decoder.SYM_CURSOR_RIGHT);
        m.put("ArrowUp",    Decoder.SYM_CURSOR_UP);
        m.put("ArrowDown",  Decoder.SYM_CURSOR_DOWN);
        m.put("Home",    Decoder.SYM_HOME);
        m.put("End",     Decoder.SYM_END);
        m.put("Insert",  Decoder.SYM_INSERT);
        m.put("PageUp",   Decoder.SYM_PAGE_UP);
        m.put("PageDown", Decoder.SYM_PAGE_DOWN);
        m.put("F1",  Decoder.SYM_F1);
        m.put("F2",  Decoder.SYM_F2);
        m.put("F3",  Decoder.SYM_F3);
        m.put("F4",  Decoder.SYM_F4);
        m.put("F5",  Decoder.SYM_F5);
        m.put("F6",  Decoder.SYM_F6);
        m.put("F7",  Decoder.SYM_F7);
        m.put("F8",  Decoder.SYM_F8);
        m.put("F9",  Decoder.SYM_F9);
        m.put("F10", Decoder.SYM_F10);
        m.put("F11", Decoder.SYM_F11);
        m.put("F12", Decoder.SYM_F12);
        SPECIAL_KEY_MAP = Collections.unmodifiableMap(m);

        Map<String, Integer> c = new HashMap<>();
        c.put("A", KeyConstants.COLORINIT);
        c.put("C", 3);
        c.put("V", 22);
        c.put("X", 24);
        c.put("L", 12);
        c.put("Q", 17);
        c.put("R", 18);
        c.put("S", 19);
        CTRL_KEY_MAP = Collections.unmodifiableMap(c);
    }

    public void start(Context context) throws IOException {
        loadHtml(context);

        SSLServerSocketFactory factory = SslHelper.getServerSocketFactory();
        if (factory == null) {
            Log.e(TAG, "TLS not available on this device (requires API 23+)");
            return;
        }
        serverSocket = (SSLServerSocket) factory.createServerSocket(PORT);
        serverSocket.setNeedClientAuth(false);
        running = true;
        threadPool = Executors.newCachedThreadPool();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                acceptLoop();
            }
        }, "WebKeyboard-accept");
        t.setDaemon(true);
        t.start();
        startHttpRedirect();
        Log.i(TAG, "HTTPS server started on port " + PORT);
    }

    private void startHttpRedirect() {
        try {
            httpRedirectSocket = new ServerSocket(HTTP_REDIRECT_PORT);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    httpRedirectLoop();
                }
            }, "WebKeyboard-redirect");
            t.setDaemon(true);
            t.start();
            Log.i(TAG, "HTTP redirect server started on port " + HTTP_REDIRECT_PORT);
        } catch (IOException e) {
            Log.w(TAG, "Could not start HTTP redirect server: " + e.getMessage());
        }
    }

    private void httpRedirectLoop() {
        while (running) {
            try {
                final Socket client = httpRedirectSocket.accept();
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        handleHttpRedirect(client);
                    }
                });
            } catch (IOException e) {
                if (running) Log.w(TAG, "Redirect accept error: " + e.getMessage());
            }
        }
    }

    private void handleHttpRedirect(Socket socket) {
        try {
            socket.setSoTimeout(10000);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            String requestLine = readLine(in);
            if (requestLine == null) return;
            String path = "/";
            String[] parts = requestLine.split(" ");
            if (parts.length >= 2) path = parts[1];

            String host = socket.getLocalAddress().getHostAddress();
            String header;
            while ((header = readLine(in)) != null && !header.isEmpty()) {
                if (header.toLowerCase().startsWith("host:")) {
                    host = header.substring(5).trim().split(":")[0];
                }
            }

            String location = "https://" + host + ":" + PORT + path;
            String response = "HTTP/1.1 301 Moved Permanently\r\n"
                    + "Location: " + location + "\r\n"
                    + "Content-Length: 0\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception e) {
            Log.w(TAG, "Redirect error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        try {
            if (httpRedirectSocket != null) httpRedirectSocket.close();
        } catch (IOException ignored) {}
        if (threadPool != null) threadPool.shutdownNow();
        validTokens.clear();
        Log.i(TAG, "HTTPS server stopped");
    }

    private void loadHtml(Context context) {
        try {
            InputStream is = context.getAssets().open("webclient.html");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            is.close();
            htmlContent = bos.toString("UTF-8");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load webclient.html: " + e.getMessage());
            htmlContent = "<html><body><h1>Remote Keyboard</h1><p>Error loading client.</p></body></html>";
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                final SSLSocket client = (SSLSocket) serverSocket.accept();
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        handleConnection(client);
                    }
                });
            } catch (IOException e) {
                if (running) Log.w(TAG, "Accept error: " + e.getMessage());
            }
        }
    }

    private void handleConnection(SSLSocket socket) {
        try {
            socket.setSoTimeout(30000);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            String requestLine = readLine(in);
            if (requestLine == null || requestLine.isEmpty()) return;

            int contentLength = 0;
            String header;
            while ((header = readLine(in)) != null && !header.isEmpty()) {
                if (header.toLowerCase().startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(header.substring(15).trim());
                    } catch (NumberFormatException ignored) {}
                }
            }

            byte[] bodyBytes = new byte[0];
            if (contentLength > 0 && contentLength <= 65536) {
                bodyBytes = new byte[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int r = in.read(bodyBytes, read, contentLength - read);
                    if (r < 0) break;
                    read += r;
                }
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;
            String method = parts[0];
            String path = parts[1].split("\\?")[0];
            String body = new String(bodyBytes, StandardCharsets.UTF_8);

            if ("GET".equals(method) && "/".equals(path)) {
                sendResponse(out, 200, "text/html; charset=utf-8", htmlContent);
            } else if ("POST".equals(method) && "/api/auth".equals(path)) {
                handleAuth(out, body);
            } else if ("POST".equals(method) && "/api/key".equals(path)) {
                handleKey(out, body);
            } else if ("OPTIONS".equals(method)) {
                sendResponse(out, 204, "text/plain", "");
            } else {
                sendResponse(out, 404, "application/json", "{\"error\":\"not found\"}");
            }
        } catch (Exception e) {
            Log.w(TAG, "Connection error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleAuth(OutputStream out, String body) throws IOException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RemoteKeyboardService.self);
        String stored = prefs.getString(PREF_PASSWORD, "");
        String provided = extractJsonString(body, "password");

        if (!stored.isEmpty() && !stored.equals(provided)) {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            sendResponse(out, 401, "application/json", "{\"error\":\"wrong password\"}");
            return;
        }

        String token = generateToken();
        validTokens.add(token);
        sendResponse(out, 200, "application/json", "{\"token\":\"" + token + "\"}");
    }

    private void handleKey(OutputStream out, String body) throws IOException {
        String token = extractJsonString(body, "token");
        if (!validTokens.contains(token)) {
            sendResponse(out, 401, "application/json", "{\"error\":\"unauthorized\"}");
            return;
        }
        if (RemoteKeyboardService.self == null) {
            sendResponse(out, 503, "application/json", "{\"error\":\"service not ready\"}");
            return;
        }

        String type = extractJsonString(body, "type");
        String key = extractJsonString(body, "key");
        String text = extractJsonString(body, "text");
        boolean ctrl = body.contains("\"ctrl\":true");

        dispatchKey(type, key, text, ctrl);
        sendResponse(out, 200, "application/json", "{\"ok\":true}");
    }

    private void dispatchKey(String type, String key, String text, boolean ctrl) {
        RemoteKeyboardService service = RemoteKeyboardService.self;
        if (service == null) return;

        TextInputAction tia = new TextInputAction(service);
        CtrlInputAction cia = new CtrlInputAction(service);
        ActionRunner runner = new ActionRunner();

        if (ctrl && key != null && key.length() == 1) {
            Integer code = CTRL_KEY_MAP.get(key.toUpperCase());
            if (code != null) {
                cia.function = code;
                runner.setAction(cia);
                service.handler.post(runner);
                runner.waitResult();
            }
            return;
        }

        if ("printable".equals(type) && text != null && !text.isEmpty()) {
            tia.text = text;
            runner.setAction(tia);
            service.handler.post(runner);
            runner.waitResult();
            return;
        }

        Integer code = SPECIAL_KEY_MAP.get(key);
        if (code != null) {
            cia.function = code;
            runner.setAction(cia);
            service.handler.post(runner);
            runner.waitResult();
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(TOKEN_BYTES * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf('"', start);
        if (end < 0) return "";
        return json.substring(start, end).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\r') {
                in.read();
                return sb.toString();
            }
            if (c == '\n') return sb.toString();
            sb.append((char) c);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private static void sendResponse(OutputStream out, int code, String contentType, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String status;
        switch (code) {
            case 200: status = "OK"; break;
            case 204: status = "No Content"; break;
            case 401: status = "Unauthorized"; break;
            case 404: status = "Not Found"; break;
            case 503: status = "Service Unavailable"; break;
            default:  status = "Error";
        }
        String headers = "HTTP/1.1 " + code + " " + status + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + bodyBytes.length + "\r\n"
                + "Connection: close\r\n"
                + "Access-Control-Allow-Origin: *\r\n"
                + "Access-Control-Allow-Headers: Content-Type\r\n"
                + "\r\n";
        out.write(headers.getBytes(StandardCharsets.UTF_8));
        if (bodyBytes.length > 0) out.write(bodyBytes);
        out.flush();
    }
}
