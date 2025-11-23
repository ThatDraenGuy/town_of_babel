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
    position: [0, 0],
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

const BLOCK_BORDER = 0.7;
const BLOCK_GAP = 0.4;

export const SimpleBlockResolver = (
  houseBlockResolver: HouseBlockResolver,
): BlockResolver => {
  return data => {
    if (isHouse(data))
      return {
        ...houseBlockResolver(data),
        type: 'houses',
      };
    const resolved = _.map(
      data.blocks,
      SimpleBlockResolver(houseBlockResolver),
    );
    const sorted = _.sortBy(resolved, 'name');
    const maxWidth =
      _.maxBy(resolved, item => item.dimensions[0])?.dimensions[0] ?? 0;

    const maxHeight =
      _.maxBy(resolved, item => item.dimensions[1])?.dimensions[1] ?? 0;

    const levelled = _.reduce(
      sorted,
      (
        acc: { length: number; width: number; levels: ResolvedBlockData[][] },
        block,
      ) => {
        const currLevel = _.last(acc.levels);
        if (acc.width + block.dimensions[0] + BLOCK_GAP > maxWidth) {
          const maxLevelLength =
            _.maxBy(currLevel, item => item.dimensions[2])?.dimensions[2] ?? 0;
          return {
            length: acc.length + maxLevelLength,
            width: block.dimensions[0],
            levels: [
              ...acc.levels,
              [
                {
                  ...block,
                  position: [
                    0 + BLOCK_BORDER,
                    acc.length +
                      maxLevelLength +
                      BLOCK_BORDER +
                      _.size(acc.levels) * BLOCK_GAP,
                  ] as [number, number],
                },
              ],
            ],
          };
        }

        currLevel?.push({
          ...block,
          position: [
            acc.width + BLOCK_BORDER + _.size(currLevel) * BLOCK_GAP,
            acc.length + BLOCK_BORDER + (_.size(acc.levels) - 1) * BLOCK_GAP,
          ],
        });
        return {
          ...acc,
          width: acc.width + block.dimensions[0],
        };
      },
      { length: 0, width: 0, levels: [[]] },
    );

    const actualLength =
      levelled.length +
      (_.maxBy(_.last(levelled.levels), item => item.dimensions[2])
        ?.dimensions[2] ?? 0);
    return {
      name: data.name,
      blocks: _.flatten(levelled.levels),
      dimensions: [
        maxWidth + 2 * BLOCK_BORDER,
        maxHeight,
        actualLength +
          2 * BLOCK_BORDER +
          (_.size(levelled.levels) - 1) * BLOCK_GAP,
      ],
      position: [0, 0],
      type: data.type,
    };
  };
};
