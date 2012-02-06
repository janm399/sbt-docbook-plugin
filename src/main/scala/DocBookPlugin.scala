// Copyright 2011 Michel Kraemer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.undercouch.sbt.docbook

import sbt._
import Keys._
import java.io.{BufferedOutputStream, File, FileOutputStream}
import Project.Initialize
import org.apache.fop.apps.{Fop, FopFactory}
import org.apache.xmlgraphics.util.MimeConstants
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.sax.SAXResult
import scala.io.Source

/**
 * Provides tasks to compile DocBook XML files to various output formats
 * @author Michel Kraemer
 */
object DocBookPlugin extends Plugin {
  //declare our own configuration scope
  val DocBook = config("DocBook") extend(Compile)
  
  //declare tasks
  val xslFoTask = TaskKey[Seq[File]]("xsl-fo",
      "Transforms DocBook XML files to XSL-FO files")
  val htmlTask = TaskKey[Seq[File]]("html",
      "Transforms DocBook XML files to HTML files")
  val htmlChunkTask = TaskKey[Unit]("html-chunk",
      "Transforms DocBook XML files to HTML files (each one chunked " +
      "into multiple files)")
  val htmlOnechunkTask = TaskKey[Unit]("html-onechunk",
      "Transforms DocBook XML files to HTML files (chunked output " +
      "in single files)")
  val xhtmlTask = TaskKey[Seq[File]]("xhtml",
      "Transforms DocBook XML files to XHTML files")
  val xhtmlChunkTask = TaskKey[Unit]("xhtml-chunk",
      "Transforms DocBook XML files to XHTML files (each one chunked " +
      "into multiple files)")
  val xhtmlOnechunkTask = TaskKey[Unit]("xhtml-onechunk",
      "Transforms DocBook XML files to XHTML files (chunked output " +
      "in single files)")
  val xhtml11Task = TaskKey[Seq[File]]("xhtml11",
      "Transforms DocBook XML files to XHTML 1.1 files")
  val xhtml11ChunkTask = TaskKey[Unit]("xhtml11-chunk",
      "Transforms DocBook XML files to XHTML 1.1 files (each one chunked " +
      "into multiple files)")
  val xhtml11OnechunkTask = TaskKey[Unit]("xhtml11-onechunk",
      "Transforms DocBook XML files to XHTML 1.1 files (chunked output " +
      "in single files)")
  val epubTask= TaskKey[Unit]("epub",
    "Transforms and zips up epub")
  val epubGenerateTask = TaskKey[Unit]("epub-generate",
  "Transforms DocBook XML files to EPUB files")
  val htmlHelpTask = TaskKey[Unit]("html-help",
  "Transforms DocBook XML files to HTML Help files")
  val javaHelpTask = TaskKey[Unit]("java-help",
  "Transforms DocBook XML files to JavaHelp files")
  val eclipseHelpTask = TaskKey[Unit]("eclipse-help",
  "Transforms DocBook XML files to Eclipse Help files")
  val manpageTask = TaskKey[Unit]("manpage",
  "Transforms DocBook XML files to man pages")
  val pdfTask = TaskKey[Seq[File]]("pdf",
  "Transforms DocBook XML files to PDF files (using FOP)")
  val zipTask = TaskKey[Unit]("zipper",
  "zips the output folder")

  //declare settings
  val mainDocBookFiles = SettingKey[Seq[File]]("main-docbook-files")
  val docBookSourceDirectory = SettingKey[File]("docbook-source-directory")
  val docBookTargetDirectory = SettingKey[File]("docbook-target-directory")

