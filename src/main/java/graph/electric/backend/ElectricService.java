package graph.electric.backend;

import com.alibaba.fastjson.JSON;
import graph.electric.DiffModel;
import graph.electric.executor.BoltCypherExecutor;
import graph.electric.executor.CypherExecutor;
import graph.electric.util.ContentUtil;
import graph.electric.util.HttpRestfulClient;
import graph.electric.util.Util;
import org.apache.commons.io.Charsets;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Relationship;
import org.neo4j.helpers.collection.Iterators;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * Created by Administrator on 2017/4/15.
 */
public class ElectricService {
    private final CypherExecutor cypher;

    public ElectricService(String uri) {
        cypher = createCypherExecutor(uri);
    }

    private CypherExecutor createCypherExecutor(String uri) {
        try {
            String auth = new URL(uri.replace("bolt", "http")).getUserInfo();
            if (auth != null) {
                String[] parts = auth.split(":");
                return new BoltCypherExecutor(uri, parts[0], parts[1]);
            }
            return new BoltCypherExecutor(uri);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Neo4j-ServerURL " + uri);
        }
    }

    public Map findElectricById(String id) {
        if (id == null || id == "") return Collections.emptyMap();
        Iterator<Map<String, Object>> query = cypher.query(
                "MATCH(N) WHERE ID(N) = {id} RETURN N",
                map("id", Integer.parseInt(id)));
        return Iterators.singleOrNull(query);
    }

    /**
     * 查询相邻节点和关系
     *
     * @param id
     * @return
     */
    public JSONObject findRelElectricById(String id, String excIds) {
        JSONObject resJson = new JSONObject();
        if (id == null || id == "" || excIds == null) {
            resJson.put("vData", "[]");
            resJson.put("eData", "[]");
            return resJson;
        }
        StatementResult res = cypher.queryResult(
                "MATCH(N)-[R]-(M) WHERE ID(N) = {id} RETURN N,R,M",
                map("id", Integer.parseInt(id)));
        JSONArray vArray = new JSONArray();
        JSONArray eArray = new JSONArray();
        while (res.hasNext()) {
            Record record = res.next();
            // 封装前端展示JSON
            // 顶点
            Node nNode = record.get("N").asNode();
            Node mNode = record.get("M").asNode();
            vArray.add(vertexPush(nNode, null, null));
            vArray.add(vertexPush(mNode, null, null));
            // 边
            Relationship r = record.get("R").asRelationship();
            eArray.add(edgePush(r));
        }
        resJson.put("vData", vArray.toString());
        resJson.put("eData", eArray.toString());
        return resJson;
    }

    public JSONObject querySubsByName(String name) {
        JSONObject resJson = new JSONObject();
        if (name == null) {
            resJson.put("vData", "[]");
            resJson.put("eData", "[]");
            return resJson;
        }
        return resJson;
    }

    public JSONObject queryAllSubs(String cql) {
        System.out.println("CQL : " + cql);
        JSONObject resJson = new JSONObject();

        JSONObject yxJson = new JSONObject();
        JSONObject scJson = new JSONObject();
        resJson.put("yxData", yxJson);
        resJson.put("scData", scJson);

        StatementResult res = cypher.queryResult(cql, null);
        // 营销
        Set<Node> yx_nodeSet = new HashSet<>();
        Set<Relationship> yx_edgeSet = new HashSet<>();
        // 生产
        Set<Node> sc_nodeSet = new HashSet<>();
        Set<Relationship> sc_edgeSet = new HashSet<>();

        while (res.hasNext()) {
            Record record = res.next();
            Node sNode = record.get("S").asNode();
            String label = sNode.labels().iterator().next();
            if ("SUBS".equals(label)) {
                yx_nodeSet.add(sNode);
            } else if ("PMS_SUBS".equals(label)) {
                sc_nodeSet.add(sNode);
            }
        }

        nodeEdgePush(yx_nodeSet, yx_edgeSet, yxJson);
        nodeEdgePush(sc_nodeSet, sc_edgeSet, scJson);

        return resJson;
    }

