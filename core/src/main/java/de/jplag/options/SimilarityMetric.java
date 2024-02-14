package de.jplag.options;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import de.jplag.JPlagComparison;
import de.jplag.Match;

public enum SimilarityMetric implements ToDoubleFunction<JPlagComparison> {
    AVG("average similarity", JPlagComparison::similarity),
    MIN("minimum similarity", JPlagComparison::minimalSimilarity),
    MAX("maximal similarity", JPlagComparison::maximalSimilarity),
    INTERSECTION("matched tokens", it -> (double) it.getNumberOfMatchedTokens()),
    SYMMETRIC("symmetric similarity", it -> {
        int divisor = it.firstSubmission().getNumberOfTokens() + it.secondSubmission().getNumberOfTokens();
        if (divisor != 0) {
            return 2.0 * it.getNumberOfMatchedTokens() / divisor;
        } else {
            return .0;
        }
    }),
    LONGEST_MATCH("number of tokens in the longest match", it -> it.matches().stream().mapToInt(Match::length).max().orElse(0)),
    OVERALL("Sum of both submission lengths", it -> it.firstSubmission().getNumberOfTokens() + it.secondSubmission().getNumberOfTokens());

    private final ToDoubleFunction<JPlagComparison> similarityFunction;
    private final String description;

    SimilarityMetric(String description, ToDoubleFunction<JPlagComparison> similarityFunction) {
        this.description = description;
        this.similarityFunction = similarityFunction;
    }

    public boolean isAboveThreshold(JPlagComparison comparison, double similarityThreshold) {
        return similarityFunction.applyAsDouble(comparison) >= similarityThreshold;
    }

    @Override
    public double applyAsDouble(JPlagComparison comparison) {
        return similarityFunction.applyAsDouble(comparison);
    }

    @Override
    public String toString() {
        return description;
    }

    public static Map<String, Double> createSimilarityMap(JPlagComparison comparison) {
        Map<String, Double> result = new HashMap<>();
        for (SimilarityMetric metric : SimilarityMetric.values()) {
            result.put(metric.name(), metric.applyAsDouble(comparison));
        }
        return result;
    }
}
