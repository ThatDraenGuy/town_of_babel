import { useRef, useState } from 'react';

import './app.css';
import { OrbitControls as Controls } from '@react-three/drei';
import { Canvas, useFrame, type ThreeElements } from '@react-three/fiber';
import * as THREE from 'three';

import { TreeMap } from './treemap/components/tree-map';

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
      <TreeMap
        position={[0, 0, 0]}
        tree={{
          name: 'rootPackage',
          type: 'node',
          children: [
            { name: 'Main.java', type: 'leaf', area: 4, height: 3 },
            {
              name: 'package',
              type: 'node',
              children: [
                { name: 'Main.java', type: 'leaf', area: 4, height: 1 },
                { name: 'Main.java', type: 'leaf', area: 4, height: 2 },
                { name: 'Main.java', type: 'leaf', area: 4, height: 3 },
                { name: 'Main.java', type: 'leaf', area: 1, height: 4 },
                { name: 'Main.java', type: 'leaf', area: 1, height: 5 },
              ],
            },
            {
              name: 'package',
              type: 'node',
              children: [
                { name: 'Main.java', type: 'leaf', area: 4, height: 2 },
              ],
            },
          ],
        }}
      />
      <Controls />
    </Canvas>
  );
}

export default App;
