import type { ThreeElements } from '@react-three/fiber';

import { House, HouseColor } from './house';

export enum HouseStatus {
  SUCCESS,
  WARNING,
  ERROR,
}

export type TProps = Omit<ThreeElements['mesh'], 'position'> & {
  position: [x: number, y: number, z: number];
};

const CELL_SIZE = 1.25;

const STATUS_MAP = {
  [HouseStatus.SUCCESS]: HouseColor.GREEN,
  [HouseStatus.WARNING]: HouseColor.YELLOW,
  [HouseStatus.ERROR]: HouseColor.RED,
};

export const City: React.FC<TProps> = ({
  position: rootPosition,
  ...props
}) => {
  return <></>;
};
