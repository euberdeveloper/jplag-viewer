import type { Match } from './Match'
import type { SubmissionFile } from './SubmissionFile'
import type { MatchInSingleFile } from './MatchInSingleFile'
import type MetricType from './MetricType'

/**
 * Comparison model used by the ComparisonView
 */
export class Comparison {
  private readonly _firstSubmissionId: string
  private readonly _secondSubmissionId: string
  private readonly _similarities: Record<MetricType, number>
  private _filesOfFirstSubmission: Map<string, SubmissionFile>
  private _filesOfSecondSubmission: Map<string, SubmissionFile>
  private _allMatches: Array<Match>

  constructor(
    firstSubmissionId: string,
    secondSubmissionId: string,
    similarities: Record<MetricType, number>,
    filesOfFirstSubmission: Map<string, SubmissionFile>,
    filesOfSecondSubmission: Map<string, SubmissionFile>,
    allMatches: Array<Match>
  ) {
    this._firstSubmissionId = firstSubmissionId
    this._secondSubmissionId = secondSubmissionId
    this._similarities = similarities
    this._filesOfFirstSubmission = filesOfFirstSubmission
    this._filesOfSecondSubmission = filesOfSecondSubmission
    this._allMatches = allMatches
  }

  /**
   * @return Map of all files of the first submission
   */
  get filesOfFirstSubmission(): Map<string, SubmissionFile> {
    return this._filesOfFirstSubmission
  }

  /**
   * @return Map of all files of the second submission
   */
  get filesOfSecondSubmission(): Map<string, SubmissionFile> {
    return this._filesOfSecondSubmission
  }

  /**
   * @return Array of all matches
   */
  get allMatches(): Array<Match> {
    return this._allMatches
  }

  /**
   * @return Map of all matches in the first submission
   */
  get matchesInFirstSubmission(): Map<string, Array<MatchInSingleFile>> {
    return this.groupMatchesByFileName(1)
  }

  /**
   * @return Map of all matches in the second submission
   */
  get matchesInSecondSubmissions(): Map<string, Array<MatchInSingleFile>> {
    return this.groupMatchesByFileName(2)
  }

  /**
   * @return Id of the first submission
   */
  get firstSubmissionId() {
    return this._firstSubmissionId
  }

  /**
   * @return Id of the second submission
   */
  get secondSubmissionId() {
    return this._secondSubmissionId
  }

  /**
   * @return Similarity of the two submissions
   */
  get similarities() {
    return this._similarities
  }

  private groupMatchesByFileName(index: 1 | 2): Map<string, Array<MatchInSingleFile>> {
    const acc = new Map<string, Array<MatchInSingleFile>>()
    this.allMatches.forEach((val) => {
      const name = index === 1 ? val.firstFile : val.secondFile

      if (!acc.get(name)) {
        acc.set(name, [])
      }

      if (index === 1) {
        const newVal: MatchInSingleFile = {
          start: val.startInFirst,
          end: val.endInFirst,
          linked_panel: 2,
          linked_file: val.secondFile,
          linked_line: val.startInSecond,
          color: val.color
        }
        acc.get(name)?.push(newVal)
      } else {
        const newVal: MatchInSingleFile = {
          start: val.startInSecond,
          end: val.endInSecond,
          linked_panel: 1,
          linked_file: val.firstFile,
          linked_line: val.startInFirst,
          color: val.color
        }
        acc.get(name)?.push(newVal)
      }
    })
    return acc
  }
}
