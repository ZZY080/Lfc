module.exports = {
  apps: [
    {
      name: 'lfc-nest-dev',
      script: 'dist/main.js',
      env: {
        NODE_ENV: 'development',
      },
    },
    {
      name: 'lfc-nest-staging',
      script: 'dist/main.js',
      env: {
        NODE_ENV: 'staging',
      },
    },
    {
      name: 'lfc-nest-prod',
      script: 'dist/main.js',
      env: {
        NODE_ENV: 'production',
      },
    },
  ],
};
