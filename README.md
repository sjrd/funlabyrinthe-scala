# FunLabyrinthe in Scala

This is a reimplementation in Scala of my long-lived project [FunLabyrinthe](http://www.funlabyrinthe.com/).
The aim of this reimplementation is to make it more portable, especially in three areas:

* Executable on Linux and Mac OS, in addition to Windows
* Thanks to [Scala.js](https://www.scala-js.org/), the runner is also executable in browsers
* Maybe make the runner executable on mobile devices

I do not expect this project to reach maturity before a long time.
The existing software can be used until then!

## Building and running

If you feel adventurous, you may test the current state of FunLabyrinthe.

### Prerequisites

* Install [scala-cli](https://scala-cli.virtuslab.org/install)
* Install [Scala and sbt](https://www.scala-lang.org/download/)
* Install [Node.js](https://nodejs.org/en/download)

### Additional preparation

This project depends on external resources (images) found in a different repository.
At the moment, the build does not automatically download these resources, so you need to do it yourself.

* Download [funlaby-library.zip](https://codeload.github.com/sjrd/funlaby-library/zip/refs/heads/master).
* Unzip and copy the `Resources` directory into the `editor-renderer` directory of this project (i.e., so that `./editor-render/Resources/` exists).

### Build

In the directory of this project, run:

* `npm install` to install Node.js dependencies
* `sbt -mem 4096 editorMain/fastLinkJS` to actually build the application

### Run

In the directory of this project, run:

* `npm run start`

Created mazes are stored in `<your home dir>/FunLabyDocuments/`.

### Warnings!

There is no protection against closing the application without saving at the moment.
Moreover, Ctrl+S does not work yet.
**Make sure to use the menu File|Save** before leaving the application.
