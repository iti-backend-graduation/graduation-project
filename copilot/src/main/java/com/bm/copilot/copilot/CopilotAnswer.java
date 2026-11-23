package com.bm.copilot.copilot;

import java.util.List;

/** The JSON shape the model must reply with. Source ids are the numbers of the sources given in the prompt. */
public record CopilotAnswer(String answer, List<Integer> citedSourceIds, boolean refused,
                            boolean suggestServiceRequest) {
}
