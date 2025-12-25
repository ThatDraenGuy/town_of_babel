import type { Color } from '@react-three/fiber';
import _ from 'lodash';

export interface TTreeLeaf {
  id: string;
  name: string;
  area: number;
  height: number;
  color: Color;
  type: 'leaf';
}

export interface TTreeNode {
  name: string;
  children: (TTreeNode | TTreeLeaf)[];
  type: 'node';
}

export interface TResolvedTreeNode {
  name: string;
  area: number;
  height: number;
  children: (TResolvedTreeNode | TTreeLeaf)[];
  type: 'node';
}

export const PADDING_MUL = 1.4;
export const MARGIN_MUL = 1.1;

export const resolveTree = (tree: TTreeNode): TResolvedTreeNode => {
  const resolved = _.map(tree.children, child => {
    if ('area' in child) {
      return child;
    }
    return resolveTree(child);
  });
  return {
    ...tree,
    area: _.sumBy(resolved, 'area') * PADDING_MUL * MARGIN_MUL,
    height: _.maxBy(resolved, 'height')?.height ?? 0,
    children: resolved,
  };
};

export const isLeaf = (
  item: TTreeLeaf | TTreeNode | TResolvedTreeNode,
): item is TTreeLeaf => {
  return item.type === 'leaf';
};

type TBoxPlacedData = {
  dimensions: [w: number, l: number];
  position: [x: number, z: number];
};

export type TPlacedTreeNode = TResolvedTreeNode & TBoxPlacedData;

export type TPlacedTreeLeaf = TTreeLeaf & TBoxPlacedData;
