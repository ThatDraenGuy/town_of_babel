import { useState } from 'react';

import { OrbitControls as Controls } from '@react-three/drei';
import { Canvas, type ThreeElements } from '@react-three/fiber';
import { Container, Fullscreen, Text } from '@react-three/uikit';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@react-three/uikit-default';
import _ from 'lodash';
import { Color } from 'three';

import { babelApi } from '../../api/babelApi';
import { CommitCard } from '../../components/commit-card/commit-card';
import { PaginationPicker } from '../../components/pagination-picker/pagination-picker';
import type { TTreeNode } from '../../treemap/algorithm/tree';
import { TreeMap } from '../../treemap/components/tree-map';
import type { TCommitMetaData } from '../../types/commit';

const Plane = (props: ThreeElements['mesh']) => {
  return (
    <mesh {...props} rotation={[-Math.PI / 2, 0, 0]}>
      <planeGeometry args={[50, 50]} />
      <meshStandardMaterial color="gray" />
    </mesh>
  );
};

type TResolvedCommitData = {
  tree: TTreeNode;
  commit: TCommitMetaData;
};

const commits: TResolvedCommitData[] = [
  {
    commit: {
      hash: '9e1e694',
      msg: 'initial commit',
      date: '29.10.2025',
      author: 'ThatDraenGuy',
    },
    tree: {
      name: 'ru.draen.test',
      type: 'node',
      children: [
        {
          name: 'Main.java',
          type: 'node',
          children: [
            {
              name: 'main',
              type: 'leaf',
              area: 1,
              height: 1,
              color: Color.NAMES.green,
            },
          ],
        },
      ],
    },
  },
  {
    commit: {
      hash: '063438e',
      msg: 'feat: first classes',
      date: '30.10.2025',
      author: 'ThatDraenGuy',
    },
    tree: {
      name: 'ru.draen.test',
      type: 'node',
      children: [
        {
          name: 'Main.java',
          type: 'node',
          children: [
            {
              name: 'main',
              type: 'leaf',
              area: 2,
              height: 1,
              color: Color.NAMES.yellow,
            },
            {
              name: 'processData',
              type: 'leaf',
              area: 4,
              height: 2,
              color: Color.NAMES.green,
            },
          ],
        },
      ],
    },
  },
  {
    commit: {
      hash: '543203f',
      msg: 'feat: lab done',
      date: '02.11.2025',
      author: 'ThatDraenGuy',
    },
    tree: {
      name: 'ru.draen.test',
      type: 'node',
      children: [
        {
          name: 'Main.java',
          type: 'node',
          children: [
            {
              name: 'main',
              type: 'leaf',
              area: 2,
              height: 1,
              color: Color.NAMES.yellow,
            },
            {
              name: 'processData',
              type: 'leaf',
              area: 4,
              height: 2,
              color: Color.NAMES.green,
            },
          ],
        },
        {
          name: 'Processor.java',
          type: 'node',
          children: [
            {
              name: 'read',
              type: 'leaf',
              area: 4,
              height: 1,
              color: Color.NAMES.red,
            },
            {
              name: 'close',
              type: 'leaf',
              area: 4,
              height: 2,
              color: Color.NAMES.green,
            },
            {
              name: 'write',
              type: 'leaf',
              area: 4,
              height: 3,
              color: Color.NAMES.yellow,
            },
            {
              name: 'append',
              type: 'leaf',
              area: 1,
              height: 4,
              color: Color.NAMES.green,
            },
            {
              name: 'flush',
              type: 'leaf',
              area: 1,
              height: 5,
              color: Color.NAMES.red,
            },
          ],
        },
        {
          name: 'Utils.java',
          type: 'node',
          children: [
            {
              name: 'capitalizeLetters',
              type: 'leaf',
              area: 4,
              height: 2,
              color: Color.NAMES.green,
            },
          ],
        },
      ],
    },
  },
];

export const TimelinePage: React.FC = () => {
  const { data } = babelApi.useGetLanguagesQuery();
  const [page, setPage] = useState(0);
  return (
    <Canvas>
      <Fullscreen>
        <Container
          padding={16}
          width="100%"
          height="100%"
          flexDirection="column-reverse"
        >
          <PaginationPicker
            currentPage={page}
            setCurrentPage={setPage}
            total={_.size(commits)}
            itemRender={index => (
              <Tooltip>
                <TooltipTrigger>
                  <Text>{commits[index].commit.hash}</Text>
                </TooltipTrigger>
                <TooltipContent>
                  <Text>{commits[index].commit.msg}</Text>
                </TooltipContent>
              </Tooltip>
            )}
          />
          <CommitCard commit={commits[page].commit} />
        </Container>
      </Fullscreen>
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
      <TreeMap position={[0, 0, 0]} tree={commits[page].tree} />
      <Controls />
    </Canvas>
  );
};
