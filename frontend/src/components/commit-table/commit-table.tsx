import { useEffect, useState } from 'react';

import { Table } from 'antd';
import type { ColumnType } from 'antd/es/table';
import _ from 'lodash';

import {
  babelApi,
  useGetProjectCommitsQuery,
  type CommitDto,
} from '../../api/babelApi';

interface TProps {
  projectId: number;
  branch: string;
  currentCommit?: string;
  setCurrentCommit: (sha: string) => void;
  onEachInPage: (commit: CommitDto) => void;
  onEachInPrefetchedPage: (commit: CommitDto) => void;
}

const pageSize = 10;

export const CommitTable: React.FC<TProps> = ({
  projectId,
  branch,
  currentCommit,
  setCurrentCommit,
  onEachInPage,
  onEachInPrefetchedPage,
}) => {
  const [page, setPage] = useState(0);
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
      title: 'sha',
      dataIndex: 'sha',
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

  return (
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
      }}
    />
  );
};
