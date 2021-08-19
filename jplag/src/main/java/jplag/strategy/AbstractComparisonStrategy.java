package jplag.strategy;

import java.util.Hashtable;
import java.util.Optional;
import java.util.Vector;

import jplag.GreedyStringTiling;
import jplag.JPlagComparison;
import jplag.Submission;
import jplag.options.JPlagOptions;
import jplag.options.SimilarityMetric;

public abstract class AbstractComparisonStrategy implements ComparisonStrategy {

    // TODO PB: I think it's better to make each submission store its own matches with the base code.
    // Hashtable that maps the name of a submissions to its matches with the provided base code.
    private Hashtable<String, JPlagComparison> baseCodeMatches = new Hashtable<>(30);

    private GreedyStringTiling greedyStringTiling;

    protected JPlagOptions options;

    public AbstractComparisonStrategy(JPlagOptions options, GreedyStringTiling greedyStringTiling) {
        this.greedyStringTiling = greedyStringTiling;
        this.options = options;
    }

    protected void compareSubmissionsToBaseCode(Vector<Submission> submissions, Submission baseCodeSubmission) {
        for (Submission currentSubmission : submissions) {
            JPlagComparison baseCodeMatch = greedyStringTiling.compareWithBaseCode(currentSubmission, baseCodeSubmission);
            baseCodeMatches.put(currentSubmission.name, baseCodeMatch);
            baseCodeSubmission.resetBaseCode();
        }
    }

    /**
     * Compares two submissions and optionally returns the results if similarity is high enough.
     */
    protected Optional<JPlagComparison> compareSubmissions(Submission first, Submission second, boolean withBaseCode) {
        JPlagComparison comparison = greedyStringTiling.compare(first, second);
        System.out.println("Comparing " + first.name + "-" + second.name + ": " + comparison.percent());
        if (withBaseCode) {
            comparison.baseCodeMatchesA = baseCodeMatches.get(comparison.firstSubmission.name);
            comparison.baseCodeMatchesB = baseCodeMatches.get(comparison.secondSubmission.name);
        }
        if (isAboveSimilarityThreshold(comparison)) {
            return Optional.of(comparison);
        }
        return Optional.empty();
    }

    private boolean isAboveSimilarityThreshold(JPlagComparison comparison) {
        float similarityThreshold = this.options.getSimilarityThreshold();
        SimilarityMetric similarityMetric = this.options.getSimilarityMetric();

        switch (similarityMetric) {
        case AVG:
            return comparison.percent() >= similarityThreshold;
        case MAX:
            return comparison.percentMaxAB() >= similarityThreshold;
        case MIN:
            return comparison.percentMinAB() >= similarityThreshold;
        default:
            return true;
        }
    }

}
