import { useState } from 'react';
import { useNavigate } from 'react-router';

import { OrbitControls as Controls } from '@react-three/drei';
import { Canvas, type ThreeElements } from '@react-three/fiber';
import { Button, Form, Modal, Select } from 'antd';
import { useForm, type FormProps } from 'antd/es/form/Form';
import FormItem from 'antd/es/form/FormItem';
import Search, { type SearchProps } from 'antd/es/input/Search';
import _ from 'lodash';

import { babelApi, type ProjectResponseDto } from '../../api/babelApi';
import { useAppDispatch, useAppSelector } from '../../hooks';
import { setProject } from '../../slices/project-slice';

const Plane = (props: ThreeElements['mesh']) => {
  return (
    <mesh {...props} rotation={[-Math.PI / 2, 0, 0]}>
      <planeGeometry args={[50, 50]} />
      <meshStandardMaterial color="gray" />
    </mesh>
  );
};

interface TFormData {
  branch: string;
  area: string;
  height: string;
  color: string;
}

export const WelcomePage: React.FC = () => {
  const dispatch = useAppDispatch();
  const project = useAppSelector(state => state.project.project);
  const [form] = useForm<TFormData>();
  const navigate = useNavigate();
  const [cloneProject, { isLoading: isCloningProject }] =
    babelApi.useCloneProjectMutation();

  const { data: branches, isLoading: isLoadingBranches } =
    babelApi.useGetProjectBranchesQuery(
      { projectId: project?.projectId ?? 0 },
      { skip: _.isNil(project) },
    );
  const { data: metrics, isLoading: isLoadingMetrics } =
    babelApi.useGetMetricsQuery(
      { languageCode: project?.languageCode ?? '' },
      { skip: _.isNil(project) },
    );

  const onSearch: SearchProps['onSearch'] = async value => {
    const result = await cloneProject({ projectRequestDto: { url: value } });
    dispatch(setProject({ project: result.data, url: value }));
  };

  const onSubmit: FormProps<TFormData>['onFinish'] = values => {
    navigate(
      `/projects/${project?.projectId}/${encodeURIComponent(values.branch)}?area=${values.area}&height=${values.height}&color=${values.color}`,
    );
  };

  return (
    <>
      <Canvas style={{ zIndex: 1 }}>
        <ambientLight intensity={Math.PI / 2} />
        <spotLight
          position={[10, 10, 10]}
          angle={0.15}
          penumbra={1}
          decay={0}
          intensity={Math.PI}
        />
        <pointLight position={[-10, -10, -10]} decay={0} intensity={Math.PI} />
        <Plane position={[0, 0, 0]} />
        <Controls />
      </Canvas>
      <Modal
        centered
        mask={false}
        closable={false}
        open
        title={
          _.isNil(project)
            ? 'Search for your repository'
            : 'Choose your options'
        }
        footer={
          _.isNil(project)
            ? []
            : [
                <Button
                  onClick={() =>
                    setProject({ project: undefined, url: undefined })
                  }
                >
                  Back
                </Button>,
                <Button onClick={() => form.submit()} type="primary">
                  Submit
                </Button>,
              ]
        }
        styles={{
          wrapper: {
            pointerEvents: 'none',
          },
        }}
      >
        {_.isNil(project) ? (
          <Search
            placeholder="input search text"
            allowClear
            enterButton
            size="large"
            onSearch={onSearch}
            loading={isCloningProject}
          />
        ) : (
          <Form form={form} onFinish={onSubmit}>
            <FormItem
              name="branch"
              required
              label="Branch to visualize"
              rules={[{ required: true }]}
            >
              <Select
                loading={isLoadingBranches}
                options={_.map(branches?.items, branch => ({
                  label: branch.branchName,
                  value: branch.branchName,
                }))}
              />
            </FormItem>
            <FormItem
              name="area"
              required
              label="Metric to show via house area"
              rules={[{ required: true }]}
            >
              <Select
                loading={isLoadingMetrics}
                options={_.map(
                  _.filter(
                    metrics?.items,
                    item => item.metricType === 'NUMERIC',
                  ),
                  metric => ({
                    label: metric.metricName,
                    value: metric.metricCode,
                  }),
                )}
              />
            </FormItem>
            <FormItem
              name="height"
              required
              label="Metric to show via house height"
              rules={[{ required: true }]}
            >
              <Select
                loading={isLoadingMetrics}
                options={_.map(
                  _.filter(
                    metrics?.items,
                    item => item.metricType === 'NUMERIC',
                  ),
                  metric => ({
                    label: metric.metricName,
                    value: metric.metricCode,
                  }),
                )}
              />
            </FormItem>
            <FormItem
              name="color"
              required
              label="Metric to show via house color"
              rules={[{ required: true }]}
            >
              <Select
                loading={isLoadingMetrics}
                options={_.map(
                  _.filter(metrics?.items, item => item.metricType === 'COLOR'),
                  metric => ({
                    label: metric.metricName,
                    value: metric.metricCode,
                  }),
                )}
              />
            </FormItem>
          </Form>
        )}
      </Modal>
    </>
  );
};
