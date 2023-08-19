/* This is written in JavaScript because Electron loads preload script
 * in a special sandbox that prevents it from being decomposed in multiple
 * modules.
 */

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('fileService', {
  showOpenProjectDialog: () => ipcRenderer.invoke('showOpenProjectDialog'),
  showSaveNewProjectDialog: () => ipcRenderer.invoke('showSaveNewProjectDialog'),
  readFileToString: (path) => ipcRenderer.invoke('readFileToString', path),
  writeStringToFile: (path, content) => ipcRenderer.invoke('writeStringToFile', path, content)
});
