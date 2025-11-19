export interface HouseData {
  name: string;
  area: number;
  height: number;
}

export interface HouseBlockData {
  name: string;
  houses: HouseData[];
}

export type BlockData =
  | (HouseBlockData & { type: 'houses' })
  | {
      name: string;
      blocks: BlockData[];
      type: 'blocks';
    };

export const isHouse = (
  data: BlockData,
): data is HouseBlockData & { type: 'houses' } => {
  return data.type === 'houses';
};

export interface ResolvedHouseData {
  name: string;
  dimensions: [width: number, height: number, length: number];
  position: [x: number, z: number];
}

export interface ResolvedHouseBlockData {
  name: string;
  houses: ResolvedHouseData[];
  dimensions: [width: number, height: number, length: number];
}

export type ResolvedBlockData =
  | (ResolvedHouseBlockData & { type: 'houses' })
  | {
      name: string;
      blocks: ResolvedBlockData[];
      dimensions: [width: number, height: number, length: number];
      type: 'blocks';
    };

export type HouseBlockResolver = (
  block: HouseBlockData,
) => ResolvedHouseBlockData;

export type BlockResolver = (block: BlockData) => ResolvedBlockData;
