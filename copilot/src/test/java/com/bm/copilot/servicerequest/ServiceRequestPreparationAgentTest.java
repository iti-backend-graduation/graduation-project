package com.bm.copilot.servicerequest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bm.copilot.copilot.agent.RequestDraft;
import com.bm.copilot.copilot.agent.RequestPreparationAgent;
import com.bm.copilot.retrieval.Citation;
import com.bm.copilot.retrieval.RetrievedChunk;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The Request Preparation Agent: a typed draft from the question and evidence, and nothing else. */
class ServiceRequestPreparationAgentTest {

    private final ServiceRequestPreparationAgent agent = new ServiceRequestPreparationAgent();

    private static RetrievedChunk chunk(String document, String section, Integer page) {
        return new RetrievedChunk(UUID.randomUUID(), "text", new Citation(document, page, section), 0.9);
    }

    @Test
    void aLostCardQuestionBecomesACardServiceDraftThatStillNeedsTheCardAndCustomer() {
        RequestDraft draft = agent.run(new RequestPreparationAgent.Input(
                "A customer lost their debit card. What should I do?",
                List.of(chunk("02-lost-stolen-card-procedure.md", "3.1 Blocking the card", null))));

        assertThat(draft.category()).isEqualTo("CARD_SERVICE");
        assertThat(draft.missingFields()).containsExactly("customerReference", "cardLastFour");
        assertThat(draft.description())
                .startsWith("A customer lost their debit card. What should I do?")
                .contains("Based on: 02-lost-stolen-card-procedure.md - 3.1 Blocking the card");
    }

    @Test
    void digitalAndAccountQuestionsGetTheirCategory() {
        assertThat(agent.run(new RequestPreparationAgent.Input(
                "The customer cannot log in to the mobile app after a password reset", List.of())).category())
                .isEqualTo("DIGITAL_BANKING");
        assertThat(agent.run(new RequestPreparationAgent.Input(
                "The customer wants to close their savings account", List.of())).category())
                .isEqualTo("ACCOUNT_SERVICE");
        assertThat(agent.run(new RequestPreparationAgent.Input(
                "Which documents and proof of address are still missing?", List.of())).category())
                .isEqualTo("ADDITIONAL_DOCUMENTS");
    }

    @Test
    void evidenceBreaksATieWhenTheQuestionIsVague() {
        RequestDraft draft = agent.run(new RequestPreparationAgent.Input("Help the customer with this",
                List.of(chunk("10-account-maintenance-and-closure.md", "Closing an account", null))));

        assertThat(draft.category()).isEqualTo("ACCOUNT_SERVICE");
        assertThat(draft.missingFields()).containsExactly("customerReference");
    }

    @Test
    void anUnclearQuestionLeavesTheCategoryToTheEmployee() {
        RequestDraft draft = agent.run(new RequestPreparationAgent.Input("What should I do next?", List.of()));

        assertThat(draft.category()).isNull();
        assertThat(draft.missingFields()).containsExactly("category", "customerReference");
        assertThat(draft.description()).isEqualTo("What should I do next?");
    }

    @Test
    void theDescriptionCitesAtMostThreeDistinctSourcesWithPages() {
        RetrievedChunk pdf = chunk("16-synthetic-fee-schedule.pdf", "Card fees", 1);
        RequestDraft draft = agent.run(new RequestPreparationAgent.Input("Card replacement fee", List.of(
                pdf, pdf, chunk("a.md", "A", null), chunk("b.md", "B", null), chunk("c.md", "C", null))));

        assertThat(draft.description()).contains("16-synthetic-fee-schedule.pdf - Card fees (page 1)")
                .contains("a.md - A").contains("b.md - B").doesNotContain("c.md");
    }

    @Test
    void theDescriptionFitsTheRequestLimit() {
        RequestDraft draft = agent.run(new RequestPreparationAgent.Input("card ".repeat(1000), List.of()));

        assertThat(draft.description()).hasSize(ServiceRequestPreparationAgent.MAX_DESCRIPTION);
    }

    @Test
    void theAgentHasNoDependenciesSoItCannotSubmitApproveOrExecute() {
        Constructor<?>[] constructors = ServiceRequestPreparationAgent.class.getConstructors();

        assertThat(constructors).hasSize(1);
        assertThat(constructors[0].getParameterCount()).isZero();
        assertThat(ServiceRequestPreparationAgent.class.getDeclaredFields())
                .allMatch(field -> java.lang.reflect.Modifier.isStatic(field.getModifiers()));
    }
}
