const path = require('path');

module.exports = {
  dependency: {
    platforms: {
      ios: { podspecPath: path.join(__dirname, 'ios', 'RNZoomUs.podspec') }
    },
  },
};
