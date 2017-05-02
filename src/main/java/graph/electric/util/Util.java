package graph.electric.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Michael Hunger @since 22.10.13
 */
public class Util {
    public static final String DEFAULT_URL = "bolt://neo4j:merit@61.185.224.85";
    public static final String WEBAPP_LOCATION = "src/main/webapp/";
    public static final String DOWNLOAD_LOCATION = "/src/main/download/";
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh_mm_ss_SSS");

    public static int getWebPort() {
        String webPort = System.getenv("PORT");
        if (webPort == null || webPort.isEmpty()) {
            return 8088;
        }
        return Integer.parseInt(webPort);
    }

    public static String getNeo4jUrl() {
        String urlVar = System.getenv("NEO4J_URL_VAR");
        if (urlVar == null) urlVar = "NEO4J_URL";
        String url = System.getenv(urlVar);
        if (url == null || url.isEmpty()) {
            return DEFAULT_URL;
        }
        return url;
    }

    public static String toUTF8(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0 && c <= 255) {
                sb.append(c);
            } else {
                byte[] b;
                try {
                    b = Character.toString(c).getBytes("utf-8");
                } catch (Exception ex) {
                    System.out.println(ex);
                    b = new byte[0];
                }
                for (int j = 0; j < b.length; j++) {
                    int k = b[j];
                    if (k < 0)
                        k += 256;
                    sb.append("%" + Integer.toHexString(k).toUpperCase());
                }
            }
        }
        return sb.toString();
    }


    public static File getDownloadCsvPath(String type) {
        String proDir = System.getProperty("user.dir");
        if ("yx".equals(type)) {
            return new File(proDir + DOWNLOAD_LOCATION + "营销_" + sdf.format(new Date(System.currentTimeMillis())) + "" +
                    ".csv");
        } else if ("sc".equals(type)) {
            return new File(proDir + DOWNLOAD_LOCATION + "生产_" + sdf.format(new Date(System.currentTimeMillis())) + "" +
                    ".csv");
        }
        return null;
    }
}
