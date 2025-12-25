import { useEffect, useState } from 'react';

import { PauseCircleOutlined, PlayCircleOutlined } from '@ant-design/icons';
import { Button, Descriptions, Flex, Slider, Table, Tooltip } from 'antd';
import type { ColumnType } from 'antd/es/table';
import _ from 'lodash';

import { useGetProjectCommitsQuery, type CommitDto } from '../../api/babelApi';
import { useInterval } from '../../utils/use-interval';

interface TProps {
  projectId: number;
  branch: string;
  currentCommit?: string;
  setCurrentCommit: (sha?: string) => void;
  onEachInPage: (commit: CommitDto) => void;
  onEachInPrefetchedPage: (commit: CommitDto) => void;
}

const pageSize = 5;

export const CommitTable: React.FC<TProps> = ({
  projectId,
  branch,
  currentCommit,
  setCurrentCommit,
  onEachInPage,
  onEachInPrefetchedPage,
}) => {
  const [page, setPage] = useState(0);
  const [isPlay, setIsPlay] = useState(false);
  const [speed, setSpeed] = useState(1);

  const { data: commits, isLoading: isLoadingCommits } =
    useGetProjectCommitsQuery({
      projectId,
      branch,
      page,
      pageSize,
    });
  const { data: nextCommits } = useGetProjectCommitsQuery(
    {
      projectId,
      branch,
      page: page + 1,
      pageSize,
    },
    {
      skip:
        _.isNil(onEachInPrefetchedPage) ||
        _.isNil(commits?.total) ||
        (page + 1) * pageSize >= commits.total,
    },
  );

  const columns: ColumnType<CommitDto>[] = [
    {
      title: 'Project commits',
      dataIndex: 'sha',
      render: (_value, record) => (
        <Descriptions
          title={
            _.size(record.message) > 45 ? (
              <Tooltip
                title={record.message}
              >{`${record.message.substring(0, 45)}...`}</Tooltip>
            ) : (
              record.message
            )
          }
        >
          <Descriptions.Item>{record.sha}</Descriptions.Item>
          <Descriptions.Item>{record.author}</Descriptions.Item>
          <Descriptions.Item>
            {new Date(record.timestamp).toString()}
          </Descriptions.Item>
        </Descriptions>
      ),
    },
  ];

  useEffect(() => {
    if (
      !_.isNil(commits?.items[0]) &&
      _.isNil(_.find(commits?.items, { sha: currentCommit }))
    ) {
      setCurrentCommit(commits.items[0].sha);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [commits?.items, currentCommit]);

  useEffect(() => {
    if (!_.isEmpty(commits?.items)) {
      _.forEach(commits?.items, commit => onEachInPage(commit));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [commits?.items]);

  useEffect(() => {
    if (!_.isEmpty(nextCommits?.items)) {
      _.forEach(_.slice(nextCommits?.items, 0, pageSize / 2), commit =>
        onEachInPage(commit),
      );
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nextCommits?.items]);

  useInterval(
    () => {
      const currentIndex = _.findIndex(commits?.items, { sha: currentCommit });
      if (currentIndex < _.size(commits?.items) - 1) {
        setCurrentCommit(commits?.items[currentIndex + 1]?.sha);
      } else if ((page + 1) * pageSize < (commits?.total ?? 0)) {
        setPage(page + 1);
      } else {
        setIsPlay(false);
      }
    },
    isPlay ? speed * 1000 : null,
  );

  return (
    <Flex style={{ height: '100%' }} justify="space-between" vertical>
      <Table<CommitDto>
        columns={columns}
        dataSource={commits?.items ?? []}
        loading={isLoadingCommits}
        rowKey={'sha'}
        onRow={record => ({
          onClick: () => setCurrentCommit(record.sha),
        })}
        rowSelection={{
          type: 'radio',
          selectedRowKeys: _.isNil(currentCommit) ? [] : [currentCommit],
          columnWidth: 0,
          renderCell: () => <></>,
        }}
        pagination={{
          pageSize: pageSize,
          current: page + 1,
          total: commits?.total,
          onChange: page => setPage(page - 1),
          showSizeChanger: false,
        }}
      />
      <Flex
        align="center"
        justify="space-between"
        style={{ marginBottom: 16 }}
        vertical
      >
        <Slider
          marks={{ 0: '1s', 25: '2s', 50: '3s', 75: '4s', 100: '5s' }}
          onChange={val => setSpeed(Math.round((val * 4) / 100 + 1))}
          step={null}
          style={{ width: '50%' }}
          tooltip={{
            formatter: value =>
              value ? `${Math.round((value * 4) / 100 + 1)}s` : '1s',
          }}
          value={((speed - 1) * 100) / 4}
        />
        <Flex justify="center">
          <Button
            onClick={() => setIsPlay(!isPlay)}
            icon={isPlay ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
          />
        </Flex>
      </Flex>
    </Flex>
  );
};
