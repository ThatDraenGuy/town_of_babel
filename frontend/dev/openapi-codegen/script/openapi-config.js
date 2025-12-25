// import type { ConfigFile } from '@rtk-query/codegen-openapi';

const config = {
  schemaFile: '../schema/openapi.yaml',
  apiFile: '../../../src/api/baseApi.ts',
  apiImport: 'baseApi',
  outputFile: '../../../src/api/babelApi.ts',
  exportName: 'babelApi',
  encodePathParams: true,
  hooks: true,
};

export default config;
