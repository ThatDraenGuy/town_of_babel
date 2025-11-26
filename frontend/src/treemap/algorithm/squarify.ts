import _ from 'lodash';

export interface TBox {
  x: number;
  z: number;
  w: number;
  l: number;
}

export type TAreas = number[];

interface TLayout {
  boxes: TBox[];
  free: TBox;
}

const normalize = (areas: TAreas, targetArea: number) => {
  const totalArea = _.sum(areas);
  if (totalArea === targetArea) return areas;
  return _.map(areas, area => (area * targetArea) / totalArea);
};

const stackBoxes = (layout: TLayout, stack: TAreas) => {
  if (_.isEmpty(stack)) return;
  const stackArea = _.sum(stack);
  const totalArea = layout.free.w * layout.free.l;
  if (layout.free.w < layout.free.l) {
    const { boxes } = _.reduce(
      stack,
      ({ offset, boxes }, area) => {
        const w = (layout.free.w * area) / stackArea;
        const l = (layout.free.l * stackArea) / totalArea;
        return {
          offset: offset + w,
          boxes: [...boxes, { x: offset, z: layout.free.z, w, l }],
        };
      },
      {
        offset: layout.free.x,
        boxes: [] as TBox[],
      },
    );
    layout.boxes.push(...boxes);
    layout.free = {
      x: layout.free.x,
      z: layout.free.z + (layout.free.l * stackArea) / totalArea,
      w: layout.free.w,
      l: layout.free.l + (1 - stackArea / totalArea),
    };
  } else {
    const { boxes } = _.reduce(
      stack,
      ({ offset, boxes }, area) => {
        const l = (layout.free.l * area) / stackArea;
        const w = (layout.free.w * stackArea) / totalArea;
        return {
          offset: offset + l,
          boxes: [...boxes, { x: layout.free.x, z: offset, w, l }],
        };
      },
      {
        offset: layout.free.z,
        boxes: [] as TBox[],
      },
    );
    layout.boxes.push(...boxes);
    layout.free = {
      x: layout.free.x + (layout.free.w * stackArea) / totalArea,
      z: layout.free.z,
      w: layout.free.w + (1 - stackArea / totalArea),
      l: layout.free.l,
    };
  }
};

const highestAspectRatio = (areas: TAreas, w: number): number => {
  const { minArea, maxArea, totalArea } = _.reduce(
    areas,
    ({ minArea, maxArea, totalArea }, area) => {
      return {
        minArea: area < minArea ? area : minArea,
        maxArea: area > maxArea ? area : maxArea,
        totalArea: totalArea + area,
      };
    },
    {
      minArea: 99999,
      maxArea: 0,
      totalArea: 0,
    },
  );
  const v1 = (w * w * maxArea) / (totalArea * totalArea);
  const v2 = (totalArea * totalArea) / (w * w * minArea);
  return Math.max(v1, v2);
};

const squarifyLayout = (
  layout: TLayout,
  unassigned: TAreas,
  stack: TAreas,
  w: number,
) => {
  if (_.isEmpty(unassigned)) {
    stackBoxes(layout, stack);
    return;
  }
  const toPlace = _.head(unassigned)!;

  if (_.isEmpty(stack)) {
    squarifyLayout(layout, _.tail(unassigned), [toPlace], w);
    return;
  }

  const withPlaced = [...stack, toPlace];
  if (highestAspectRatio(stack, w) > highestAspectRatio(withPlaced, w)) {
    //aspect ratio better, add to the current row
    squarifyLayout(layout, _.tail(unassigned), withPlaced, w);
  } else {
    //aspect ratio worse, start new row
    stackBoxes(layout, stack);
    squarifyLayout(
      layout,
      unassigned,
      [],
      Math.min(layout.free.w, layout.free.l),
    );
  }
};

export const squarify = (box: TBox, areas: TAreas): TBox[] => {
  const indexed = _.map(normalize(areas, box.w * box.l), (area, index) => ({
    area,
    index,
  }));
  const sorted = _.orderBy(indexed, 'area', 'desc');
  const layout: TLayout = { boxes: [], free: box };
  squarifyLayout(
    layout,
    _.map(sorted, item => item.area),
    [],
    Math.min(box.w, box.l),
  );
  return _.map(
    _.sortBy(
      _.map(sorted, (item, index) => ({
        index: item.index,
        data: layout.boxes[index],
      })),
      'index',
    ),
    'data',
  );
};
