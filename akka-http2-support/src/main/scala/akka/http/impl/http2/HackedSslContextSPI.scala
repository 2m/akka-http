
/*package javax.net.ssl {

  import java.security.SecureRandom
  import javax.net.ssl.KeyManager
  import javax.net.ssl.SSLContextSpi
  import javax.net.ssl.SSLEngine
  import javax.net.ssl.SSLServerSocketFactory
  import javax.net.ssl.SSLSessionContext
  import javax.net.ssl.SSLSocketFactory
  import javax.net.ssl.TrustManager

  class HackedSslContextSPI(underlying: SSLContextSpi) extends SSLContextSpi {
    def engineGetSocketFactory(): SSLSocketFactory =
      underlying.engineGetSocketFactory()

    def engineInit(keyManagers: Array[KeyManager], trustManagers: Array[TrustManager], secureRandom: SecureRandom): Unit =
      underlying.engineInit(keyManagers, trustManagers, secureRandom)

    def engineCreateSSLEngine(): SSLEngine =
      underlying.engineCreateSSLEngine()

    def engineCreateSSLEngine(s: String, i: Int): SSLEngine =
      underlying.engineCreateSSLEngine(s, i)

    def engineGetClientSessionContext(): SSLSessionContext =
      underlying.engineGetClientSessionContext()

    def engineGetServerSessionContext(): SSLSessionContext =
      underlying.engineGetServerSessionContext()

    def engineGetServerSocketFactory(): SSLServerSocketFactory =
      underlying.engineGetServerSocketFactory()
  }

}*/

package akka.http.impl.http2 {
  import javax.net.ssl.SSLContext
  import javax.net.ssl.SSLContextSpi
  import javax.net.ssl.WrappedSslContextSPI

  import akka.http.scaladsl.HttpsConnectionContext

  object HackedSslContextSPI {
    val field = {
      val field = classOf[SSLContext].getDeclaredField("contextSpi")
      field.setAccessible(true)
      field
    }

    def wrapContext(context: HttpsConnectionContext): HttpsConnectionContext = {
      val newContext = SSLContext.getInstance("TLS")
      field.set(newContext, new WrappedSslContextSPI(context.sslContext))

      new HttpsConnectionContext(
        newContext,
        context.sslConfig,
        context.enabledCipherSuites,
        context.enabledProtocols,
        context.clientAuth,
        context.sslParameters
      )
    }
  }

}