import _ from 'lodash';

import {
  isHouse,
  type BlockResolver,
  type HouseBlockResolver,
  type ResolvedBlockData,
  type ResolvedHouseData,
} from '../types/house';

const MIN_WIDTH = 1;

export const SimpleSplitResolver: HouseBlockResolver = data => {
  const houses = data.houses;
  const sorted = _.sortBy(houses, 'name');
  const totalArea = _.sumBy(houses, 'area');
  const sideLen = Math.sqrt(totalArea);
  const resolved = _.reduce(
    sorted,
    (
      acc: { width: number; height: number; houses: ResolvedHouseData[] },
      house,
    ) => {
      const width = house.area / sideLen;
      const actualWidth = width > MIN_WIDTH ? width : MIN_WIDTH;
      const actualLength = width > MIN_WIDTH ? sideLen : house.area / MIN_WIDTH;
      acc.houses.push({
        name: house.name,
        dimensions: [actualWidth, house.height, actualLength],
        position: [acc.width, 0],
      });
      return {
        width: acc.width + actualWidth,
        height: house.height > acc.height ? house.height : acc.height,
        houses: acc.houses,
      };
    },
    { width: 0, height: 0, houses: [] },
  );
  return {
    name: data.name,
    houses: resolved.houses,
    dimensions: [resolved.width, resolved.height, sideLen],
  };
};

const HOUSE_GAP = 0.2;

export const GappedHouseBlockResolver = (
  actualResolver: HouseBlockResolver,
): HouseBlockResolver => {
  return data => {
    const resolved = actualResolver(data);
    return {
      ...resolved,
      houses: _.map(resolved.houses, (house, index) => ({
        ...house,
        position: [
          house.position[0] + (index + 1) * HOUSE_GAP,
          house.position[1] + HOUSE_GAP,
        ],
      })),
      dimensions: [
        resolved.dimensions[0] + (_.size(resolved.houses) + 1) * HOUSE_GAP,
        resolved.dimensions[1],
        resolved.dimensions[2] + 2 * HOUSE_GAP,
      ],
    };
  };
};

// export const SimpleBlockResolver = (
//   houseBlockResolver: HouseBlockResolver,
// ): BlockResolver => {
//   return data => {
//     if (isHouse(data)) return houseBlockResolver(data);
//     const resolved = _.map(
//       data.blocks,
//       SimpleBlockResolver(houseBlockResolver),
//     );
//     const sorted = _.sortBy(resolved, 'name');
//     const maxWidth = _.maxBy(resolved, item => item.dimensions[0]);
//     const totalWidth = _.sumBy(resolved, item => item.dimensions[0]);

//     _.reduce(
//       sorted,
//       (acc: ResolvedBlockData[][], block) => {

//         return [[]];
//       },
//       [[]],
//     );
//   };
// };
