package javax.net.ssl;

import org.eclipse.jetty.alpn.ALPN;

import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

public class WrappedSslContextSPI extends SSLContextSpi {
    private SSLContext underlying;

    public WrappedSslContextSPI(SSLContext underlying) {
        this.underlying = underlying;
    }

    @Override
    protected void engineInit(KeyManager[] keyManagers, TrustManager[] trustManagers, SecureRandom secureRandom) throws KeyManagementException {
        underlying.init(keyManagers, trustManagers, secureRandom);
    }

    @Override
    protected SSLSocketFactory engineGetSocketFactory() {
        return underlying.getSocketFactory();
    }

    @Override
    protected SSLServerSocketFactory engineGetServerSocketFactory() {
        return underlying.getServerSocketFactory();
    }

    @Override
    protected SSLEngine engineCreateSSLEngine() {
        System.out.println("New engine was created!");
        SSLEngine result = underlying.createSSLEngine();
        ALPN.put(result, new ALPN.ServerProvider() {
            @Override
            public void unsupported() {
                System.out.println("ALPN not supported by client!");
            }

            @Override
            public String select(List<String> protocols) throws SSLException {
                if (protocols.contains("h2")) {
                    System.out.println("HTTP/2 is supported!");
                }

                return "h2";
            }
        });
        return result;
    }

    @Override
    protected SSLEngine engineCreateSSLEngine(String s, int i) {
        return underlying.createSSLEngine(s, i);
    }

    @Override
    protected SSLSessionContext engineGetServerSessionContext() {
        return underlying.getServerSessionContext();
    }

    @Override
    protected SSLSessionContext engineGetClientSessionContext() {
        return underlying.getClientSessionContext();
    }
}