  val docBookStyleSheetBase = SettingKey[String]("docbook-stylesheet-base")
  val docBookXslFoStyleSheet = SettingKey[String]("docbook-xslfo-stylesheet")
  val docBookHtmlStyleSheet = SettingKey[String]("docbook-html-stylesheet")
  val docBookHtmlChunkStyleSheet = SettingKey[String]("docbook-htmlchunk-stylesheet")
  val docBookHtmlOnechunkStyleSheet = SettingKey[String]("docbook-htmlonechunk-stylesheet")
  val docBookXHtmlStyleSheet = SettingKey[String]("docbook-xhtml-stylesheet")
  val docBookXHtmlChunkStyleSheet = SettingKey[String]("docbook-xhtmlchunk-stylesheet")
  val docBookXHtmlOnechunkStyleSheet = SettingKey[String]("docbook-xhtmlonechunk-stylesheet")
  val docBookXHtml11StyleSheet = SettingKey[String]("docbook-xhtml11-stylesheet")
  val docBookXHtml11ChunkStyleSheet = SettingKey[String]("docbook-xhtml11chunk-stylesheet")
  val docBookXHtml11OnechunkStyleSheet = SettingKey[String]("docbook-xhtml11onechunk-stylesheet")
  val docBookEpubStyleSheet = SettingKey[String]("docbook-epub-stylesheet")
  val docBookHtmlHelpStyleSheet = SettingKey[String]("docbook-htmlhelp-stylesheet")
  val docBookJavaHelpStyleSheet = SettingKey[String]("docbook-javahelp-stylesheet")
  val docBookEclipseHelpStyleSheet = SettingKey[String]("docbook-eclipsehelp-stylesheet")
  val docBookManpageStyleSheet = SettingKey[String]("docbook-manpage-stylesheet")
  
  val fopConfigFile = SettingKey[Option[String]]("fop-config-file")
  
  private lazy val fopFactory = FopFactory.newInstance()
  
  /**
   * Returns the DocBook files to compile. Searches the directory
   * denoted by <code>source</code> for XML files. If this directory does
   * not contain XML files, searches the other directories denoted by
   * <code>otherSources</code>. If the method finds exactly one XML file in
   * these directories, then it returns a sequence containing exactly this file.
   * If the method finds multiple files or no files at all, it returns a
   * sequence containing the file <code>source/main.xml</code>.
   * @param source the directory to search for XML files first
   * @param otherSources the directories to search if <code>source</code>
   * does not contain XML files
   * @return the list of XML files to compile
   */
  private def getMainDocBookFiles(source: File, otherSources: Seq[File]): Seq[File] = {
    var xmlFiles = (source ** "*.xml").get
    if (xmlFiles.isEmpty) {
      val o = otherSources find { s => !(s ** "*.xml").get.isEmpty }
      xmlFiles = (o map { s => (s ** "*.xml").get }) getOrElse Seq.empty
    }
    if (xmlFiles.size == 1) xmlFiles else Seq(source / "main.xml")
  }
  
  /**
   * Runs a transformation from <code>src</code> to <code>dst</code>.
   * @param src the file to transform
   * @param dst the destination file
   * @param log the logger
   * @param doTransform a function actually performing the transformation. This
   * function should return a code (0 = success, every other value = error)
   */
  private def transform(src: File, dst: File, log: Logger)(doTransform: => Int) {
    log.info("  " + src.getName() + " to " + dst.getName() + " ...")
    
    val code = doTransform
    
    if (code != 0) {
      error("Creating " + dst.getName() + " did not succeed: " + code)
    }
  }

  def inputToFile(is: java.io.InputStream, f: java.io.File) {
    val in = scala.io.Source.fromInputStream(is)
    val out = new java.io.PrintWriter(f)
    try { in.getLines().foreach(out.print(_)) }
    finally { out.close }
  }

  /**
   * Transforms the given DocBook XML file to another single file (using
   * the given stylesheet)
   * @param src the DocBook XML file
   * @param dst the target file
   * @param styleSheet an URI to a stylesheet or the name of a
   * local stylesheet file
   * @param cp the classpath required during transformation
   * @param log the logger
   */
  private def transformDocBook(src: File, dst: File, styleSheet: String,
  cp: Classpath, log: Logger) {
    val stream= getClass.getResourceAsStream("/foCustomization.xml")
    val customization  = File.createTempFile("sbt-docbook-plugin-","cust.xml" )
    inputToFile(stream,  customization)

    transform(src, dst, log) {
      //write output to temporary file. this avoids a bug in Saxon
      //that caused spaces in the output filename to be converted to
      //an escaped sequence (%20)
      val temp = File.createTempFile("sbt-docbook-plugin-", ".fo")
      val code = Fork.java(None, Seq[String](
      "-cp", cp.files.mkString(File.pathSeparator),
      "-Djavax.xml.parsers.DocumentBuilderFactory="+
        "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl",
      "-Djavax.xml.parsers.SAXParserFactory=" +
        "org.apache.xerces.jaxp.SAXParserFactoryImpl",
      "-Dorg.apache.xerces.xni.parser.XMLParserConfiguration=" +
        "org.apache.xerces.parsers.XIncludeParserConfiguration",
      "com.icl.saxon.StyleSheet",
      "-o", temp.toString,
        src.toString, styleSheet
      ), log)

      //copy temporary file to real output file
      temp #> dst !

      //delete temporary file (if this files it will be deleted on exit)
      if (!temp.delete()) {
        temp.deleteOnExit()
      }

      code
    }
    if (!customization.delete()) {
      customization.deleteOnExit()
    }
  }

