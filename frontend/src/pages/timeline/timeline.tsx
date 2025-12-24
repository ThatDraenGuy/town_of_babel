import { useEffect, useMemo, useState } from 'react';
import { useParams, useSearchParams } from 'react-router';

import { OrbitControls as Controls } from '@react-three/drei';
import { Canvas, type ThreeElements } from '@react-three/fiber';
import { Layout } from 'antd';
import Sider from 'antd/es/layout/Sider';
import { Content, Footer, Header } from 'antd/es/layout/layout';
import _ from 'lodash';
import { Color } from 'three';

import {
  babelApi,
  useGetCommitMetricsQuery,
  useGetProjectCommitsQuery,
  type ClassMetricsNodeDto,
  type CommitDto,
  type MethodMetricsNodeDto,
  type MetricsNodeDto,
  type PackageMetricsNodeDto,
} from '../../api/babelApi';
import { CommitTable } from '../../components/commit-table/commit-table';
import type { TTreeLeaf, TTreeNode } from '../../treemap/algorithm/tree';
import { TreeMap } from '../../treemap/components/tree-map';
import type { TCommitMetaData } from '../../types/commit';

const Plane = (props: ThreeElements['mesh']) => {
  return (
    <mesh {...props} rotation={[-Math.PI / 2, 0, 0]}>
      <planeGeometry args={[50, 50]} />
      <meshStandardMaterial color="gray" />
    </mesh>
  );
};

type TResolvedCommitData = {
  tree: TTreeNode;
  commit: TCommitMetaData;
};

const commits: TResolvedCommitData[] = [
  {
    commit: {
      hash: '9e1e694',
      msg: 'initial commit',
      date: '29.10.2025',
      author: 'ThatDraenGuy',
    },
    tree: {
      name: 'ru.draen.test',
      type: 'node',
      children: [
        {
          name: 'Main.java',
          type: 'node',
          children: [
            {
              name: 'main',
              type: 'leaf',
              area: 1,
              height: 1,
              color: Color.NAMES.green,
            },
          ],
        },
      ],
    },
  },
  {
    commit: {
      hash: '063438e',
      msg: 'feat: first classes',
      date: '30.10.2025',
      author: 'ThatDraenGuy',
    },
    tree: {
      name: 'ru.draen.test',
      type: 'node',
      children: [
        {
          name: 'Main.java',
          type: 'node',
          children: [
            {
              name: 'main',
              type: 'leaf',
              area: 2,
              height: 1,
              color: Color.NAMES.yellow,
            },
            {
              name: 'processData',
              type: 'leaf',
              area: 4,
              height: 2,
              color: Color.NAMES.green,
            },
          ],
        },
      ],
    },
  },
  {
    commit: {
      hash: '543203f',
      msg: 'feat: lab done',
      date: '02.11.2025',
      author: 'ThatDraenGuy',
    },
    tree: {
      name: 'ru.draen.test',
      type: 'node',
      children: [
        {
          name: 'Main.java',
          type: 'node',
          children: [
            {
              name: 'main',
              type: 'leaf',
              area: 2,
              height: 1,
              color: Color.NAMES.yellow,
            },
            {
              name: 'processData',
              type: 'leaf',
              area: 4,
              height: 2,
              color: Color.NAMES.green,
            },
          ],
        },
        {
          name: 'Processor.java',
          type: 'node',
          children: [
            {
              name: 'read',
              type: 'leaf',
              area: 4,
              height: 1,
              color: Color.NAMES.red,
            },
            {
              name: 'close',
              type: 'leaf',
              area: 4,
              height: 2,
              color: Color.NAMES.green,
            },
            {
              name: 'write',
              type: 'leaf',
              area: 4,
              height: 3,
              color: Color.NAMES.yellow,
            },
            {
              name: 'append',
              type: 'leaf',
              area: 1,
              height: 4,
              color: Color.NAMES.green,
            },
            {
              name: 'flush',
              type: 'leaf',
              area: 1,
              height: 5,
              color: Color.NAMES.red,
            },
          ],
        },
        {
          name: 'Utils.java',
          type: 'node',
          children: [
            {
              name: 'capitalizeLetters',
              type: 'leaf',
              area: 4,
              height: 2,
              color: Color.NAMES.green,
            },
          ],
        },
      ],
    },
  },
];

