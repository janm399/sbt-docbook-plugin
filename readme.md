DocBook Plugin for sbt
======================

This project is a plugin for [sbt 0.10](https://github.com/harrah/xsbt)
(and above) providing tasks to transform DocBook XML files to various output
formats like HTML, PDF, EPUB, and others.

The main advantage of this plugin is that you don't need to have the DocBook
stylesheets installed nor do you need any other tools to create advanced
formats like PDF. It all runs out of the box!

Usage
-----

1. Install [sbt 0.10](https://github.com/harrah/xsbt)
2. Create a new directory for your project&mdash;e.g. `/home/user/paper`.
3. In this directory, create a subdirectory where all your DocBook XML files
   will go to. By default, the plugin automatically searches the subdirectory
   `src/main/docbook`.
4. In the same directory, create another subdirectory `project/plugins`. Create
   a file called `build.sbt` in this directory with the following contents:
  <pre>
//yet to be published:
libraryDependencies += "de.undercouch" %% "sbt-docbook-plugin" % "1.0"
</pre>
5. Write a DocBook file
6. Execute one of the sbt tasks below and have fun!

This files will be written to the `target` subdirectory.

You can also have a look at the [examples](https://github.com/michel-kraemer/sbt-docbook-plugin/tree/master/examples)
directory to get a better starting point.

Tasks
-----

Change to your project's root directory and run one of the following commands:

### PDF

    sbt pdf

> Transforms DocBook XML files to PDF files

    sbt xsl-fo

> Transforms DocBook XML files to XSL-FO files. Note that you can also use
> the `sbt pdf` task to create PDF files directly.

### HTML

    sbt html

> Transforms DocBook XML files to HTML files

    sbt html-chunk

> Transforms DocBook XML files to HTML files (each one chunked into multiple
> files)

    sbt html-onechunk

> Transforms DocBook XML files to HTML files (chunked output in single files)

### XHTML

    sbt xhtml
    sbt xhtml-chunk
    sbt xhtml-onechunk

> Just like `sbt html`, `sbt html-chunk` and `sbt html-onechunk` but with
> XHTML 1.0 output.

    sbt xhtml11
    sbt xhtml11-chunk
    sbt xhtml11-onechunk

> Just like `sbt html`, `sbt html-chunk` and `sbt html-onechunk` but with
> stricter XHTML 1.1 output.

### Open eBook

    sbt epub

> Transforms DocBook XML files to Open eBook (EPUB) files

### Help formats

    sbt html-help

> Transforms DocBook XML files to HTML Help files

    sbt java-help

> Transforms DocBook XML files to JavaHelp files

    sbt eclipse-help

> Transforms DocBook XML files to Eclipse Help files

    sbt manpage

> Transforms DocBook XML files to man pages

Settings you may override
-------------------------

There are several settings you may override to customize document
transformation. You can override settings by adding the new values to a
file called `build.sbt` which you have to place in your project's root
directory. Please do not mix that file up with the one under `project/plugins`!

Example:

    // manually specify source files
    mainDocBookFiles in DocBook := Seq(file("src/file1.xml"), file("src/file2.xml"))
    
    // use local DocBook stylesheets
    docBookStyleSheetBase in DocBook := "/home/user/docbook-xsl-1.76.1/"
    
    // use a custom stylesheet for XSL-FO
    docBookXslFoStyleSheet in DocBook := "/home/user/paper/mystylesheet.xsl"

Note that each setting must be written in its own line. Settings must be
separated by a blank line.

    mainDocBookFiles

> A sequence of source files. If you don't overwrite this setting, the plugin
> will search the subdirectory `src/main/docbook` for `.xml` files. If it
> finds exactly one file, it uses this as source. If it finds multiple files,
> it uses the one called `main.xml` as the main source file. If you want to
> convert multiple main source files, you have to override this setting.

    docBookSourceDirectory

> The default directory the plugin should search for `.xml` files. By default
> this is `src/main/docbook`

    docBookStyleSheetBase

> A directory or a URI where the DocBook stylesheets are stored. By default,
> the plugin loads the stylesheets from the Internet dynamically. You can
> speed up document transformation significantly if you
> [download the files](http://sourceforge.net/projects/docbook/files/docbook-xsl/1.76.1/)
> to your local hard drive.

    docBookXslFoStyleSheet
    docBookHtmlStyleSheet
    docBookHtmlChunkStyleSheet
    docBookHtmlOnechunkStyleSheet
    docBookXHtmlStyleSheet
    docBookXHtmlChunkStyleSheet
    docBookXHtmlOnechunkStyleSheet
    docBookXHtml11StyleSheet
    docBookXHtml11ChunkStyleSheet
    docBookXHtml11OnechunkStyleSheet
    docBookEpubStyleSheet
    docBookHtmlHelpStyleSheet
    docBookJavaHelpStyleSheet
    docBookEclipseHelpStyleSheet
    docBookManpageStyleSheet

> Override these settings to specify custom stylesheets for the particular
> output formats.

Implementation details
----------------------

This plugin automatically downloads all libraries needed to transform your
DocBook files. It uses the [SAXON XSLT processor](http://saxon.sourceforge.net)
and [Apache FOP](http://xmlgraphics.apache.org/fop).

License
-------

The docbook plugin for sbt has been released under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) (the
"License").

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
