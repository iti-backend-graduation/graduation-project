package com.bm.copilot.copilot.agent;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Placeholder until Developer D's Request Preparation Agent exists; delete this class when it is added.
 * It returns the question as the description and asks the employee to fill in the rest.
 */
@Component
class PlaceholderRequestPreparationAgent implements RequestPreparationAgent {

    @Override
    public RequestDraft run(Input input) {
        return new RequestDraft(null, input.question(), List.of("category", "customerReference"));
    }
}