  /**
   * Transforms the given DocBook XML file using the given stylesheet.
   * Writes output files to a given directory.
   * @param src the DocBook XML file
   * @param target the target directory
   * @param styleSheet an URI to a stylesheet or the name of a
   * local stylesheet file
   * @param cp the classpath required during transformation
   * @param log the logger
   */
  private def transformDocBookMultiple(src: File, target: File,
  styleSheet: String, cp: Classpath, log: Logger) {
    log.info("  " + src.getName() + " ...")
    val code = Fork.java(None, Seq[String](
      "-cp", cp.files.mkString(File.pathSeparator),
      "-Djavax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl",
      "-Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl",
      "-Dorg.apache.xerces.xni.parser.XMLParserConfiguration=org.apache.xerces.parsers.XIncludeParserConfiguration",
      "com.icl.saxon.StyleSheet",
      src.toString, styleSheet
    ), Some(target), log)
    
    if (code != 0) {
      error("Transformation did not succeed: " + code)
    }
  }
  
  /**
   * Transforms the given XSL-FOL file to a PDF file
   * @param src the XSL-FO file
   * @param dst the target PDF file
   */
  private def transformXslFo(src: File, dst: File) {
    val out = new BufferedOutputStream(new FileOutputStream(dst))
    try {
      val fop = fopFactory.newFop(MimeConstants.MIME_PDF, out)
      val tf = TransformerFactory.newInstance()
      val transformer = tf.newTransformer()
      val s = new StreamSource(src)
      val res = new SAXResult(fop.getDefaultHandler())
      transformer.transform(s, res)
    } finally {
      out.close()
    }
  }
  
  /**
   * <p>Creates a target file out of the given source file. Uses the source
   * file's name, changes it extension to <code>ext</code> and puts that file
   * into the given target directory.</p>
   * <p>Example: <code>makeTargetFile(file("src/foo.txt"), file("target"),
   * ".bar")</code> will return <code>file("target/foo.bar")</code></p>
   * @param src the source file
   * @param target the target directory
   * @param ext the new extension to use
   * @return the target file
   */
  private def makeTargetFile(src: File, target: File, ext: String): File = {
    val n = src.getName()
    val d = n.lastIndexOf('.')
    val o = (if (d == -1) n else n.substring(0, d)) + ext
    target / o
  }

  private def genericTask(targetName: String, ext: String)(mdbf: Seq[File],
      docBookSource: File, sources: Seq[File], t: File, styleSheet: String,
      cp: Classpath, s: TaskStreams): Seq[File] = {
    s.log.info("Transforming DocBook XML to " + targetName + ":")
    s.log.debug("Using stylesheet: " + styleSheet)
    val mdbfFiles = if (!mdbf.isEmpty) mdbf else getMainDocBookFiles(docBookSource, sources)
    mdbfFiles map { mf =>
      val tf = makeTargetFile(mf, t, ext)
      transformDocBook(mf, tf, styleSheet, cp, s.log)
      tf
    }
  }
  
  private def genericTaskMultiple(targetName: String)(mdbf: Seq[File],
      docBookSource: File, sources: Seq[File], t: File, styleSheet: String,
      cp: Classpath, s: TaskStreams) {
    t.mkdir()
    s.log.info("Transforming DocBook XML to " + targetName + ":")
    val mdbfFiles = if (!mdbf.isEmpty) mdbf else getMainDocBookFiles(docBookSource, sources)
    mdbfFiles foreach { mf =>
      transformDocBookMultiple(mf, t, styleSheet, cp, s.log)
    }

  }

