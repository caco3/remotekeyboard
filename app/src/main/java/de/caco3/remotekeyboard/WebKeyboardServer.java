package de.caco3.remotekeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class WebKeyboardServer {

    public static final String TAG = "WebKeyboardServer";
    public static final String PREF_PASSWORD = "pref_password";
    public static final int PORT = 4430;

    private static final int TOKEN_BYTES = 32;

    private ServerSocket serverSocket;
    private SSLSocketFactory sslSocketFactory;
    private ExecutorService threadPool;
    private volatile boolean running = false;
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

        SSLContext sslCtx = SslHelper.getSslContext();
        if (sslCtx == null) {
            Log.e(TAG, "TLS not available on this device (requires API 23+)");
            return;
        }
        sslSocketFactory = sslCtx.getSocketFactory();
        serverSocket = new ServerSocket(PORT);
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
        Log.i(TAG, "Server started on port " + PORT + " (HTTPS + plain-HTTP redirect)");
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        if (threadPool != null) threadPool.shutdownNow();
        validTokens.clear();
        Log.i(TAG, "Server stopped");
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
                final Socket client = serverSocket.accept();
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        dispatchSocket(client);
                    }
                });
            } catch (IOException e) {
                if (running) Log.w(TAG, "Accept error: " + e.getMessage());
            }
        }
    }

    /**
     * Peek the first byte to decide whether the client is speaking TLS
     * (handshake record starts with 0x16) or plain HTTP (ASCII verb).
     * In the latter case, send a 301 redirect to the same URL on https://.
     */
    private void dispatchSocket(Socket socket) {
        try {
            socket.setSoTimeout(30000);
            BufferedInputStream peek = new BufferedInputStream(socket.getInputStream(), 1);
            peek.mark(1);
            int first = peek.read();
            if (first == -1) {
                socket.close();
                return;
            }
            peek.reset();

            if (first == 0x16) {
                /* TLS handshake — wrap the socket so that SSL reads our peeked
                 * byte first, then continues from the real socket. Android's
                 * SSLSocketFactory does not implement createSocket(Socket,
                 * InputStream, boolean), so we use createSocket(Socket, String,
                 * int, boolean) on a Socket subclass that injects the peeked
                 * stream via getInputStream(). */
                Socket wrapped = new PeekedSocket(socket, peek);
                SSLSocket tls = (SSLSocket) sslSocketFactory.createSocket(
                        wrapped, socket.getInetAddress().getHostAddress(),
                        socket.getPort(), true);
                tls.setUseClientMode(false);
                tls.setNeedClientAuth(false);
                handleConnection(tls);
            } else {
                /* Plain HTTP — redirect to https:// on the same port */
                handleHttpRedirect(socket, peek);
            }
        } catch (Exception e) {
            Log.w(TAG, "Dispatch error: " + e.getClass().getName() + ": " + e.getMessage(), e);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleHttpRedirect(Socket socket, InputStream in) {
        try {
            OutputStream out = socket.getOutputStream();
            String requestLine = readLine(in);
            String path = "/";
            if (requestLine != null) {
                String[] parts = requestLine.split(" ");
                if (parts.length >= 2) path = parts[1];
            }
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
            Log.i(TAG, "Redirected plain-HTTP request to " + location);
        } catch (Exception e) {
            Log.w(TAG, "Redirect error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
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

    /**
     * Socket subclass that wraps an existing connected Socket but exposes a
     * caller-supplied InputStream from {@link #getInputStream()}. This lets us
     * hand a "peeked" stream to the SSL layer so the very first byte of the
     * TLS handshake (already consumed during protocol sniffing) is replayed.
     * All other operations delegate to the inner socket.
     */
    private static final class PeekedSocket extends Socket {
        private final Socket inner;
        private final InputStream in;

        PeekedSocket(Socket inner, InputStream in) throws SocketException {
            super();
            this.inner = inner;
            this.in = in;
        }
        @Override public InputStream getInputStream() { return in; }
        @Override public OutputStream getOutputStream() throws IOException { return inner.getOutputStream(); }
        @Override public synchronized void close() throws IOException { inner.close(); }
        @Override public InetAddress getInetAddress() { return inner.getInetAddress(); }
        @Override public InetAddress getLocalAddress() { return inner.getLocalAddress(); }
        @Override public int getPort() { return inner.getPort(); }
        @Override public int getLocalPort() { return inner.getLocalPort(); }
        @Override public SocketAddress getRemoteSocketAddress() { return inner.getRemoteSocketAddress(); }
        @Override public SocketAddress getLocalSocketAddress() { return inner.getLocalSocketAddress(); }
        @Override public boolean isConnected() { return inner.isConnected(); }
        @Override public boolean isBound() { return inner.isBound(); }
        @Override public boolean isClosed() { return inner.isClosed(); }
        @Override public synchronized void setSoTimeout(int t) throws SocketException { inner.setSoTimeout(t); }
        @Override public synchronized int getSoTimeout() throws SocketException { return inner.getSoTimeout(); }
        @Override public void setTcpNoDelay(boolean on) throws SocketException { inner.setTcpNoDelay(on); }
        @Override public boolean getTcpNoDelay() throws SocketException { return inner.getTcpNoDelay(); }
        @Override public void setKeepAlive(boolean on) throws SocketException { inner.setKeepAlive(on); }
        @Override public boolean getKeepAlive() throws SocketException { return inner.getKeepAlive(); }
        @Override public synchronized void setReceiveBufferSize(int size) throws SocketException { inner.setReceiveBufferSize(size); }
        @Override public synchronized int getReceiveBufferSize() throws SocketException { return inner.getReceiveBufferSize(); }
        @Override public synchronized void setSendBufferSize(int size) throws SocketException { inner.setSendBufferSize(size); }
        @Override public synchronized int getSendBufferSize() throws SocketException { return inner.getSendBufferSize(); }
        @Override public void setReuseAddress(boolean on) throws SocketException { inner.setReuseAddress(on); }
        @Override public boolean getReuseAddress() throws SocketException { return inner.getReuseAddress(); }
        @Override public void shutdownInput() throws IOException { inner.shutdownInput(); }
        @Override public void shutdownOutput() throws IOException { inner.shutdownOutput(); }
        @Override public boolean isInputShutdown() { return inner.isInputShutdown(); }
        @Override public boolean isOutputShutdown() { return inner.isOutputShutdown(); }
    }
}
