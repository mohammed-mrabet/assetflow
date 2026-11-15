package com.mrabet.assetflow.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parcours complet par l'API REST, sur une vraie base PostgreSQL migrée par Flyway
 * (variables DB_URL, DB_USER, DB_PASSWORD ; voir docker-compose.yml et la CI).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AssetFlowApiIT {

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    /** Suffixe unique : le test peut être relancé sur la même base. */
    private final String run = Long.toString(System.nanoTime(), 36).toUpperCase();

    private record Res(int status, JsonNode body) {
    }

    private Res call(String method, String path, String body) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> res = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode node = res.body() == null || res.body().isBlank() ? null : json.readTree(res.body());
        return new Res(res.statusCode(), node);
    }

    private Res post(String path, String body) throws Exception {
        return call("POST", path, body);
    }

    private Res get(String path) throws Exception {
        return call("GET", path, null);
    }

    private String code(String prefix) {
        return prefix + "-" + run;
    }

    @Test
    void nomenclatureEclatementEtCasDEmploi() throws Exception {
        String model = code("MOD"), motor = code("MOT"), bearing = code("BRG"), roller = code("ROL");
        assertEquals(201, post("/api/items", "{\"code\":\"" + model + "\",\"name\":\"Convoyeur\",\"type\":\"ASSEMBLY\",\"minStock\":0}").status());
        assertEquals(201, post("/api/items", "{\"code\":\"" + motor + "\",\"name\":\"Motoréducteur\",\"type\":\"ASSEMBLY\",\"minStock\":0}").status());
        assertEquals(201, post("/api/items", "{\"code\":\"" + bearing + "\",\"name\":\"Roulement\",\"type\":\"PART\",\"minStock\":4}").status());
        assertEquals(201, post("/api/items", "{\"code\":\"" + roller + "\",\"name\":\"Rouleau\",\"type\":\"PART\",\"minStock\":0}").status());
        assertEquals(409, post("/api/items", "{\"code\":\"" + bearing + "\",\"name\":\"Doublon\",\"type\":\"PART\",\"minStock\":0}").status());

        long motorBom = post("/api/items/" + motor + "/bom-revisions", null).body().get("id").asLong();
        assertEquals(200, post("/api/bom-revisions/" + motorBom + "/lines", "{\"childCode\":\"" + bearing + "\",\"quantity\":2}").status());
        assertEquals(200, post("/api/bom-revisions/" + motorBom + "/release", null).status());

        Res modelDraft = post("/api/items/" + model + "/bom-revisions", null);
        assertEquals("A", modelDraft.body().get("revision").asText());
        long modelBom = modelDraft.body().get("id").asLong();
        post("/api/bom-revisions/" + modelBom + "/lines", "{\"childCode\":\"" + motor + "\",\"quantity\":2}");
        post("/api/bom-revisions/" + modelBom + "/lines", "{\"childCode\":\"" + roller + "\",\"quantity\":12}");
        assertEquals("RELEASED", post("/api/bom-revisions/" + modelBom + "/release", null).body().get("status").asText());

        // Le motoréducteur ne peut pas contenir le convoyeur qui le contient déjà
        long motorB = post("/api/items/" + motor + "/bom-revisions", null).body().get("id").asLong();
        assertEquals(409, post("/api/bom-revisions/" + motorB + "/lines", "{\"childCode\":\"" + model + "\",\"quantity\":1}").status());

        JsonNode explosion = get("/api/items/" + model + "/explosion").body();
        assertEquals(3, explosion.size());
        long bearings = 0;
        for (JsonNode line : explosion) {
            if (line.get("code").asText().equals(bearing)) {
                bearings = line.get("quantity").asLong();
            }
        }
        assertEquals(4, bearings); // 2 motoréducteurs × 2 roulements

        JsonNode whereUsed = get("/api/items/" + bearing + "/where-used").body();
        assertEquals(motor, whereUsed.get(0).get("parentCode").asText());
    }

    @Test
    void cycleDeVieDUnOrdreDeTravailCorrectif() throws Exception {
        String model = code("MODW"), seal = code("SEAL");
        post("/api/items", "{\"code\":\"" + model + "\",\"name\":\"Pompe\",\"type\":\"ASSEMBLY\",\"minStock\":0}");
        post("/api/items", "{\"code\":\"" + seal + "\",\"name\":\"Joint\",\"type\":\"PART\",\"minStock\":2}");
        long bom = post("/api/items/" + model + "/bom-revisions", null).body().get("id").asLong();
        post("/api/bom-revisions/" + bom + "/lines", "{\"childCode\":\"" + seal + "\",\"quantity\":1}");
        post("/api/bom-revisions/" + bom + "/release", null);
        assertEquals(3, post("/api/items/" + seal + "/receipts", "{\"quantity\":3}").body().get("stockQuantity").asInt());

        Res asset = post("/api/assets", "{\"code\":\"" + code("PUMP") + "\",\"name\":\"Pompe de lavage\",\"criticality\":\"A\","
                + "\"commissionedOn\":\"2024-05-01\",\"modelItemCode\":\"" + model + "\"}");
        assertEquals(201, asset.status());
        long assetId = asset.body().get("id").asLong();
        assertEquals(seal, get("/api/assets/" + assetId + "/spare-parts").body().get(0).get("code").asText());

        Res wo = post("/api/work-orders", "{\"assetId\":" + assetId + ",\"priority\":\"P1\",\"description\":\"Fuite au joint\",\"assetStopped\":true}");
        assertEquals(201, wo.status());
        long woId = wo.body().get("id").asLong();
        assertEquals("DOWN", get("/api/assets/" + assetId).body().get("status").asText());

        assertEquals(409, post("/api/work-orders/" + woId + "/close", null).status());       // CREATED → CLOSED interdit
        assertEquals(409, post("/api/work-orders/" + woId + "/parts", "{\"itemCode\":\"" + seal + "\",\"quantity\":1}").status());
        assertEquals("IN_PROGRESS", post("/api/work-orders/" + woId + "/start", null).body().get("status").asText());
        assertEquals(200, post("/api/work-orders/" + woId + "/parts", "{\"itemCode\":\"" + seal + "\",\"quantity\":2}").status());
        Res tooMany = post("/api/work-orders/" + woId + "/parts", "{\"itemCode\":\"" + seal + "\",\"quantity\":5}");
        assertEquals(409, tooMany.status());
        assertTrue(tooMany.body().get("detail").asText().contains("Stock insuffisant"));

        JsonNode low = get("/api/items/below-minimum").body();
        boolean sealLow = false;
        for (JsonNode i : low) {
            sealLow = sealLow || i.get("code").asText().equals(seal);
        }
        assertTrue(sealLow, "le joint doit passer sous le stock minimum (1 < 2)");

        assertEquals(400, post("/api/work-orders/" + woId + "/complete", "{\"report\":\"\"}").status());
        Res done = post("/api/work-orders/" + woId + "/complete", "{\"report\":\"Joint remplacé, essai d'étanchéité OK\"}");
        assertEquals("DONE", done.body().get("status").asText());
        assertEquals("IN_SERVICE", get("/api/assets/" + assetId).body().get("status").asText());
        assertEquals("CLOSED", post("/api/work-orders/" + woId + "/close", null).body().get("status").asText());

        // Période large (hier → demain, bornée à maintenant par le service) : indépendante du fuseau horaire
        String from = LocalDate.now().minusDays(1).toString();
        String to = LocalDate.now().plusDays(1).toString();
        JsonNode kpis = get("/api/assets/" + assetId + "/kpis?from=" + from + "&to=" + to).body().get("kpis");
        assertEquals(1, kpis.get("failures").asInt());
        assertTrue(kpis.get("availability").asDouble() <= 1.0);
    }

    @Test
    void generationDesOrdresPreventifsIdempotente() throws Exception {
        Res asset = post("/api/assets", "{\"code\":\"" + code("CONV") + "\",\"name\":\"Convoyeur\",\"criticality\":\"B\","
                + "\"commissionedOn\":\"2024-01-15\"}");
        long assetId = asset.body().get("id").asLong();
        String lastDone = LocalDate.now().minusDays(40).toString();
        Res plan = post("/api/preventive-plans", "{\"assetId\":" + assetId + ",\"title\":\"Graissage\",\"intervalDays\":30,\"lastDoneOn\":\"" + lastDone + "\"}");
        assertEquals(201, plan.status());

        JsonNode first = post("/api/preventive-plans/generate", null).body().get("createdWorkOrderIds");
        assertTrue(first.size() >= 1);
        JsonNode list = get("/api/work-orders?assetId=" + assetId).body();
        assertEquals(1, list.size());
        assertEquals("PREVENTIVE", list.get(0).get("type").asText());
        assertEquals("PLANNED", list.get(0).get("status").asText());

        post("/api/preventive-plans/generate", null);
        assertEquals(1, get("/api/work-orders?assetId=" + assetId).body().size()); // pas de doublon
    }

    @Test
    void validationEtRessourcesInconnues() throws Exception {
        Res invalid = post("/api/items", "{\"code\":\"\",\"name\":\"x\",\"type\":\"PART\",\"minStock\":-1}");
        assertEquals(400, invalid.status());
        assertTrue(invalid.body().get("errors").has("code"));
        assertEquals(404, get("/api/assets/999999999").status());
        assertEquals(404, get("/api/items/INCONNU-" + run + "/explosion").status());
    }
}
