package org.example.jetty;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class MinimalReproduction
{
    public static void main(String[] args) throws Exception
    {
        Server server = new Server();

        Path keystorePath = findTlsPath("keystore.jks");
        Path truststorePath = findTlsPath("truststore.jks");

        SslContextFactory.Server ssl = new SslContextFactory.Server();
        ssl.setKeyStorePath(keystorePath.toString());
        ssl.setKeyStoreType("JKS"); // default is PKCS12
        ssl.setKeyStorePassword("changeit");
        ssl.setTrustStorePath(truststorePath.toString());
        ssl.setTrustStoreType("JKS"); // default is PKCS12
        ssl.setTrustStorePassword("changeit");

        HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory();
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol("http/1.1");

        ServerConnector connector = new ServerConnector(server,
            new SslConnectionFactory(ssl, alpn.getProtocol()),
            alpn, http2, new HttpConnectionFactory());
        connector.setPort(8443);
        server.addConnector(connector);

        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream in = Files.newInputStream(keystorePath))
        {
            ks.load(in, "changeit".toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "changeit".toCharArray());

        KeyStore ts = KeyStore.getInstance("JKS");
        try (InputStream in = Files.newInputStream(truststorePath))
        {
            ts.load(in, "changeit".toCharArray());
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        HttpClient client = HttpClient.newBuilder().sslContext(ctx).build();

        server.setHandler(
            new Handler.Abstract()
            {
                @Override
                public boolean handle(Request request, Response response, Callback callback) throws Exception
                {
                    switch (request.getHttpURI().getPath())
                    {
                        case "/external" ->
                        {
                            System.out.println("External request - making internal call");

                            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("https://localhost:8443/internal")).GET().build();
                            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                            response.setStatus(200);
                            Content.Sink.write(response, true, "Success: " + resp.body(), callback);
                        }
                        case "/internal" ->
                        {
                            response.setStatus(200);
                            Content.Sink.write(response, true, "OK", callback);
                        }
                        default ->
                        {
                            response.setStatus(404);
                            callback.succeeded();
                        }
                    }
                    return true;
                }
            });

        server.start();
        System.out.println("Server started on " + server.getURI());
        server.join();
    }

    private static Path findTlsPath(String filename) throws FileNotFoundException
    {
        Path path = Path.of("tls/" + filename);
        if (Files.isRegularFile(path))
            return path.toAbsolutePath();
        throw new FileNotFoundException("Unable to find file: " + filename);
    }
}
