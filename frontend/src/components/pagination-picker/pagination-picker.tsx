import type { ReactNode } from 'react';

import { Text } from '@react-three/uikit';
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from '@react-three/uikit-default';
import _ from 'lodash';
import { Color } from 'three';

interface TProps {
  currentPage: number;
  setCurrentPage: (page: number) => void;
  total: number;
  itemRender?: (page: number) => ReactNode;
}

export const PaginationPicker: React.FC<TProps> = ({
  currentPage,
  setCurrentPage,
  total,
  itemRender,
}) => {
  const renderedItems = total <= 5 ? _.range(0, total) : [];
  return (
    <Pagination>
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious
            color={currentPage === 0 ? Color.NAMES.gray : undefined}
            onClick={() => {
              if (currentPage !== 0) setCurrentPage(currentPage - 1);
            }}
          />
        </PaginationItem>
        {_.map(renderedItems, page => (
          <PaginationItem>
            <PaginationLink
              isActive={page === currentPage}
              onClick={() => setCurrentPage(page)}
            >
              {itemRender?.(page) ?? <Text>{page + 1}</Text>}
            </PaginationLink>
          </PaginationItem>
        ))}
        <PaginationItem>
          <PaginationNext
            color={currentPage === total - 1 ? Color.NAMES.gray : undefined}
            onClick={() => {
              if (currentPage !== total - 1) setCurrentPage(currentPage + 1);
            }}
          />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  );
};
