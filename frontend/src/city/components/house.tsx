import type { ThreeElements } from '@react-three/fiber';
import { Color } from 'three';

import type { ResolvedHouseData } from '../types/house';

export const HouseColor = {
  GREEN: new Color('green'),
  YELLOW: new Color('yellow'),
  RED: new Color('red'),
};

export type TProps = Omit<ThreeElements['mesh'], 'position'> & {
  color: Color;
  data: ResolvedHouseData;
  position: [x: number, y: number, z: number];
};

export const House: React.FC<TProps> = ({
  color,
  data,
  position,
  ...props
}) => {
  return (
    <mesh
      {...props}
      position={[
        position[0] + data.position[0] + data.dimensions[0] / 2,
        position[1] + data.dimensions[1] / 2,
        position[2] + data.position[1] + data.dimensions[2] / 2,
      ]}
    >
      <boxGeometry args={data.dimensions} />
      <meshStandardMaterial color={color} />
    </mesh>
  );
};