const PREFETCH_LIMIT = 10;

export const TimelinePage: React.FC = () => {
  const params = useParams();
  const projectId = _.toNumber(params['projectId']);
  const branch = params['branch'] ?? '';
  const [searchParams, setSearchParams] = useSearchParams();
  const prefetch = babelApi.usePrefetch('getCommitMetrics');
  const areaMetric = searchParams.get('area') ?? ''; //TODO actual fallbacks?
  const heightMetric = searchParams.get('height') ?? '';
  const colorMetric = searchParams.get('color') ?? '';

  const [commitSha, setCommitSha] = useState<string>();

  const { data: commitData, isLoading: isLoadingCommitData } =
    useGetCommitMetricsQuery(
      {
        projectId,
        branch,
        sha: commitSha ?? '',
        metrics: _.uniq([areaMetric, heightMetric, colorMetric]),
      },
      { skip: _.isNil(commitSha) },
    );
  // const { data: nextCommits } = useGetProjectCommitsQuery(
  //   {
  //     projectId,
  //     branch,
  //     limit: PREFETCH_LIMIT,
  //     offset: commitData?.commit.number,
  //   },
  //   { skip: _.isNil(commitData) },
  // );

  const prefetchMetrics = (commit: CommitDto) => {
    prefetch({
      projectId,
      branch,
      sha: commit.sha,
      metrics: _.uniq([areaMetric, heightMetric, colorMetric]),
    });
  };
  // useEffect(() => {
  //   if (!_.isEmpty(nextCommits?.items)) {
  //     _.forEach(nextCommits?.items, commit =>
  //       prefetch({
  //         projectId,
  //         branch,
  //         sha: commit.sha,
  //         metrics: _.uniq([areaMetric, heightMetric, colorMetric]),
  //       }),
  //     );
  //   }
  //   // eslint-disable-next-line react-hooks/exhaustive-deps
  // }, [nextCommits]);

  const tree = useMemo(() => {
    const isLeaf = (
      item: MethodMetricsNodeDto | MetricsNodeDto,
    ): item is MethodMetricsNodeDto => 'metrics' in item;
    const mapLeaf = (node: MethodMetricsNodeDto): TTreeLeaf => {
      const area =
        _.find(node.metrics, metric => metric.metricCode === areaMetric)
          ?.value ?? 0;
      const height =
        _.find(node.metrics, metric => metric.metricCode === heightMetric)
          ?.value ?? 0;
      const color =
        _.find(node.metrics, metric => metric.metricCode === colorMetric)
          ?.value ?? 0;
      return {
        area,
        height,
        color: Color.NAMES.green,
        name: node.name,
        type: 'leaf',
      };
    };
    const mapNode = (node: MetricsNodeDto): TTreeNode => ({
      name: node.name,
      children: node.items.map(item =>
        isLeaf(item) ? mapLeaf(item) : mapNode(item),
      ),
      type: 'node',
    });
    return commitData?.root ? mapNode(commitData?.root) : undefined;
  }, [commitData]);
  return (
    // <>
    <Layout style={{ height: '100%' }}>
      <Layout>
        <Content>
          <Canvas>
            <ambientLight intensity={Math.PI / 2} />
            <spotLight
              position={[10, 10, 10]}
              angle={0.15}
              penumbra={1}
              decay={0}
              intensity={Math.PI}
            />
            <pointLight
              position={[-10, -10, -10]}
              decay={0}
              intensity={Math.PI}
            />
            <Plane position={[0, 0, 0]} />
            {tree && <TreeMap position={[0, 0, 0]} tree={tree} />}
            <Controls />
          </Canvas>
        </Content>
      </Layout>
      <Sider width="25%">
        <CommitTable
          projectId={projectId}
          branch={branch}
          currentCommit={commitSha}
          setCurrentCommit={setCommitSha}
          onEachInPage={prefetchMetrics}
          onEachInPrefetchedPage={prefetchMetrics}
        />
      </Sider>
    </Layout>
    // </>
  );
};
