package akka.http.javadsl.server.examples.upload;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.HttpsConnectionContext;
import akka.http.javadsl.model.*;
import akka.http.javadsl.server.examples.simple.SimpleServerApp;
import akka.stream.ActorMaterializer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUploadHttpHttpsSender {

  public static RequestEntity createFileUploadEntityFromFile(Path filePath) {
    return Multiparts.createFormDataFromParts(
      Multiparts.createFormDataPartFromPath("log", ContentTypes.APPLICATION_OCTET_STREAM, filePath, 100000)
    ).toEntity();
  }

  public static HttpRequest createRequest(String url) {
    return HttpRequest.POST(url).withEntity(createFileUploadEntityFromFile(Paths.get("akka-http-tests/src/main/resources/httpsDemoKeys/keys/README.md")));
  }

  public static void main(String[] args) throws IOException {
    final ActorSystem system = ActorSystem.create("FileUploadHttpHttpsSender");

    final Http http = Http.get(system);

    if (args[0].equals("http")) {
    //Run HTTP client
      http.singleRequest(createRequest("http://akka.example.org:80/")).thenAccept((response) -> {
        System.out.println("File HTTP upload response " + response);
      }).exceptionally((error) -> {
        System.out.println("File HTTP upload error " + error);
        return null;
      });
    }
    else if (args[0].equals("https")) {
      //get configured HTTPS context
      HttpsConnectionContext https = SimpleServerApp.useHttps(system);

      // sets default context to HTTPS – all clients for this ActorSystem will use HTTPS from now on
      http.setDefaultClientHttpsContext(https);

      //Then run HTTPS server
      http.singleRequest(createRequest("https://akka.example.org:443/")).thenAccept((response) -> {
        System.out.println("File HTTPS upload response " + response);
      }).exceptionally((error) -> {
        System.out.println("File HTTPS upload error " + error);
        return null;
      });
    }

    System.out.println("Type RETURN to exit");
    System.in.read();
    system.terminate();
  }

}
