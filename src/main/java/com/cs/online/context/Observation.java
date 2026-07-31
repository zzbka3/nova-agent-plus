package com.cs.online.context;

import java.util.ArrayList;
import java.util.List;

public class Observation {

    private final List<String> traces = new ArrayList<>();

    public void record(String trace) {
        traces.add(trace);
    }

    public List<String> traces() {
        return traces;
    }
}
