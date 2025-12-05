package ru.itmo.backend.evaluator;

public class MetricEvaluationException extends Exception {

    public MetricEvaluationException(String message) {
        super(message);
    }

    public MetricEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetricEvaluationException(Throwable cause) {
        super(cause);
    }
}
