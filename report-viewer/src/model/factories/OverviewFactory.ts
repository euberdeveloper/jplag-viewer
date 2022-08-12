import { Overview } from "../Overview";
import { Metric } from "../Metric";
import { ComparisonListElement } from "../ComparisonListElement";
import { Cluster } from "@/model/Cluster";
import store from "@/store/store";

export class OverviewFactory {

  static getOverview(json: Record<string, unknown>): Overview {
    const submissionFolder = json.submission_folder_path as Array<string>;
    const baseCodeFolder = "";
    const language = json.language as string;
    const fileExtensions = json.file_extensions as Array<string>;
    const matchSensitivity = json.match_sensitivity as number;
    const jsonSubmissions = json.submission_id_to_display_name as Map<string, string>;
    const map = new Map<string, string>(Object.entries(jsonSubmissions));
    const dateOfExecution = json.date_of_execution as string;
    const duration = json.execution_time as number as number;
    const metrics = [] as Array<Metric>;
    const clusters = [] as Array<Cluster>;
    (json.metrics as Array<unknown>).forEach((jsonMetric) => {
      const metric = jsonMetric as Record<string, unknown>;
      const comparisons = [] as Array<ComparisonListElement>;

      (metric.topComparisons as Array<Record<string, unknown>>).forEach(
        (jsonComparison) => {
          const comparison: ComparisonListElement = {
            firstSubmissionId: jsonComparison.first_submission as string,
            secondSubmissionId: jsonComparison.second_submission as string,
            matchPercentage: jsonComparison.match_percentage as number,
          };
          comparisons.push(comparison);
        }
      );
      metrics.push({
        metricName: metric.name as string,
        metricThreshold: metric.threshold as number,
        distribution: metric.distribution as Array<number>,
        comparisons: comparisons,
        description: metric.description as string,
      });
    });
    store.commit("saveSubmissionNames", map);
    if (json.clusters) {
      (json.clusters as Array<unknown>).forEach((jsonCluster) => {
        const cluster = jsonCluster as Record<string, unknown>;
        const newCluster: Cluster = {
          averageSimilarity: cluster.average_similarity as number,
          strength: cluster.strength as number,
          members: cluster.members as Array<string>,
        };
        clusters.push(newCluster);
      });
    }
    
    OverviewFactory.saveSubmissionsToComparisonNameMap(json);
    return new Overview(
      submissionFolder,
      baseCodeFolder,
      language,
      fileExtensions,
      matchSensitivity,
      dateOfExecution,
      duration,
      metrics,
      clusters,
      new Map()
    );
  }

  private static  saveSubmissionsToComparisonNameMap(json: Record<string, unknown>){
    const submissionIdsToComparisonName =
    json.submission_ids_to_comparison_file_name as Map<
      string,
      Map<string, string>
    >;
  const test: Array<Array<string | object>> = Object.entries(
    submissionIdsToComparisonName
  );
  const comparisonMap = new Map<string, Map<string, string>>(
  );
  for (const [key, value] of test) {
    comparisonMap.set(key as string, new Map(Object.entries(value as object)));
  }

  store.commit("saveComparisonFileLookup", comparisonMap);
  }
}
