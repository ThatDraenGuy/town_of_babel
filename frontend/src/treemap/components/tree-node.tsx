import { Html } from '@react-three/drei';
import type { ThreeElements } from '@react-three/fiber';
import _ from 'lodash';

import { squarify } from '../algorithm/squarify';
import {
  isLeaf,
  MARGIN_MUL,
  PADDING_MUL,
  type TPlacedTreeLeaf,
  type TPlacedTreeNode,
} from '../algorithm/tree';

import { Border } from './border';
import { TreeLeaf } from './tree-leaf';

type TProps = Omit<ThreeElements['mesh'], 'position'> & {
  position: [x: number, y: number, z: number];
  node: TPlacedTreeNode;
};

export const TreeNode: React.FC<TProps> = ({ position, node, ...props }) => {
  const padding =
    (node.dimensions[0] +
      node.dimensions[1] -
      Math.sqrt(
        (node.dimensions[0] - node.dimensions[1]) ** 2 +
          (4 * node.area) / PADDING_MUL,
      )) /
    4;
  const fullBox = {
    x: 0,
    z: 0,
    w: node.dimensions[0],
    l: node.dimensions[1],
  };
  const childrenBox = {
    x: padding,
    z: padding,
    w: node.dimensions[0] - 2 * padding,
    l: node.dimensions[1] - 2 * padding,
  };
  const squarified = squarify(childrenBox, _.map(node.children, 'area'));
  const placed: (TPlacedTreeNode | TPlacedTreeLeaf)[] = _.map(
    node.children,
    (child, index) => {
      const place = squarified[index];
      const margin =
        (place.w +
          place.l -
          Math.sqrt((place.w - place.l) ** 2 + (4 * child.area) / MARGIN_MUL)) /
        4;
      return {
        ...child,
        dimensions: [place.w - 2 * margin, place.l - 2 * margin],
        position: [place.x + margin, place.z + margin],
      };
    },
  );

  const geometry = [
    node.dimensions[0],
    node.height,
    node.dimensions[1],
  ] as const;
  return (
    <mesh
      {...props}
      position={[
        position[0] + node.position[0] + geometry[0] / 2,
        position[1] + geometry[1] / 2,
        position[2] + node.position[1] + geometry[2] / 2,
      ]}
    >
      <boxGeometry args={geometry} />
      <meshBasicMaterial wireframe={true} transparent opacity={0} />
      {_.map(placed, child =>
        isLeaf(child) ? (
          <TreeLeaf
            position={[-geometry[0] / 2, -geometry[1] / 2, -geometry[2] / 2]}
            leaf={child}
          />
        ) : (
          <TreeNode
            position={[-geometry[0] / 2, -geometry[1] / 2, -geometry[2] / 2]}
            node={child}
          />
        ),
      )}
      <Border
        position={[-geometry[0] / 2, -geometry[1] / 2, -geometry[2] / 2]}
        name={node.name}
        inner={childrenBox}
        outer={fullBox}
      />
      <Html center position={[0, -geometry[1] / 2 + 0.2, -geometry[2] / 2]}>
        <span className="label">{node.name}</span>
      </Html>
    </mesh>
  );
};