    /**
     * 根据cql查询
     *
     * @param cql
     * @return
     */
    public JSONObject queryElectricByCypher(String cql) {
        System.out.println("CQL : " + cql);
        JSONObject resJson = new JSONObject();

        JSONObject yxJson = new JSONObject();
        JSONObject scJson = new JSONObject();
        resJson.put("yxData", yxJson);
        resJson.put("scData", scJson);

        StatementResult res = cypher.queryResult(cql, null);
        // 营销
        Set<Node> yx_nodeSet = new HashSet<>();
        Set<Relationship> yx_edgeSet = new HashSet<>();
        // 生产
        Set<Node> sc_nodeSet = new HashSet<>();
        Set<Relationship> sc_edgeSet = new HashSet<>();

        while (res.hasNext()) {
            Record record = res.next();
            Node sNode = record.get("S").asNode();
            String label = sNode.labels().iterator().next();
            if ("SUBS".equals(label)) {
                recordPush(yx_nodeSet, yx_edgeSet, record, sNode);
            } else if ("PMS_SUBS".equals(label)) {
                recordPush(sc_nodeSet, sc_edgeSet, record, sNode);
            }
        }

        nodeEdgePush(yx_nodeSet, yx_edgeSet, yxJson);
        nodeEdgePush(sc_nodeSet, sc_edgeSet, scJson);

        return resJson;
    }

    private void nodeEdgePush(Set<Node> nodeSet, Set<Relationship>
            edgeSet, JSONObject json) {
        JSONArray vArray = new JSONArray();
        JSONArray eArray = new JSONArray();
        for (Node n : nodeSet) {
            // 顶点
            vArray.add(vertexPush(n, null, null));
        }
        for (Relationship r : edgeSet) {
            // 边
            eArray.add(edgePush(r));
        }

        json.put("vData", vArray.toString());
        json.put("eData", eArray.toString());
    }

    private void recordPush(Set<Node> nodeSet, Set<Relationship> edgeSet, Record record, Node sNode) {
        Node lNode = record.get("N").asNode();
        nodeSet.add(sNode);
        nodeSet.add(lNode);
        // 边
        List<Object> objectList = record.get("R").asList();
        for (Object r : objectList) {
            edgeSet.add((Relationship) r);
        }
    }

    private JSONObject edgePush(Relationship r) {
        JSONObject ejson = new JSONObject();
        ejson.put("id", r.id());
        ejson.put("from", r.startNodeId());
        ejson.put("to", r.endNodeId());
//        ejson.put("arrows", "to");
        return ejson;
    }

    private JSONObject vertexPush(Node node, Integer type, List<JSONArray> sameArray) {
        JSONObject vjson = new JSONObject();
        vjson.put("id", node.id());
        vjson.put("type", type);
        String label_m = node.labels().iterator().next().toString();
        if (type == null) {
            vjson.put("group", label_m);
        } else {
            vjson.put("group", "DIFF");
        }
        if (sameArray != null) {
            vjson.put("sameId", getSameId(sameArray, node.id()));
        } else {
            vjson.put("sameId", "");
        }
        switch (label_m) {
            case "SUBS":
                vjson.put("label", node.get("SUBS_NAME").asString().trim());
                break;
            case "LINES":
                vjson.put("label", node.get("LINE_NAME").asString().trim());
                break;
            case "TRAN":
                vjson.put("label", node.get("TRAN_NAME").asString().trim());
                break;
            case "PMS_SUBS":
                vjson.put("label", node.get("BDZMC").asString().trim());
                break;
            case "PMS_LINES":
                vjson.put("label", node.get("XLMC").asString().trim());
                break;
            case "PMS_TRAN":
                vjson.put("label", node.get("SBMC").asString().trim());
                break;
        }
        return vjson;
    }

