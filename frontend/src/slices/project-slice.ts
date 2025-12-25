import {
  createSelector,
  createSlice,
  type PayloadAction,
} from '@reduxjs/toolkit';

import type { ProjectResponseDto } from '../api/babelApi';

export interface ProjectState {
  project?: ProjectResponseDto;
  url?: string;
}

const initialState: ProjectState = {
  project: undefined,
  url: undefined,
};

export const projectSlice = createSlice({
  name: 'project',
  initialState: initialState,
  reducers: {
    setProject: (state, action: PayloadAction<ProjectState>) => {
      state.project = action.payload.project;
      state.url = action.payload.url;
    },
  },
});

// export const selectProject = createSelector((state: State) => state.project, item);

export const { setProject } = projectSlice.actions;

export default projectSlice.reducer;
