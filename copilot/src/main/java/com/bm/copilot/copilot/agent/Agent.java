package com.bm.copilot.copilot.agent;

/** One specialised step of the copilot. Input and output are typed records, never free text alone. */
public interface Agent<I, O> {

    O run(I input);
}