    /**
     * 差异分析
     *
     * @param cql
     * @return
     */
    public JSONObject difYxScAnalysis(String cql) {
        System.out.println("Diff Analysis YX SC CQL : " + cql);
        JSONObject resJson = new JSONObject();

        JSONObject yxJson = new JSONObject();
        JSONObject scJson = new JSONObject();
        resJson.put("yxData", yxJson);
        resJson.put("scData", scJson);

        StatementResult res = cypher.queryResult(cql, null);
        // 营销
        Set<Node> yx_nodeSet = new HashSet<>();
        Set<Relationship> yx_edgeSet = new HashSet<>();
        // 生产
        Set<Node> sc_nodeSet = new HashSet<>();
        Set<Relationship> sc_edgeSet = new HashSet<>();

        while (res.hasNext()) {
            Record record = res.next();
            Node sNode = record.get("S").asNode();
            String label = sNode.labels().iterator().next();
            if ("SUBS".equals(label)) {
                recordPush(yx_nodeSet, yx_edgeSet, record, sNode);
            } else {
                recordPush(sc_nodeSet, sc_edgeSet, record, sNode);
            }
        }
        Set<Node> nodeSet = new HashSet<>();
        nodeSet.addAll(yx_nodeSet);
        nodeSet.addAll(sc_nodeSet);
        JSONObject diffNodes = getDiffNodes(nodeSet, yx_edgeSet, sc_edgeSet);

        JSONArray id_tuple = diffNodes.getJSONArray("id_tuple");
        JSONArray diff_type = diffNodes.getJSONArray("diff_type");
        List<Integer> types = diff_type.toJavaList(Integer.class);
        Iterator<Object> iterator = id_tuple.iterator();
        List<DiffModel> dms = new ArrayList<>();
        List<JSONArray> sameArray = new ArrayList<>();
        int i = 0;
        while (iterator.hasNext()) {
            Object nextObj = iterator.next();
            if (nextObj instanceof Integer) {
                DiffModel dm = new DiffModel((int) nextObj, types.get(i));
                dms.add(dm);
            } else if (nextObj instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) nextObj;
                if (types.get(i) == 0 || types.get(i) == 6) {
                    sameArray.add(jsonArray);
                } else {
                    Iterator<Object> iterator1 = jsonArray.iterator();
                    while (iterator1.hasNext()) {
                        DiffModel dm = new DiffModel((int) iterator1.next(), types.get(i));
                        dms.add(dm);
                    }
                }
            }
            ++i;
        }

        // 设置差异
        JSONArray yxNodeArray = setDiffModelGroup(yx_nodeSet, dms, sameArray);
        JSONArray scNodeArray = setDiffModelGroup(sc_nodeSet, dms, sameArray);

        JSONArray yxEdgeArray = new JSONArray();
        for (Relationship r : yx_edgeSet) {
            yxEdgeArray.add(edgePush(r));
        }
        yxJson.put("vData", yxNodeArray.toString());
        yxJson.put("eData", yxEdgeArray.toString());

        JSONArray scEdgeArray = new JSONArray();
        for (Relationship r : sc_edgeSet) {
            scEdgeArray.add(edgePush(r));
        }

