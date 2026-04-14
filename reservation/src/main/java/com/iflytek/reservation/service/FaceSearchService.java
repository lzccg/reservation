package com.iflytek.reservation.service;

import com.aliyun.facebody20191230.models.CreateFaceDbRequest;
import com.aliyun.facebody20191230.models.SearchFaceAdvanceRequest;
import com.aliyun.facebody20191230.models.SearchFaceResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.iflytek.reservation.integration.aliyun.AliyunClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FaceSearchService {
    private static final String FACE_DB_NAME_ENV = "RESERVATION_FACE_DB_NAME";
    private static final String DEFAULT_FACE_DB_NAME = "reservation_face_date";
    private static final Map<String, Boolean> FACE_DB_READY = new ConcurrentHashMap<>();

    @Autowired
    private AliyunClients aliyunClients;

    public Match searchTop(byte[] imageBytes) throws Exception {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        String dbName = System.getenv(FACE_DB_NAME_ENV);
        if (dbName == null || dbName.isBlank()) {
            dbName = DEFAULT_FACE_DB_NAME;
        } else {
            dbName = dbName.trim();
        }

        RuntimeOptions runtime = new RuntimeOptions();
        ensureFaceDb(dbName, runtime);

        SearchFaceAdvanceRequest req = new SearchFaceAdvanceRequest()
                .setDbName(dbName)
                .setLimit(1)
                .setImageUrlObject(new ByteArrayInputStream(imageBytes));
        SearchFaceResponse resp = aliyunClients.faceClient().searchFaceAdvance(req, runtime);
        if (resp == null || resp.getBody() == null || resp.getBody().getData() == null) {
            return null;
        }
        var matchList = resp.getBody().getData().getMatchList();
        if (matchList == null || matchList.isEmpty() || matchList.get(0) == null) {
            return null;
        }
        var faceItems = matchList.get(0).getFaceItems();
        if (faceItems == null || faceItems.isEmpty() || faceItems.get(0) == null) {
            return null;
        }
        var item = faceItems.get(0);
        String entityId = item.getEntityId();
        Float confidence = item.getConfidence();
        if (entityId == null || entityId.isBlank() || confidence == null) {
            return null;
        }
        return new Match(entityId.trim(), confidence.doubleValue());
    }

    private void ensureFaceDb(String dbName, RuntimeOptions runtime) throws Exception {
        if (dbName == null || dbName.isBlank()) {
            return;
        }
        if (FACE_DB_READY.putIfAbsent(dbName, true) != null) {
            return;
        }
        try {
            CreateFaceDbRequest req = new CreateFaceDbRequest().setName(dbName);
            aliyunClients.faceClient().createFaceDbWithOptions(req, runtime);
        } catch (TeaException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase(Locale.ROOT).contains("db has existed")) {
                return;
            }
            FACE_DB_READY.remove(dbName);
            throw e;
        } catch (Exception e) {
            FACE_DB_READY.remove(dbName);
            throw e;
        }
    }

    public static class Match {
        private final String entityId;
        private final double score;

        public Match(String entityId, double score) {
            this.entityId = entityId;
            this.score = score;
        }

        public String getEntityId() {
            return entityId;
        }

        public double getScore() {
            return score;
        }
    }
}
