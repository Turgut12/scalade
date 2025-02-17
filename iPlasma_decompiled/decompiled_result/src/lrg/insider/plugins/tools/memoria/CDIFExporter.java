/*    */ package classes.lrg.insider.plugins.tools.memoria;
/*    */ 
/*    */ import java.io.File;
/*    */ import java.io.FileNotFoundException;
/*    */ import java.io.FileOutputStream;
/*    */ import java.io.PrintStream;
/*    */ import java.util.ArrayList;
/*    */ import lrg.common.abstractions.entities.AbstractEntityInterface;
/*    */ import lrg.common.abstractions.plugins.tools.AbstractEntityTool;
/*    */ import lrg.memoria.core.System;
/*    */ import lrg.memoria.exporter.cdif.MooseCDIFExporter;
/*    */ 
/*    */ public class CDIFExporter
/*    */   extends AbstractEntityTool
/*    */ {
/* 16 */   public CDIFExporter() { super("CDIF Exporter", "Exports the model into CDIF", "system"); }
/*    */ 
/*    */   
/*    */   public void run(AbstractEntityInterface abstractEntityInterface, Object o) {
/* 20 */     if (!(abstractEntityInterface instanceof System))
/*    */       return; 
/* 22 */     System aSystem = (System)abstractEntityInterface;
/*    */     
/* 24 */     ArrayList<String> params = (ArrayList)o;
/* 25 */     File outputFile = new File((String)params.get(0));
/*    */     try {
/* 27 */       (new MooseCDIFExporter(aSystem)).exportToStream(new PrintStream(new FileOutputStream(outputFile)));
/* 28 */     } catch (FileNotFoundException e) {
/* 29 */       throw new RuntimeException(e.getMessage());
/*    */     } 
/*    */   }
/*    */ 
/*    */   
/* 34 */   public String getToolName() { return "CDIF Exporter"; }
/*    */ 
/*    */   
/*    */   public ArrayList<String> getParameterList() {
/* 38 */     ArrayList<String> parList = new ArrayList<String>();
/* 39 */     parList.add("Output File ");
/* 40 */     return parList;
/*    */   }
/*    */   
/*    */   public ArrayList<String> getParameterExplanations() {
/* 44 */     ArrayList<String> parList = new ArrayList<String>();
/* 45 */     parList.add("The CDIF file name where the model will be exported");
/* 46 */     return parList;
/*    */   }
/*    */ }


/* Location:              C:\Users\emill\Dropbox\slimmerWorden\2018-2019-Semester2\THESIS\iPlasma6\tools\iPlasma\classes.zip!\classes\lrg\insider\plugins\tools\memoria\CDIFExporter.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.0.7
 */