        scJson.put("vData", scNodeArray.toString());
        scJson.put("eData", scEdgeArray.toString());
        return resJson;
    }

    private JSONArray setDiffModelGroup(Set<Node> yx_nodeSet, List<DiffModel> dms, List<JSONArray> sameArray) {
        JSONArray vArray = new JSONArray();
        for (Node n : yx_nodeSet) {
            boolean pushFlag = false;
            for (int i = 0; i < dms.size(); i++) {
                DiffModel diffModel = dms.get(i);
                if (n.id() == diffModel.getId()) {
                    vArray.add(vertexPush(n, diffModel.getDiffGroup(), null));
                    pushFlag = true;
                }
            }
            if (!pushFlag) {
                vArray.add(vertexPush(n, null, sameArray));
            }
        }
        return vArray;
    }

    /**
     * 封装差异请求参数
     *
     * @param nodeSet
     * @return
     */
    private JSONObject getDiffNodes(Set<Node> nodeSet, Set<Relationship> yx_edgeSet, Set<Relationship> sc_edgeSet) {
        // 变电站
        JSONObject subsJson = new JSONObject();
        subsJson.put("type", "subs");
        JSONObject subsData = new JSONObject();
        JSONArray subsIdArray = new JSONArray();
        subsData.put("ID", subsIdArray);
        JSONArray subsNameArray = new JSONArray();
        subsData.put("NAME", subsNameArray);
        JSONArray subsVoltArray = new JSONArray();
        subsData.put("VOLT", subsVoltArray);
        JSONArray subsAddrArray = new JSONArray();
        subsData.put("ADDR", subsAddrArray);
        JSONArray subsPmsIdrArray = new JSONArray();
        subsData.put("PMSID", subsPmsIdrArray);
        JSONArray subsParentIdrArray = new JSONArray();
        subsData.put("PARENTID", subsParentIdrArray);
        JSONArray subsSourceIdrArray = new JSONArray();
        subsData.put("SOURCE", subsSourceIdrArray);
        subsJson.put("data", subsData);
        // 线路
        JSONObject linesJson = new JSONObject();
        linesJson.put("type", "lines");
        JSONObject linesData = new JSONObject();
        JSONArray linesIdArray = new JSONArray();
        linesData.put("ID", linesIdArray);
        JSONArray linesNameArray = new JSONArray();
        linesData.put("NAME", linesNameArray);
        JSONArray linesVoltArray = new JSONArray();
        linesData.put("VOLT", linesVoltArray);
        JSONArray linesAddrArray = new JSONArray();
        linesData.put("ADDR", linesAddrArray);
        JSONArray linesPmsIdrArray = new JSONArray();
        linesData.put("PMSID", linesPmsIdrArray);
        JSONArray linesParentIdrArray = new JSONArray();
        linesData.put("PARENTID", linesParentIdrArray);
        JSONArray linesSourceIdrArray = new JSONArray();
        linesData.put("SOURCE", linesSourceIdrArray);
        linesJson.put("data", linesData);
        // 配变
        JSONObject transJson = new JSONObject();
        transJson.put("type", "trans");
        JSONObject transData = new JSONObject();
        JSONArray transIdArray = new JSONArray();
        transData.put("ID", transIdArray);
        JSONArray transNameArray = new JSONArray();
        transData.put("NAME", transNameArray);
        JSONArray transVoltArray = new JSONArray();
        transData.put("VOLT", transVoltArray);
        JSONArray transAddrArray = new JSONArray();
        transData.put("ADDR", transAddrArray);
        JSONArray transPmsIdrArray = new JSONArray();
        transData.put("PMSID", transPmsIdrArray);
        JSONArray transParentIdrArray = new JSONArray();
        transData.put("PARENTID", transParentIdrArray);
        JSONArray transSourceIdrArray = new JSONArray();
        transData.put("SOURCE", transSourceIdrArray);
        transJson.put("data", transData);

        for (Node node : nodeSet) {
            String label_m = node.labels().iterator().next().toString();
            switch (label_m) {
                case "SUBS":
                    subsIdArray.add(node.id());
                    subsNameArray.add(node.get("SUBS_NAME").asString().trim());
                    subsVoltArray.add(node.get("VOLT_CODE").asString().trim());
                    subsAddrArray.add(node.get("SUBS_ADDR").asString().trim());
                    subsPmsIdrArray.add(node.get("PMS_SUBS_ID").asString().trim());
                    subsParentIdrArray.add("-1");
                    subsSourceIdrArray.add("YX");
                    break;
                case "LINES":
                    linesIdArray.add(node.id());
                    linesNameArray.add(node.get("LINE_NAME").asString().trim());
                    linesVoltArray.add(node.get("VOLT_CODE").asString().trim());
                    linesAddrArray.add("铜川市");
                    linesPmsIdrArray.add(node.get("PMS_LINE_ID").asString().trim());
                    linesParentIdrArray.add(getParentId(yx_edgeSet, node.id()));
                    linesSourceIdrArray.add("YX");
                    break;
                case "TRAN":
                    transIdArray.add(node.id());
                    transNameArray.add(node.get("TRAN_NAME").asString().trim());
                    transVoltArray.add(node.get("FRSTSIDE_VOLT_CODE").asString().trim());
                    transAddrArray.add("铜川市");
                    transPmsIdrArray.add(node.get("PMS_EQUIP_ID").asString().trim());
                    transParentIdrArray.add(getParentId(yx_edgeSet, node.id()));
                    transSourceIdrArray.add("YX");
                    break;
                case "PMS_SUBS":
                    subsIdArray.add(node.id());
                    subsNameArray.add(node.get("BDZMC").asString().trim());
                    subsVoltArray.add(node.get("DYDJMC").asString().trim());
                    subsAddrArray.add(node.get("ZCDWMC").asString().trim());
                    subsPmsIdrArray.add(node.get("OBJ_ID").asString().trim());
                    subsParentIdrArray.add("-1");
                    subsSourceIdrArray.add("SC");
                    break;
                case "PMS_LINES":
                    linesIdArray.add(node.id());
                    linesNameArray.add(node.get("XLMC").asString().trim());
                    linesVoltArray.add(node.get("DYDJMC").asString().trim());
                    linesAddrArray.add("铜川市");
                    linesPmsIdrArray.add(node.get("OBJ_ID").asString().trim());
                    linesParentIdrArray.add(getParentId(sc_edgeSet, node.id()));
                    linesSourceIdrArray.add("SC");
                    break;
                case "PMS_TRAN":
                    transIdArray.add(node.id());
                    transNameArray.add(node.get("SBMC").asString().trim());
                    transVoltArray.add(node.get("DYDJMC").asString().trim());
                    transAddrArray.add("铜川市");
                    transPmsIdrArray.add(node.get("OBJ_ID").asString().trim());
                    transParentIdrArray.add(getParentId(sc_edgeSet, node.id()));
                    transSourceIdrArray.add("SC");
                    break;
            }
        }
        JSONObject reqJson = new JSONObject();
        reqJson.put("0", subsJson);
        reqJson.put("1", linesJson);
        reqJson.put("2", transJson);
        return sendRestfulRequest(reqJson);
    }

    /**
     * 发送restful请求，进行差异分析
     *
     * @param paramJson
     */
    public JSONObject sendRestfulRequest(JSONObject paramJson) {
        String url = "http://127.0.0.1:5000/get_diff";
        return JSONObject.parseObject(HttpRestfulClient.sendRequest(url, JSON.toJSONString(paramJson)));
    }


    private Integer getSameId(List<JSONArray> arrays, long id) {
        for (JSONArray array : arrays) {
            if (array.size() != 2) {
                return null;
            }
            if (id == (int) array.get(0)) {
                return (int) array.get(1);
            } else if (id == (int) array.get(1)) {
                return (int) array.get(0);
            }
        }
        return null;
    }

    /**
     * 导出差异
     *
     * @param diffIds
     * @return
     */
    public List<File> exportDiffCsv(String diffIds) throws IOException {
        if (diffIds == null || diffIds == "") {
            return Collections.emptyList();
        }
        String cql = "MATCH(N) WHERE ID(N) in [" + diffIds + "] RETURN N";
        StatementResult res = cypher.queryResult(cql, null);
        List<Node> yxNode = new ArrayList<>();
        List<Node> scNode = new ArrayList<>();
        while (res.hasNext()) {
            Record record = res.next();
            Node sNode = record.get("N").asNode();
            String label = sNode.labels().iterator().next();
            switch (label) {
                case "SUBS":
                    yxNode.add(sNode);
                    break;
                case "LINES":
                    yxNode.add(sNode);
                    break;
                case "TRAN":
                    yxNode.add(sNode);
                    break;
                case "PMS_SUBS":
                    scNode.add(sNode);
                    break;
                case "PMS_LINES":
                    scNode.add(sNode);
                    break;
                case "PMS_TRAN":
                    scNode.add(sNode);
                    break;
            }
        }
        return productExportZipFile(yxNode, scNode);
    }

    private List<File> productExportZipFile(List<Node> yxNode, List<Node> scNode) throws IOException {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        List<File> files = new ArrayList<>();
        File yxFile = Util.getDownloadCsvPath("yx");
        File scFile = Util.getDownloadCsvPath("sc");
        files.add(yxFile);
        files.add(scFile);
        String[] yxHeader = ContentUtil.getYxHeader();
        try (BufferedWriter yxbw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(yxFile), Charsets
                .UTF_8))) {
            yxbw.write(new String(bom));
            yxbw.write(ContentUtil.yxExportHeader);
            yxbw.newLine();
            for (Node n : yxNode) {
                for (String header : yxHeader) {
                    yxbw.write(n.get(header).asString().trim() + ",");
                }
                yxbw.newLine();
            }
        }
        String[] scHeader = ContentUtil.getScHeader();
        try (BufferedWriter scbw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(scFile), Charsets
                .UTF_8))) {
            scbw.write(new String(bom));
            scbw.write(ContentUtil.scExportHeader);
            scbw.newLine();
            for (Node n : scNode) {
                for (String header : scHeader) {
                    scbw.write(n.get(header).asString().trim() + ",");
                }
                scbw.newLine();
            }
        }
        return files;
    }


    public long getParentId(Set<Relationship> edgeSet, long id) {
        for (Relationship r : edgeSet) {
            if (r.endNodeId() == id) {
                return r.startNodeId();
            }
        }
        return -1;
    }
}
