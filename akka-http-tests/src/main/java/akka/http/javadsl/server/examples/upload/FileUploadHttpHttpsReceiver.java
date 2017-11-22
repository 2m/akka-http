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
import akka.http.javadsl.server.directives.FileInfo;
import akka.http.javadsl.server.examples.simple.SimpleServerApp;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

public class FileUploadHttpHttpsReceiver extends AllDirectives {

  final Function<FileInfo, File> fileDestination = (info) -> {
    try {
      return File.createTempFile(info.getFileName(), ".tmp");
    } catch (Exception e) {
      return null;
    }
  };

  public Route createRoute() {
    return storeUploadedFile("log", fileDestination, (info, file) -> {
      // do something with the file and file metadata ...
      System.out.println("Uploaded " + file.getAbsolutePath());
      file.delete();
      return complete(StatusCodes.OK);
    });
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
