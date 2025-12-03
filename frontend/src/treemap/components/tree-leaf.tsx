import type { ThreeElements } from '@react-three/fiber';

import type { TPlacedTreeLeaf } from '../algorithm/tree';

type TProps = Omit<ThreeElements['mesh'], 'position'> & {
  position: [x: number, y: number, z: number];
  leaf: TPlacedTreeLeaf;
};

export const TreeLeaf: React.FC<TProps> = ({ position, leaf, ...props }) => {
  const geometry = [
    leaf.dimensions[0],
    leaf.height,
    leaf.dimensions[1],
  ] as const;
  return (
    <mesh
      {...props}
      position={[
        position[0] + leaf.position[0] + geometry[0] / 2,
        position[1] + geometry[1] / 2,
        position[2] + leaf.position[1] + geometry[2] / 2,
      ]}
    >
      <boxGeometry args={geometry} />
      <meshStandardMaterial color={leaf.color} />
    </mesh>
  );
};
