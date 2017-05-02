package graph.electric.backend;


import graph.electric.util.Util;

import static spark.Spark.externalStaticFileLocation;
import static spark.Spark.port;

/**
 * Created by Administrator on 2017/4/15.
 */
public class ElectricServer {
    public static void main(String[] args) {
        port(Util.getWebPort());
        String path = "E:\\graph\\neo4j\\neo4j-movies-java-bolt-master\\src\\main\\webapp\\";
//        String path = "/home/huanghai/graph_demo/0428/webapp/";
        externalStaticFileLocation(path);
        final ElectricService service = new ElectricService(Util.getNeo4jUrl());
        new ElectricRoutes(service).init();
    }
}
