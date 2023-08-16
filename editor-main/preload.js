/* This is written in JavaScript because Electron loads preload script
 * in a special sandbox that prevents it from being decomposed in multiple
 * modules.
 */

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('fileService', {
  showSaveNewProjectDialog: () => ipcRenderer.invoke('showSaveNewProjectDialog')
});