  private def zipOutputDirectory(ext: String)(directoryToZip: File, t: File, s: TaskStreams) {
    def recursiveListFiles(f: File): Array[File] = {
      val these = f.listFiles
      these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
    }

    val files = recursiveListFiles(directoryToZip)

    val filesAndNames = files.zip(files map (f => f.getAbsolutePath drop t.getAbsolutePath.length ))
    val outputZip = t / (directoryToZip.getName + ext)
    s.log.info("Zipping to " + outputZip.getName)
    IO.zip(filesAndNames, outputZip)
  }

  private def makeGenericTask(targetName: String, ext: String,
      styleSheet: SettingKey[String]): Initialize[Task[Seq[File]]] =
    (mainDocBookFiles, docBookSourceDirectory, sourceDirectories in Compile,
      target, styleSheet, externalDependencyClasspath in Compile, streams) map
        genericTask(targetName, ext)
  
  private def makeGenericTaskMultiple(targetName: String,
      styleSheet: SettingKey[String]): Initialize[Task[Unit]] =
    (mainDocBookFiles, docBookSourceDirectory, sourceDirectories in Compile,
      docBookTargetDirectory, styleSheet, externalDependencyClasspath in Compile, streams) map
        genericTaskMultiple(targetName)

  private def makeZipOutputDirectory(ext: String = ".zip"): Initialize[Task[Unit]] =
  (docBookTargetDirectory, target, streams) map zipOutputDirectory(ext)

