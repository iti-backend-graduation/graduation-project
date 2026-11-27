package com.bm.copilot.copilot;

import java.util.List;

/**
 * The JSON shape the model must reply with. Source ids are the numbers of the sources given in the prompt.
 * {@code clarificationQuestion} is set only when the question is ambiguous; otherwise it is null or empty.
 */
public record CopilotAnswer(String answer, List<Integer> citedSourceIds, boolean refused,
                            boolean suggestServiceRequest, String clarificationQuestion) {
}
