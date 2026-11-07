package org.app.services.layer_3.c3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratingActionRouteStepTest {

    @Test
    void addsLodcProcedureWithoutDuplicates() {
        assertEquals(List.of("20", "71", "82", "81", "83", "84"),
                GeneratingActionRouteStep.withLodcProcedure(List.of("20", "71", "82")));
    }

    @Test
    void handlesNullApplicable() {
        assertEquals(List.of("81", "82", "83", "84"), GeneratingActionRouteStep.withLodcProcedure(null));
    }
}
