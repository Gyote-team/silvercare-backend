package com.gyote.silvercare.ai_result.command.application;
import org.apache.pdfbox.pdmodel.*; import org.apache.pdfbox.pdmodel.font.*; import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName; import org.apache.pdfbox.pdmodel.common.PDRectangle; import java.io.*;
/** PDF 구조는 서버가 만들며, 모델 HTML을 실행하거나 포함하지 않는다. */
final class SimplePdfRenderer { private SimplePdfRenderer(){}
 static byte[] render(String title,String content) throws IOException { try(var pdf=new PDDocument(); var out=new ByteArrayOutputStream()) { var page=new PDPage(PDRectangle.A4); pdf.addPage(page); try(var cs=new PDPageContentStream(pdf,page)){ cs.beginText(); cs.setFont(new PDType1Font(FontName.HELVETICA), 12); cs.newLineAtOffset(48,790); for(String line:((title==null?"AI 상세 설명":title)+"\n\n"+(content==null?"":content)).replaceAll("[#*_]", "").split("\\R")){ cs.showText(line.replaceAll("[^\\x20-\\x7E]", "?")); cs.newLineAtOffset(0,-17); } cs.endText(); } pdf.save(out); return out.toByteArray(); } }
}
