package graph.movies.backend;

import graph.movies.util.Util;

import static spark.Spark.externalStaticFileLocation;
import static spark.Spark.port;

/**
 * @author Michael Hunger @since 22.10.13
 */
public class MovieServer {

    public static void main(String[] args) {
        port(Util.getWebPort());
        externalStaticFileLocation("src/main/webapp");
        final MovieService service = new MovieService(Util.getNeo4jUrl());
        new MovieRoutes(service).init();
    }
}
