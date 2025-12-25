import { baseApi as api } from './baseApi';
const injectedRtkApi = api.injectEndpoints({
  endpoints: build => ({
    cloneProject: build.mutation<CloneProjectApiResponse, CloneProjectApiArg>({
      query: queryArg => ({
        url: `/projects/clone`,
        method: 'POST',
        body: queryArg.projectRequestDto,
      }),
    }),
    getProjectBranches: build.query<
      GetProjectBranchesApiResponse,
      GetProjectBranchesApiArg
    >({
      query: queryArg => ({
        url: `/projects/${encodeURIComponent(String(queryArg.projectId))}/branches`,
      }),
    }),
    getProjectBranch: build.query<
      GetProjectBranchApiResponse,
      GetProjectBranchApiArg
    >({
      query: queryArg => ({
        url: `/projects/${encodeURIComponent(String(queryArg.projectId))}/branches/${encodeURIComponent(String(queryArg.branch))}`,
      }),
    }),
    getProjectCommits: build.query<
      GetProjectCommitsApiResponse,
      GetProjectCommitsApiArg
    >({
      query: queryArg => ({
        url: `/projects/${encodeURIComponent(String(queryArg.projectId))}/branches/${encodeURIComponent(String(queryArg.branch))}/commits`,
        params: {
          page: queryArg.page,
          pageSize: queryArg.pageSize,
        },
      }),
    }),
    getProjectCommit: build.query<
      GetProjectCommitApiResponse,
      GetProjectCommitApiArg
    >({
      query: queryArg => ({
        url: `/projects/${encodeURIComponent(String(queryArg.projectId))}/branches/${encodeURIComponent(String(queryArg.branch))}/commits/${encodeURIComponent(String(queryArg.sha))}`,
      }),
    }),
    getCommitMetrics: build.query<
      GetCommitMetricsApiResponse,
      GetCommitMetricsApiArg
    >({
      query: queryArg => ({
        url: `/projects/${encodeURIComponent(String(queryArg.projectId))}/branches/${encodeURIComponent(String(queryArg.branch))}/commits/${encodeURIComponent(String(queryArg.sha))}/metrics`,
        params: {
          metrics: queryArg.metrics,
        },
      }),
    }),
    getLanguages: build.query<GetLanguagesApiResponse, GetLanguagesApiArg>({
      query: () => ({ url: `/languages` }),
    }),
    getMetrics: build.query<GetMetricsApiResponse, GetMetricsApiArg>({
      query: queryArg => ({
        url: `/languages/${encodeURIComponent(String(queryArg.languageCode))}/metrics`,
      }),
    }),
  }),
  overrideExisting: false,
});
export { injectedRtkApi as babelApi };
export type CloneProjectApiResponse =
  /** status 200 Project retrieved/cloned successfully */ ProjectResponseDto;
export type CloneProjectApiArg = {
  projectRequestDto: ProjectRequestDto;
};
export type GetProjectBranchesApiResponse =
  /** status 200 OK */ BranchResponseDto;
export type GetProjectBranchesApiArg = {
  projectId: number;
};
export type GetProjectBranchApiResponse = /** status 200 OK */ BranchDto;
export type GetProjectBranchApiArg = {
  projectId: number;
  branch: string;
};
export type GetProjectCommitsApiResponse =
  /** status 200 OK */ PageResponseCommitDto;
export type GetProjectCommitsApiArg = {
  projectId: number;
  branch: string;
  page?: number;
  pageSize?: number;
};
export type GetProjectCommitApiResponse = /** status 200 OK */ CommitDto;
export type GetProjectCommitApiArg = {
  projectId: number;
  branch: string;
  sha: string;
};
export type GetCommitMetricsApiResponse = /** status 200 OK */ CommitMetricsDto;
export type GetCommitMetricsApiArg = {
  projectId: number;
  branch: string;
  sha: string;
  metrics: string[];
};
export type GetLanguagesApiResponse = /** status 200 OK */ {
  items?: LanguageResponseDto[];
};
export type GetLanguagesApiArg = void;
export type GetMetricsApiResponse =
  /** status 200 OK */ LanguageMetricsResponseDto;
export type GetMetricsApiArg = {
  languageCode: string;
};
export type ProjectResponseDto = {
  /** ID of the project in the database */
  projectId: number;
  /** GitHub repository owner */
  owner: string;
  /** GitHub repository name */
  project: string;
  /** Local path where the project is stored */
  path: string;
  /** Top language of repository */
  languageCode?: string;
  /** Status of repository */
  updateStatus: 'CLONED' | 'UPDATED' | 'NOT_UPDATED';
};
export type ProjectRequestDto = {
  /** URL of the GitHub project */
  url: string;
};
export type BranchDto = {
  /** Short branch name */
  branchName: string;
  /** SHA hash of the latest commit on the branch */
  latestCommit: string;
};
export type BranchResponseDto = {
  items?: BranchDto[];
};
export type PageInfo = {
  page: number;
  pageSize: number;
  total: number;
};
export type CommitDto = {
  /** Commit SHA hash */
  sha: string;
  /** Commit message */
  message: string;
  /** Commit author name */
  author: string;
  /** Commit timestamp in milliseconds since epoch */
  timestamp: number;
  /** Commit number */
  number: number;
};
export type PageResponseCommitDto = PageInfo & {
  items: CommitDto[];
};
export type PackageMetricsNodeDto = {
  name: string;
  items: MetricsNodeDto[];
};
export type ColorValueDto = {
  hex: string;
  display: string;
};
export type MethodMetricDto = {
  metricCode: string;
  numberValue?: number;
  stringValue?: string;
  colorValue?: ColorValueDto;
};
export type MethodMetricsNodeDto = {
  name: string;
  metrics: MethodMetricDto[];
};
export type ClassMetricsNodeDto = {
  name: string;
  items: MethodMetricsNodeDto[];
};
export type MetricsNodeDto = PackageMetricsNodeDto | ClassMetricsNodeDto;
export type CommitMetricsDto = {
  commit: CommitDto;
  root: MetricsNodeDto;
};
export type LanguageResponseDto = {
  languageId: number;
  languageCode: string;
  /** Programming language name */
  languageName: string;
};
export type MetricDto = {
  /** Unique metric identifier */
  metricCode: string;
  /** Human-readable metric name */
  metricName: string;
  /** Detailed description of what the metric measures */
  metricDescription: string;
  metricType: 'NUMERIC' | 'STRING' | 'COLOR';
};
export type LanguageMetricsResponseDto = {
  /** List of available metrics for the language */
  items: MetricDto[];
};
export const {
  useCloneProjectMutation,
  useGetProjectBranchesQuery,
  useGetProjectBranchQuery,
  useGetProjectCommitsQuery,
  useGetProjectCommitQuery,
  useGetCommitMetricsQuery,
  useGetLanguagesQuery,
  useGetMetricsQuery,
} = injectedRtkApi;
