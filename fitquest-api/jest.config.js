module.exports = {
  testEnvironment: 'node',
  testTimeout: 30000,
  forceExit: true,
  // Inside Jest, the mongodb 7.6.0 driver used by mongoose sends a handshake that
  // MongoDB rejects. Tests use the 7.5.0 copy from mongodb-memory-server instead.
  // This only affects tests; the running API still uses mongoose's own driver.
  moduleNameMapper: {
    '^mongodb$': '<rootDir>/node_modules/mongodb-memory-server-core/node_modules/mongodb',
    '^mongodb/(.*)$': '<rootDir>/node_modules/mongodb-memory-server-core/node_modules/mongodb/$1',
  },
};
