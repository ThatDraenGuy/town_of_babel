// Or from '@reduxjs/toolkit/query' if not using the auto-generated hooks
import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

// initialize an empty api service that we'll inject endpoints into later as needed
export const baseApi = createApi({
  baseQuery: fetchBaseQuery({ baseUrl: 'http://127.0.0.1:8080/api/v1' }),
  endpoints: () => ({}),
});
