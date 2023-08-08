/* This is written in JavaScript because Electron loads preload script
 * in a special sandbox that prevents it from being decomposed in multiple
 * modules.
 */

const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('versions', {
  node: () => process.versions.node,
  chrome: () => process.versions.chrome,
  electron: () => process.versions.electron
});