  override val settings = inConfig(DocBook)(Seq(
    //define default values
    mainDocBookFiles := Seq.empty,
    docBookSourceDirectory <<= sourceDirectory(_ / "main" / "docbook"),
    fopConfigFile := None,
    docBookTargetDirectory <<= target( _ / "doc_output"),
    
    //default values for the docbook stylesheets. Download the stylesheets
    //to your local hard drive and override these settings to speed up
    //transformation significantly
    docBookStyleSheetBase := "http://docbook.sourceforge.net/release/xsl/current/",
    docBookXslFoStyleSheet <<= docBookStyleSheetBase(_ + "fo/docbook.xsl"),
    docBookHtmlStyleSheet <<= docBookStyleSheetBase(_ + "html/docbook.xsl"),
    docBookHtmlChunkStyleSheet <<= docBookStyleSheetBase(_ + "html/chunk.xsl"),
    docBookHtmlOnechunkStyleSheet <<= docBookStyleSheetBase(_ + "html/onechunk.xsl"),
    docBookXHtmlStyleSheet <<= docBookStyleSheetBase(_ + "xhtml/docbook.xsl"),
    docBookXHtmlChunkStyleSheet <<= docBookStyleSheetBase(_ + "xhtml/chunk.xsl"),
    docBookXHtmlOnechunkStyleSheet <<= docBookStyleSheetBase(_ + "xhtml/onechunk.xsl"),
    docBookXHtml11StyleSheet <<= docBookStyleSheetBase(_ + "xhtml-1_1/docbook.xsl"),
    docBookXHtml11ChunkStyleSheet <<= docBookStyleSheetBase(_ + "xhtml-1_1/chunk.xsl"),
    docBookXHtml11OnechunkStyleSheet <<= docBookStyleSheetBase(_ + "xhtml-1_1/onechunk.xsl"),
    docBookEpubStyleSheet <<= docBookStyleSheetBase(_ + "epub/docbook.xsl"),
    docBookHtmlHelpStyleSheet <<= docBookStyleSheetBase(_ + "htmlhelp/htmlhelp.xsl"),
    docBookJavaHelpStyleSheet <<= docBookStyleSheetBase(_ + "javahelp/javahelp.xsl"),
    docBookEclipseHelpStyleSheet <<= docBookStyleSheetBase(_ + "eclipse/eclipse.xsl"),
    docBookManpageStyleSheet <<= docBookStyleSheetBase(_ + "manpages/docbook.xsl"),

    //define zip task
    zipTask <<= makeZipOutputDirectory(),


    //define tasks
    xslFoTask <<= makeGenericTask("XSL-FO", ".fo", docBookXslFoStyleSheet),
    htmlTask <<= makeGenericTask("HTML (single file)", ".html", docBookHtmlStyleSheet),
    htmlChunkTask <<= makeGenericTaskMultiple("HTML (chunked)", docBookHtmlChunkStyleSheet),
    htmlOnechunkTask <<= makeGenericTaskMultiple("HTML (chunked)", docBookHtmlOnechunkStyleSheet),
    xhtmlTask <<= makeGenericTask("XHTML (single file)", ".html", docBookXHtmlStyleSheet),
    xhtmlChunkTask <<= makeGenericTaskMultiple("XHTML (chunked)", docBookXHtmlChunkStyleSheet),
    xhtmlOnechunkTask <<= makeGenericTaskMultiple("XHTML (chunked)", docBookXHtmlOnechunkStyleSheet),
    xhtml11Task <<= makeGenericTask("XHTML 1.1 (single file)", ".html", docBookXHtml11StyleSheet),
    xhtml11ChunkTask <<= makeGenericTaskMultiple("XHTML 1.1 (chunked)", docBookXHtml11ChunkStyleSheet),
    xhtml11OnechunkTask <<= makeGenericTaskMultiple("XHTML 1.1 (chunked)", docBookXHtml11OnechunkStyleSheet),
    htmlHelpTask <<= makeGenericTaskMultiple("HTML Help", docBookHtmlHelpStyleSheet),
    javaHelpTask <<= makeGenericTaskMultiple("JavaHelp", docBookJavaHelpStyleSheet),
    eclipseHelpTask <<= makeGenericTaskMultiple("Eclipse Help", docBookEclipseHelpStyleSheet),
    manpageTask <<= makeGenericTaskMultiple("man page", docBookManpageStyleSheet),

    //define epub task
    epubGenerateTask <<= makeGenericTaskMultiple("EPUB", docBookEpubStyleSheet),

    epubTask <<= (epubGenerateTask, docBookTargetDirectory, target,streams) map {
      (e, d, t, s) =>
       zipOutputDirectory(".epub")(d, t, s)
    },

      //define pdf task
    pdfTask <<= (xslFoTask, fopConfigFile, baseDirectory, target, streams) map {
      (xslFoFiles, configFile, base, t, s) =>
      
      s.log.info("Transforming XSL-FO to PDF:")
      
      val baseUrlStr = base.toURI().toString()
      fopFactory.setBaseURL(baseUrlStr)
      configFile foreach fopFactory.setUserConfig
      
      val conversions = xslFoFiles map { mf => (mf, makeTargetFile(mf, t, ".pdf")) }
      conversions foreach { c => transformXslFo(c._1, c._2) }
      
      conversions map { _._2 }
    }
  )) ++
  Seq(
    //add required libraries to project's classpath
    libraryDependencies ++= Seq(
      "saxon" % "saxon" % "6.5.3",
      "xml-resolver" % "xml-resolver" % "1.2",
      "net.sf.docbook" % "docbook-xsl" % "1.76.1",
      "net.sf.docbook" % "docbook-xsl-saxon" % "1.0.0",
      "xerces" % "xercesImpl" % "2.10.0"
//      "net.sf.xslthl" % "xslthl" % "2.0.2"
    ),
    
    //add source directory for DocBook XML files
    unmanagedSourceDirectories in Compile <+=
      (docBookSourceDirectory in DocBook).identity,

    resourceDirectory in Compile <<= javaSource in Compile,
    
    xslFoTask <<= (xslFoTask in DocBook).identity,
    htmlTask <<= (htmlTask in DocBook).identity,
    htmlChunkTask <<= (htmlChunkTask in DocBook).identity,
    htmlOnechunkTask <<= (htmlOnechunkTask in DocBook).identity,
    xhtmlTask <<= (xhtmlTask in DocBook).identity,
    xhtmlChunkTask <<= (xhtmlChunkTask in DocBook).identity,
    xhtmlOnechunkTask <<= (xhtmlOnechunkTask in DocBook).identity,
    xhtml11Task <<= (xhtml11Task in DocBook).identity,
    xhtml11ChunkTask <<= (xhtml11ChunkTask in DocBook).identity,
    xhtml11OnechunkTask <<= (xhtml11OnechunkTask in DocBook).identity,
    epubTask <<= (epubTask in DocBook).identity,
    htmlHelpTask <<= (htmlHelpTask in DocBook).identity,
    javaHelpTask <<= (javaHelpTask in DocBook).identity,
    eclipseHelpTask <<= (eclipseHelpTask in DocBook).identity,
    manpageTask <<= (manpageTask in DocBook).identity,
    pdfTask <<= (pdfTask in DocBook).identity
  )
}
