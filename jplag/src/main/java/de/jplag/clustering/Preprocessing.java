package de.jplag.clustering;

import java.util.Optional;
import java.util.function.Function;

import antlr.preprocessor.Preprocessor;

import de.jplag.clustering.preprocessors.CumulativeDistributionFunctionPreprocessor;
import de.jplag.clustering.preprocessors.PercentileThresholdProcessor;
import de.jplag.clustering.preprocessors.ThresholdPreprocessor;

/**
 * List of all usable {@link Preprocessor}s.
 */
public enum Preprocessing {
    NONE(options -> null),
    /** {@link CumulativeDistributionFunctionPreprocessor} */
    CUMULATIVE_DISTRIBUTION_FUNCTION(options -> new CumulativeDistributionFunctionPreprocessor()),
    /** {@link ThresholdPreprocessor} */
    THRESHOLD(options -> new ThresholdPreprocessor(options.getPreprocessorThreshold())),
    /** {@link PercentileThresholdProcessor} */
    PERCENTILE(options -> new PercentileThresholdProcessor(options.getPreprocessorPercentile()));

    private final Function<ClusteringOptions, ClusteringPreprocessor> constructor;

    private Preprocessing(Function<ClusteringOptions, ClusteringPreprocessor> constructor) {
        this.constructor = constructor;
    }

    public Optional<ClusteringPreprocessor> constructPreprocessor(ClusteringOptions options) {
        return Optional.ofNullable(constructor.apply(options));
    }
}
