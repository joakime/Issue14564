package org.example.jetty;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class Niloc132Main
{
    public static void main(String[] args) throws Exception
    {
        int port = 8080;
        Server server = new Server();

        final HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory();

        final ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(h2.getProtocol());

        final SslConnectionFactory tls = new SslConnectionFactory(alpn.getProtocol());
        final SslContextFactory sslContextFactory = tls.getSslContextFactory();

        final Path keystorePath = findTlsPath("keystore.jks");

        sslContextFactory.setKeyStorePath(keystorePath);
        sslContextFactory.setKeyStoreType("JKS");
        sslContextFactory.setKeyStorePassword("changeit");

        ServerConnector connector = new ServerConnector(server, tls, alpn, h2);
        connector.setPort(port);
        server.addConnector(connector);

//        ResourceHandler ctx = new ResourceHandler();
        final WebAppContext ctx = new WebAppContext("/", null, null, null, null, ServletContextHandler.NO_SESSIONS);
        final ResourceFactory resourceFactory = ctx.getResourceFactory();
        final Resource resource = resourceFactory.newClassLoaderResource("/webapp-root/");
        System.out.println(resource);
        ctx.setBaseResource(resource);

        server.setHandler(ctx);

        server.start();
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
