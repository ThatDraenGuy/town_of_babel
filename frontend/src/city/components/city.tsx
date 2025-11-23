import type { ThreeElements } from '@react-three/fiber';
import _ from 'lodash';

import type { ResolvedBlockData } from '../types/house';

import { HouseColor } from './house';
import { HouseBlock } from './house-block';

export enum HouseStatus {
  SUCCESS,
  WARNING,
  ERROR,
}

export type TProps = Omit<ThreeElements['mesh'], 'position'> & {
  data: ResolvedBlockData;
  position: [x: number, y: number, z: number];
};

const STATUS_MAP = {
  [HouseStatus.SUCCESS]: HouseColor.GREEN,
  [HouseStatus.WARNING]: HouseColor.YELLOW,
  [HouseStatus.ERROR]: HouseColor.RED,
};

export const City: React.FC<TProps> = ({ data, position, ...props }) => {
  if (data.type === 'houses') {
    return <HouseBlock {...props} data={data} position={position} />;
  }
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
      <meshBasicMaterial transparent opacity={0.5} />
      {_.map(data.blocks, block => (
        <City
          position={[
            -data.dimensions[0] / 2,
            -data.dimensions[1] / 2,
            -data.dimensions[2] / 2,
          ]}
          data={block}
        />
      ))}
    </mesh>
  );
};
