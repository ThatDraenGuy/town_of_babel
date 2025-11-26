import './app.css';
import { OrbitControls as Controls } from '@react-three/drei';
import { Canvas, type ThreeElements } from '@react-three/fiber';

import { TreeMap } from './treemap/components/tree-map';

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
