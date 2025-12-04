import { Text } from '@react-three/uikit';
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@react-three/uikit-default';

import type { TCommitMetaData } from '../../types/commit';

interface TProps {
  commit: TCommitMetaData;
}

export const CommitCard: React.FC<TProps> = ({ commit }) => {
  return (
    <Card width={300}>
      <CardHeader>
        <CardTitle>
          <Text>{commit.msg}</Text>
        </CardTitle>
      </CardHeader>
      <CardDescription>
        <Text margin={8}>{commit.hash}</Text>
        <Text margin={8}>{commit.author}</Text>
        <Text margin={8}>{commit.date}</Text>
      </CardDescription>
    </Card>
  );
};
