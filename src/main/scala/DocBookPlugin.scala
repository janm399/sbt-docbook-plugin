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
import java.io.File

/**
 * Provides tasks to compile DocBook XML files to various output formats
 * @author Michel Kraemer
 */
object DocBookPlugin extends Plugin {
  //declare our own configuration scope
  val DocBook = config("DocBook") extend(Compile)
  
  //declare tasks
  val xslFoTask = TaskKey[Seq[File]]("xsl-fo",
      "Transforms DocBook files to XSL-FO files")
  val pdfTask = TaskKey[Seq[File]]("pdf",
      "Transforms DocBook files to PDF files")
  
  //declare settings
  val mainDocBookFiles = SettingKey[Seq[File]]("main-docbook-files")
  val docBookSourceDirectory = SettingKey[File]("docbook-source-directory")
  val docBookStyleSheet = SettingKey[URI]("docbook-stylesheet")
  
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
  
  /**
   * Transforms the given DocBook XML file to a XSL-FO file
   * @param src the DocBook XML file
   * @param dst the target XSL-FO file
   * @param styleSheet an URI to a stylesheet to use during transformation
   * @param cp the classpath required during transformation
   * @param log the logger
   */
  private def transformDocBook(src: File, dst: File, styleSheet: URI,
      cp: Classpath, log: Logger) {
    transform(src, dst, log) {
      Fork.java(None, Seq[String](
        "-cp", cp.files.mkString(File.pathSeparator),
        "com.icl.saxon.StyleSheet",
        "-x", "org.apache.xml.resolver.tools.ResolvingXMLReader",
        "-y", "org.apache.xml.resolver.tools.ResolvingXMLReader",
        "-r", "org.apache.xml.resolver.tools.CatalogResolver",
        "-o", dst.toString,
        src.toString,
        styleSheet.toString
      ), log)
    }
  }
  
  /**
   * Transforms the given XSL-FOL file to a PDF file
   * @param src the XSL-FO file
   * @param dst the target PDF file
   * @param cp the classpath required during transformation
   * @param log the logger
   */
  private def transformXslFo(src: File, dst: File, cp: Classpath, log: Logger) {
    transform(src, dst, log) {
      Fork.java(None, Seq[String](
        "-cp", cp.files.mkString(File.pathSeparator),
        "org.apache.fop.cli.Main",
        "-fo", src.toString,
        "-pdf", dst.toString
      ), log)
    }
  }
  
  /**
   * <p>Creates a target file out of the given source file. Uses the source file's
   * name, changes it extension to <code>ext</code> and puts that file into
   * the given target directory.</p>
   * <p>Example: <code>makeTargetFile(file("src/foo.txt"), file("target"), ".bar")</code>
   * will return <code>file("target/foo.bar")</code></p>
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
  
  override val settings = inConfig(DocBook)(Seq(
    //define default values
    mainDocBookFiles := Seq.empty,
    docBookSourceDirectory <<= sourceDirectory(_ / "main" / "docbook"),
    
    //define xsl-fo task
    xslFoTask <<= (mainDocBookFiles, docBookSourceDirectory,
        sourceDirectories in Compile, target, docBookStyleSheet,
        externalDependencyClasspath in Compile, streams) map {
      (mdbf, docBookSource, sources, t, styleSheet, cp, s) =>
      
      s.log.info("Transforming DocBook XML to XSL-FO:")
      val mdbfFiles = if (!mdbf.isEmpty) mdbf else getMainDocBookFiles(docBookSource, sources)
      val conversions = mdbfFiles map { mf => (mf, makeTargetFile(mf, t, ".fo")) }
      conversions foreach { c => transformDocBook(c._1, c._2, styleSheet, cp, s.log) }
      
      conversions map { _._2 }
    },
    
    //define pdf task
    pdfTask <<= (xslFoTask, target, externalDependencyClasspath in Compile,
        streams) map {
      (xslFoFiles, t, cp, s) =>
      
      s.log.info("Transforming XSL-FO to PDF:")
      val conversions = xslFoFiles map { mf => (mf, makeTargetFile(mf, t, ".pdf")) }
      conversions foreach { c => transformXslFo(c._1, c._2, cp, s.log) }
      
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
      "org.apache.xmlgraphics" % "fop" % "1.0"
    ),
    
    //add source directory for DocBook XML files
    unmanagedSourceDirectories in Compile <+=
      (docBookSourceDirectory in DocBook).identity,
    
    //watch .xml files
    sourceFilter <<= sourceFilter(_ || "*.xml"),
    
    xslFoTask <<= (xslFoTask in DocBook).identity,
    pdfTask <<= (pdfTask in DocBook).identity
  )
}
