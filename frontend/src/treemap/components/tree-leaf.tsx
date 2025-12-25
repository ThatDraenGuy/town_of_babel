import { useState } from 'react';

import { Html, Outlines } from '@react-three/drei';
import type { ThreeElements } from '@react-three/fiber';

import type { TPlacedTreeLeaf } from '../algorithm/tree';

type TProps = Omit<ThreeElements['mesh'], 'position'> & {
  position: [x: number, y: number, z: number];
  leaf: TPlacedTreeLeaf;
  onEnter?: () => void;
  onLeave?: () => void;
  onClick?: () => void;
  selected?: string;
  setSelected: (id: string) => void;
};

export const TreeLeaf: React.FC<TProps> = ({
  position,
  leaf,
  onEnter,
  onLeave,
  onClick,
  selected,
  setSelected,
  ...props
}) => {
  const [hovered, setHovered] = useState(false);
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
      onPointerEnter={e => {
        e.stopPropagation();
        setHovered(true);
        onEnter?.();
      }}
      onPointerLeave={e => {
        e.stopPropagation();
        setHovered(false);
        onLeave?.();
      }}
      onClick={e => {
        e.stopPropagation();
        setSelected(leaf.id);
        onClick?.();
      }}
    >
      <boxGeometry args={geometry} />
      <meshStandardMaterial color={leaf.color} />
      {(hovered || selected === leaf.id) && (
        <Outlines thickness={3} color="white" />
      )}
      {hovered && (
        <Html center position={[0, geometry[1] / 2 + 0.5, 0]}>
          <span className="label">{leaf.name}</span>
        </Html>
      )}
    </mesh>
  );
};
