package graph.electric.backend;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import graph.electric.util.Util;
import graph.electric.util.ZipUtil;
import spark.servlet.SparkApplication;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import static spark.Spark.get;
import static spark.Spark.post;

/**
 * Created by Administrator on 2017/4/15.
 */
public class ElectricRoutes implements SparkApplication {

    private Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private ElectricService service;

    public ElectricRoutes(ElectricService service) {
        this.service = service;
    }

    @Override
    public void init() {
        get("/electric/findById/:id", (req, res) -> gson.toJson(service.findElectricById(req.params("id"))));
        post("/electric/relById", "application/json", (req, res) -> {
            JSONObject resJson = service.findRelElectricById(req.queryParams("id"), req.queryParams("excIds"));
            return resJson.toString();
        });
        post("/electric/queryByCql", "application/json", (req, res) -> {
            String subName = req.queryParams("subName");
            if (subName != null && subName.length() != 0) {
                return service.queryElectricByCypher(getYxScQueryCypher(subName));
            } else {
                return service.queryAllSubs(getAllSubsQueryCypher());
            }
        });

        /**
         * 差异分析
         */
        post("/electric/difAnalysis", "application/json", (req, res) -> {
            String subName = req.queryParams("subName");
            if (subName != null && subName.length() != 0) {
                return service.difYxScAnalysis(getYxScQueryCypher(subName)).toString();
            } else {
                return setEmptyResult();
            }
        });

        post("/electric/diffExport", "application/json", (req, res) -> {
            String diffIds = req.queryParams("diffIds");
            String subName = req.queryParams("subName");

            HttpServletResponse response = res.raw();
            response.reset();
            ServletOutputStream outputStream = res.raw().getOutputStream();
            response.setContentType("application/x-msdownload;");
            response.setHeader("Content-Disposition", "attachment; filename="
                    + Util.toUTF8(subName + ".zip"));
            List<File> fileList = service.exportDiffCsv(diffIds);
            try (ZipOutputStream out = new ZipOutputStream(outputStream)) {
                for (File f : fileList) {
                    ZipUtil.doCompress(f, out);
                    response.flushBuffer();
                }
            }
            return response;
        });
    }

    private Object setEmptyResult() {
        JSONObject reJson = new JSONObject();
        reJson.put("yxData", new JSONObject());
        reJson.put("scData", new JSONObject());
        return reJson;
    }

    private String getYxQueryCypher(String subName) {
        return "MATCH(S:SUBS)-[R:relationship*0..2]-(N) WHERE TRIM(S.SUBS_NAME) =~ '.*" + subName +
                ".*' RETURN S,R,N";
    }

    private String getYxSubsQueryCypher() {
        return "MATCH(S:SUBS) RETURN S";
    }

    private String getScQueryCypher(String subName) {
        return "MATCH(S:PMS_SUBS)-[R:relationship*0..2]-(N) WHERE TRIM(S" +
                ".BDZMC) =~ '.*" + subName + ".*' RETURN S,R,N";
    }

    private String getScSubsQueryCypher() {
        return "MATCH(S:PMS_SUBS) RETURN S";
    }

    private String getYxScQueryCypher(String subName) {
        return getYxQueryCypher(subName) + " UNION " + getScQueryCypher(subName);
    }

    private String getAllSubsQueryCypher() {
        return getYxSubsQueryCypher() + " UNION " + getScSubsQueryCypher();
    }
}