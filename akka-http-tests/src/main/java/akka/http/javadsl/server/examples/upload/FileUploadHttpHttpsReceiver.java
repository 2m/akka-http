package akka.http.javadsl.server.examples.upload;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.HttpsConnectionContext;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.server.examples.simple.SimpleServerApp;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.scaladsl.Compression;

import java.io.File;
import java.io.IOException;

public class FileUploadHttpHttpsReceiver extends AllDirectives {

  public Route createRoute() {
    return extractMaterializer((mat) ->
      entity(Unmarshaller.entityToMultipartFormData(), (formData) ->
        completeWithFutureStatus(
          formData.getParts().mapAsync(1, (part) ->
            part.getEntity().getDataBytes().via(Compression.gunzip(64 * 1024)).runWith(FileIO.toFile(File.createTempFile(part.getName(), ".tmp")), mat).thenApply((res) ->
              Pair.create(part.getName(), res)
            )
          ).runWith(Sink.foreach((res) ->
            System.out.println("File [" + res.first() + "] saved. Total bytes: [" + res.second().getCount() + "]")
          ), mat).thenApply((done) ->
            StatusCodes.OK
          )
        )
      )
    );
  }

  public static void main(String[] args) throws IOException {
    final ActorSystem system = ActorSystem.create("FileUploadHttpHttpsReceiver");
    final ActorMaterializer materializer = ActorMaterializer.create(system);

    final FileUploadHttpHttpsReceiver app = new FileUploadHttpHttpsReceiver();
    final Flow<HttpRequest, HttpResponse, NotUsed> flow = app.createRoute().flow(system, materializer);

    final Http http = Http.get(system);
    //Run HTTP server firstly
    http.bindAndHandle(flow, ConnectHttp.toHost("akka.example.org", 80), materializer);

    //get configured HTTPS context
    HttpsConnectionContext https = SimpleServerApp.useHttps(system);

    // sets default context to HTTPS – all Http() bound servers for this ActorSystem will use HTTPS from now on
    http.setDefaultServerHttpContext(https);

    //Then run HTTPS server
    http.bindAndHandle(flow, ConnectHttp.toHost("akka.example.org", 443), materializer);
    //#both-https-and-http

    System.out.println("Type RETURN to exit");
    System.in.read();
    system.terminate();
  }

}
