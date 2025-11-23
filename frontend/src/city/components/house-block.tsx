import type { ThreeElements } from '@react-three/fiber';
import _ from 'lodash';
import { Color } from 'three';

import type { ResolvedHouseBlockData } from '../types/house';

import { House, HouseColor } from './house';

export type TProps = Omit<ThreeElements['mesh'], 'position'> & {
  data: ResolvedHouseBlockData;
  position: [x: number, y: number, z: number];
};

const PLATFORM_HEIGHT = 0.2;

export const HouseBlock: React.FC<TProps> = ({ data, position, ...props }) => {
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
      <meshBasicMaterial transparent opacity={0} />
      {_.map(data.houses, house => (
        <House
          color={HouseColor.GREEN}
          position={[
            -data.dimensions[0] / 2,
            -data.dimensions[1] / 2 + PLATFORM_HEIGHT,
            -data.dimensions[2] / 2,
          ]}
          data={house}
        />
      ))}
      <mesh position={[0, PLATFORM_HEIGHT / 2 - data.dimensions[1] / 2, 0]}>
        <boxGeometry
          args={[data.dimensions[0], PLATFORM_HEIGHT, data.dimensions[2]]}
        />
        <meshBasicMaterial color={Color.NAMES.black} />
      </mesh>
    </mesh>
  );
};
