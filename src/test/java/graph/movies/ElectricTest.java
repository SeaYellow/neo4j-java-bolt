//package example.movies;
//
//import com.alibaba.fastjson.JSONObject;
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import example.electric.backend.ElectricService;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.util.List;
//
///**
// * Created by Administrator on 2017/4/15.
// */
//public class ElectricTest {
//    ElectricService service = null;
//
//    @Before
//    public void init() {
//        String DEFAULT_URL = "bolt://neo4j:merit@61.185.224.85";
//        service = new ElectricService(DEFAULT_URL);
//    }
//
//    @Test
//    public void findRelElectricById() {
//        JSONObject jsonObject = service.findRelElectricById("47", "0");
//        System.out.println(jsonObject.toString());
//    }
//
//    @Test
//    public void queryElectricByCypher() {
//        JSONObject jsonObject = service.queryElectricByCypher("MATCH(N)-[R]-(M) RETURN N,R,M LIMIT 2");
//        System.out.println(jsonObject.toString());
//    }
//
//    @Test
//    public void queryElectricByName() {
//        long l = System.currentTimeMillis();
//        String cql = "MATCH(S:SUBS)-[R]-(L:LINES)-[R1]-(T:TRAN) WHERE TRIM(S" +
//                ".SUBS_NAME) =~ '.*铜川变电站.*' RETURN S,R,L,R1,T\n" +
//                "UNION\n" +
//                "MATCH(S:PMS_SUBS)-[R]-(L:PMS_LINES)-[R1]-(T:PMS_TRAN) WHERE TRIM(S.BDZMC) =~ '.*铜川变电站.*' RETURN S,R," +
//                "L,R1," +
//                "T";
//        JSONObject jsonObject = service.queryElectricByCypher(cql);
//        System.out.println((System.currentTimeMillis() - l) + " ms");
//        System.out.println(jsonObject.toString());
//    }
//}
