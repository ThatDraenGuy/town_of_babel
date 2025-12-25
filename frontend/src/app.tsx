import { createBrowserRouter, RouterProvider } from 'react-router';

import './app.css';
import { ConfigProvider, theme } from 'antd';

import { TimelinePage } from './pages/timeline/timeline';
import { WelcomePage } from './pages/welcome/welcome';

const router = createBrowserRouter([
  {
    path: '/',
    index: true,
    element: <WelcomePage />,
  },
  {
    path: '/projects/:projectId/:branch',
    element: <TimelinePage />,
  },
]);

function App() {
  return (
    <ConfigProvider
      theme={{
        algorithm: theme.darkAlgorithm,
      }}
    >
      <RouterProvider router={router} />
    </ConfigProvider>
  );
}

export default App;
