import { useMemo } from 'react';

import type { ThreeElements } from '@react-three/fiber';
import { Color, Shape } from 'three';

import type { TBox } from '../algorithm/squarify';

type TProps = Omit<ThreeElements['mesh'], 'position'> & {
  position: [x: number, y: number, z: number];
  name: string;
  inner: TBox;
  outer: TBox;
};

export const Border: React.FC<TProps> = ({
  inner,
  outer,
  name,
  position,
  ...props
}) => {
  const shape = useMemo(() => {
    const outerShape = new Shape();
    outerShape.moveTo(outer.x, outer.z);
    outerShape.lineTo(outer.x + outer.w, outer.z);
    outerShape.lineTo(outer.x + outer.w, outer.z + outer.l);
    outerShape.lineTo(outer.x, outer.z + outer.l);
    outerShape.lineTo(outer.x, outer.z);

    const innerShape = new Shape();
    innerShape.moveTo(inner.x, inner.z);
    innerShape.lineTo(inner.x + inner.w, inner.z);
    innerShape.lineTo(inner.x + inner.w, inner.z + inner.l);
    innerShape.lineTo(inner.x, inner.z + inner.l);
    innerShape.lineTo(inner.x, inner.z);

    outerShape.holes.push(innerShape);
    return outerShape;
  }, [inner, outer]);

  const depth = 0.2;
  const settings = {
    steps: 1,
    depth,
    bevelEnabled: false,
  };

  return (
    <mesh
      {...props}
      position={[position[0], position[1] + depth, position[2]]}
      rotation={[Math.PI / 2, 0, 0]}
    >
      <extrudeGeometry args={[shape, settings]} />
      <meshStandardMaterial color={Color.NAMES.black} />
    </mesh>
  );
};
