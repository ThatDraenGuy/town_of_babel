import { useMemo, useState } from 'react';
import { useParams, useSearchParams } from 'react-router';

import { GithubOutlined } from '@ant-design/icons';
import {
  OrbitControls as Controls,
  OrthographicCamera,
  PerspectiveCamera,
} from '@react-three/drei';
import { Canvas, type ThreeElements } from '@react-three/fiber';
import { Button, Descriptions, Drawer, Layout } from 'antd';
import Sider from 'antd/es/layout/Sider';
import { Content } from 'antd/es/layout/layout';
import _ from 'lodash';
import { Color } from 'three';
import { label } from 'three/tsl';

import {
  babelApi,
  useGetCommitMetricsQuery,
  type CommitDto,
  type MethodMetricsNodeDto,
  type MetricsNodeDto,
} from '../../api/babelApi';
import { CommitTable } from '../../components/commit-table/commit-table';
import { useAppSelector } from '../../hooks';
import type { TTreeLeaf, TTreeNode } from '../../treemap/algorithm/tree';
import { TreeMap } from '../../treemap/components/tree-map';

const Plane = (props: ThreeElements['mesh']) => {
  return (
    <mesh {...props} rotation={[-Math.PI / 2, 0, 0]}>
      <planeGeometry args={[500, 500]} />
      <meshStandardMaterial color="gray" />
    </mesh>
  );
};

export const TimelinePage: React.FC = () => {
  const project = useAppSelector(state => state.project);

  const params = useParams();
  const projectId = _.toNumber(params['projectId']);
  const branch = params['branch'] ?? '';
  const [searchParams, setSearchParams] = useSearchParams();
  const prefetch = babelApi.usePrefetch('getCommitMetrics');
  const areaMetric = searchParams.get('area') ?? ''; //TODO actual fallbacks?
  const heightMetric = searchParams.get('height') ?? '';
  const colorMetric = searchParams.get('color') ?? '';

  const [commitSha, setCommitSha] = useState<string>();
  const [selectedItem, setSelectedItem] = useState<string>();

  const { data: metrics } = babelApi.useGetMetricsQuery(
    { languageCode: project.project?.languageCode ?? '' },
    { skip: _.isNil(project.project) },
  );

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

  const prefetchMetrics = (commit: CommitDto) => {
    prefetch({
      projectId,
      branch,
      sha: commit.sha,
      metrics: _.uniq([areaMetric, heightMetric, colorMetric]),
    });
  };

  const isLeaf = (
    item: MethodMetricsNodeDto | MetricsNodeDto,
  ): item is MethodMetricsNodeDto => 'metrics' in item;

  const tree = useMemo(() => {
    const mapLeaf = (
      node: MethodMetricsNodeDto,
      prefix?: string,
    ): TTreeLeaf => {
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
        id: `${prefix}::${node.name}`,
        area,
        height,
        color: Color.NAMES.green,
        name: node.name,
        type: 'leaf',
      };
    };

    const mapNode = (node: MetricsNodeDto, prefix?: string): TTreeNode => ({
      name: node.name,
      children: node.items.map(item =>
        isLeaf(item)
          ? mapLeaf(item, prefix ? `${prefix}.${node.name}` : node.name)
          : mapNode(item, prefix ? `${prefix}.${node.name}` : node.name),
      ),
      type: 'node',
    });
    return commitData?.root ? mapNode(commitData?.root) : undefined;
  }, [commitData]);

  const findMethodData = (id?: string) => {
    const goInto = (node: MetricsNodeDto, name: string) => {
      for (const item of node.items) {
        if (name.startsWith(item.name)) {
          const next = name.replace(new RegExp(`^${item.name}(\\.|::)`), '');
          if (isLeaf(item)) {
            return item;
          }
          return goInto(item, next);
        }
      }
    };

    if (commitData?.root && id) {
      return goInto(
        commitData.root,
        id.replace(new RegExp(`^${commitData.root.name}(\\.|::)`), ''),
      );
    }
  };

  return (
    <>
      <Layout style={{ height: '100%' }}>
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
        <Layout>
          <Content>
            <Canvas>
              <ambientLight intensity={Math.PI / 2} />
              <pointLight position={[0, 20, 0]} decay={0} intensity={Math.PI} />
              <Plane position={[0, 0, 0]} />
              {tree && (
                <TreeMap
                  position={[0, 0, 0]}
                  tree={tree}
                  selected={selectedItem}
                  setSelected={setSelectedItem}
                />
              )}
              <Controls />
              <OrthographicCamera position={[0, 20, 0]} />
              <PerspectiveCamera makeDefault position={[0, 20, 0]} />
            </Canvas>
          </Content>
        </Layout>
      </Layout>
      <Drawer
        size="large"
        open={!_.isNil(selectedItem)}
        onClose={() => setSelectedItem(undefined)}
        title={selectedItem}
      >
        <Descriptions
          items={_.map(findMethodData(selectedItem)?.metrics, metric => ({
            key: metric.metricCode,
            label: _.find(metrics?.items, { metricCode: metric.metricCode })
              ?.metricName,
            children: metric.value,
          }))}
        />
      </Drawer>
    </>
  );
};
