import { useRef, useState } from 'react';

import './app.css';
import { OrbitControls as Controls } from '@react-three/drei';
import { Canvas, useFrame, type ThreeElements } from '@react-three/fiber';
import * as THREE from 'three';

import { City } from './city/components/city';
import {
  GappedHouseBlockResolver,
  SimpleBlockResolver,
  SimpleSplitResolver,
} from './city/helpers/house-placement';

const Box = (props: ThreeElements['mesh']) => {
  const meshRef = useRef<THREE.Mesh>(null!);
  const [hovered, setHover] = useState(false);
  const [active, setActive] = useState(false);
  useFrame((_state, delta) => (meshRef.current.rotation.x += delta));
  return (
    <mesh
      {...props}
      ref={meshRef}
      scale={active ? 1.5 : 1}
      onClick={_event => setActive(!active)}
      onPointerOver={_event => setHover(true)}
      onPointerOut={_event => setHover(false)}
    >
      <boxGeometry args={[1, 1, 1]} />
      <meshStandardMaterial color={hovered ? 'hotpink' : '#2f74c0'} />
    </mesh>
  );
};

const Plane = (props: ThreeElements['mesh']) => {
  return (
    <mesh {...props} rotation={[-Math.PI / 2, 0, 0]}>
      <planeGeometry args={[10, 10]} />
      <meshStandardMaterial color="white" />
    </mesh>
  );
};

function App() {
  return (
    <Canvas>
      <ambientLight intensity={Math.PI / 2} />
      <spotLight
        position={[10, 10, 10]}
        angle={0.15}
        penumbra={1}
        decay={0}
        intensity={Math.PI}
      />
      <pointLight position={[-10, -10, -10]} decay={0} intensity={Math.PI} />
      <Plane position={[0, 0, 0]} />
      <City
        position={[0, 0, 0]}
        data={SimpleBlockResolver(
          GappedHouseBlockResolver(SimpleSplitResolver),
        )({
          name: 'rootPackage',
          blocks: [
            {
              name: 'Main.java',
              houses: [
                {
                  name: 'main',
                  area: 2,
                  height: 3,
                },
                {
                  name: 'process',
                  area: 1,
                  height: 1,
                },
              ],
              type: 'houses',
            },
            {
              name: 'TACVisitor',
              houses: [
                {
                  name: 'defaultAction',
                  area: 1,
                  height: 1,
                },
                {
                  name: 'handleBinary',
                  area: 2,
                  height: 2,
                },
                {
                  name: 'visit',
                  area: 10,
                  height: 10,
                },
              ],
              type: 'houses',
            },
            {
              name: 'tac',
              blocks: [
                {
                  name: 'ScopeContext',
                  houses: [
                    {
                      name: 'enterScope',
                      area: 2,
                      height: 1,
                    },
                  ],
                  type: 'houses',
                },
              ],
              type: 'blocks',
            },
            // {
            //   name: 'Main.java',
            //   houses: [
            //     {
            //       name: 'test1',
            //       area: 2,
            //       height: 3,
            //     },
            //     {
            //       name: 'test2',
            //       area: 5,
            //       height: 2,
            //     },
            //     {
            //       name: 'test3',
            //       area: 3,
            //       height: 1,
            //     },
            //     {
            //       name: 'test4',
            //       area: 1,
            //       height: 5,
            //     },
            //     {
            //       name: 'test5',
            //       area: 6,
            //       height: 5,
            //     },
            //   ],
            //   type: 'houses',
            // },
            // {
            //   name: 'Main2.java',
            //   houses: [
            //     {
            //       name: 'test1',
            //       area: 2,
            //       height: 3,
            //     },
            //   ],
            //   type: 'houses',
            // },
            // {
            //   name: 'Main3.java',
            //   houses: [
            //     {
            //       name: 'test1',
            //       area: 1,
            //       height: 4,
            //     },
            //   ],
            //   type: 'houses',
            // },
            // {
            //   name: 'innerPackage',
            //   blocks: [
            //     {
            //       name: 'Example.java',
            //       houses: [
            //         {
            //           name: 'test1',
            //           area: 20,
            //           height: 3,
            //         },
            //       ],
            //       type: 'houses',
            //     },
            //     {
            //       name: 'Example2.java',
            //       houses: [
            //         {
            //           name: 'test1',
            //           area: 10,
            //           height: 2,
            //         },
            //       ],
            //       type: 'houses',
            //     },
            //   ],
            //   type: 'blocks',
            // },
          ],
          type: 'blocks',
        })}
      />
      <Controls />
    </Canvas>
  );
}

export default App;
