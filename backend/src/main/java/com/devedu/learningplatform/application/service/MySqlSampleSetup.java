package com.devedu.learningplatform.application.service;

final class MySqlSampleSetup {

    private static final String START_MARKER = "-- DEVEDU_SAMPLE_DATA_BEGIN";
    private static final String END_MARKER = "-- DEVEDU_SAMPLE_DATA_END";

    private MySqlSampleSetup() {
    }

    static String includeIn(String sourceCode, String setupSql) {
        var sourceWithoutSetup = removeFrom(sourceCode);
        if (setupSql == null || setupSql.isBlank()) return sourceWithoutSetup;
        return START_MARKER + "\n"
                + setupSql.stripTrailing() + "\n"
                + END_MARKER + "\n\n"
                + sourceWithoutSetup.stripLeading();
    }

    static String removeFrom(String sourceCode) {
        if (sourceCode == null) return null;
        var start = sourceCode.indexOf(START_MARKER);
        if (start < 0) return sourceCode;
        var end = sourceCode.indexOf(END_MARKER, start + START_MARKER.length());
        if (end < 0) return sourceCode;
        return (sourceCode.substring(0, start)
                + sourceCode.substring(end + END_MARKER.length())).stripLeading();
    }
}
