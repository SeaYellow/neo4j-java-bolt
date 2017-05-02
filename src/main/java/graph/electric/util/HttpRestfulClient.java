package graph.electric.util;

import cn.dreampie.client.HttpClientRequest;
import cn.dreampie.client.HttpClientResult;
import cn.dreampie.common.http.ContentType;
import cn.dreampie.common.http.Encoding;
import cn.dreampie.log.Logger;


/**
 * Created by Administrator on 2017/4/3.
 */
public class HttpRestfulClient {
    private static final Logger logger = Logger.getLogger(HttpRestfulClient.class);

    public static String sendRequest(String url, String jsonParam) {
        long timeMillis = System.currentTimeMillis();
        HttpClientRequest request = new HttpClientRequest();
        request.setJsonParam(jsonParam);
        request.setEncoding(Encoding.UTF_8.name());
        request.setContentType(ContentType.JSON.value());
        cn.dreampie.client.HttpClient httpClient = new cn.dreampie.client.HttpClient(url).build(request);
        HttpClientResult clientResult = httpClient.post();
        String result = clientResult.getResult();
        System.out.println(result);
        logger.info("Request time : " + (System.currentTimeMillis() - timeMillis) / 1000 + " seconds.");
        return result;
    }
}
