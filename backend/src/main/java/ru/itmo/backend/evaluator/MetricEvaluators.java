package ru.itmo.backend.evaluator;

import ru.itmo.backend.evaluator.evaluators.CxxMetricsEvaluator;
import ru.itmo.backend.evaluator.evaluators.JavaMetricEvaluator;

import java.util.List;

public class MetricEvaluators {

    public static MetricEvaluator forLanguage(Language languageName) throws MetricEvaluationException {
        return switch (languageName) {
            case CXX -> new CxxMetricsEvaluator();
            case JAVA -> new JavaMetricEvaluator();
            default ->
                    throw new MetricEvaluationException("No evaluator found for language" + languageName);
        };
    }

    public enum Language {
        CXX, JAVA, GO, PYTHON; // TODO: add other languages

        private static final List<String> CXX_NAMES = List.of("C++", "c++", "CXX", "cxx", "Cxx");
        private static final List<String> JAVA_NAMES = List.of("Java", "java");


        public static Language ofName(String languageName) throws MetricEvaluationException {
            String lowered = languageName.toLowerCase();
            if (CXX_NAMES.contains(languageName)) {
                return CXX;
            } else if (JAVA_NAMES.contains(lowered)) {
                return JAVA;
            } else {
                throw  new MetricEvaluationException("Unknown language name " + languageName);
            }
        }
    }
}
