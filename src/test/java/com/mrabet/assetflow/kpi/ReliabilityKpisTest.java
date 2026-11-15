package com.mrabet.assetflow.kpi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mrabet.assetflow.kpi.ReliabilityKpis.Failure;

class ReliabilityKpisTest {

    private static final Instant FROM = Instant.parse("2027-02-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2027-02-11T00:00:00Z"); // 240 heures

    private static Instant at(String s) {
        return Instant.parse(s);
    }

    @Test
    void mttrMtbfEtDisponibilite() {
        List<Failure> failures = List.of(
                new Failure(at("2027-02-02T08:00:00Z"), at("2027-02-02T12:00:00Z")),   // 4 h
                new Failure(at("2027-02-05T10:00:00Z"), at("2027-02-05T18:00:00Z")));  // 8 h
        ReliabilityKpis k = ReliabilityKpis.compute(failures, FROM, TO);
        assertEquals(2, k.failures());
        assertEquals(12.0, k.downtimeHours(), 1e-9);
        assertEquals(6.0, k.mttrHours(), 1e-9);
        assertEquals(114.0, k.mtbfHours(), 1e-9);      // (240 - 12) / 2
        assertEquals(0.95, k.availability(), 1e-9);    // 228 / 240
    }

    @Test
    void panneEnCoursOuQuiDepasseLaPeriode() {
        List<Failure> failures = List.of(
                new Failure(at("2027-02-10T18:00:00Z"), null),                          // toujours en panne : 6 h
                new Failure(at("2027-01-31T20:00:00Z"), at("2027-02-01T02:00:00Z")));   // commencée avant : ignorée
        ReliabilityKpis k = ReliabilityKpis.compute(failures, FROM, TO);
        assertEquals(1, k.failures());
        assertEquals(6.0, k.downtimeHours(), 1e-9);
        assertEquals(0.975, k.availability(), 1e-9);
    }

    @Test
    void aucunePanne() {
        ReliabilityKpis k = ReliabilityKpis.compute(List.of(), FROM, TO);
        assertEquals(0, k.failures());
        assertEquals(0.0, k.mttrHours(), 1e-9);
        assertEquals(240.0, k.mtbfHours(), 1e-9);
        assertEquals(1.0, k.availability(), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> ReliabilityKpis.compute(List.of(), TO, FROM));
    }
}
