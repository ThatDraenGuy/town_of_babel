import type { ThreeElements } from '@react-three/fiber';

import { resolveTree, type TTreeNode } from '../algorithm/tree';

import { TreeNode } from './tree-node';

type TProps = Omit<ThreeElements['mesh'], 'position'> & {
  position: [x: number, y: number, z: number];
  tree: TTreeNode;
};

export const TreeMap: React.FC<TProps> = ({ position, tree, ...props }) => {
  const resolved = resolveTree(tree);
  console.log(resolved);
  const geometry = [
    Math.sqrt(resolved.area),
    resolved.height,
    Math.sqrt(resolved.area),
  ] as const;
  return (
    <mesh
      {...props}
      position={[position[0], position[1] + geometry[1] / 2, position[2]]}
    >
      {/* <boxGeometry args={geometry} />
      <meshBasicMaterial transparent opacity={0} /> */}
      <TreeNode
        position={[-geometry[0] / 2, -geometry[1] / 2, -geometry[2] / 2]}
        node={{
          ...resolved,
          dimensions: [geometry[0], geometry[2]],
          position: [0, 0],
        }}
      />
    </mesh>
  );
};
