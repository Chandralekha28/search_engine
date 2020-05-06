package com.util;

import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class PDFCreator {

	public void createPDF(ArrayList<Integer> limits,ArrayList<Float> medians,ArrayList<Double> averages, ArrayList<Float> q1s,ArrayList<Float> q3s) {
		Document doc = new Document();
		PdfWriter docWriter = null;

			DecimalFormat df = new DecimalFormat("0.00");
			try {
				Font bfBold12 = new Font(FontFamily.TIMES_ROMAN, 12, Font.BOLD, new BaseColor(0, 0, 0)); 
				Font bf12 = new Font(FontFamily.TIMES_ROMAN, 12); 
				
				String path = "Report.pdf";
				docWriter = PdfWriter.getInstance(doc , new FileOutputStream(path));
				doc.addAuthor("Pooja");
				doc.addCreationDate();
				doc.addProducer();
				doc.addCreator("Pooja");
				doc.addTitle("Report of Min hashing");
				doc.setPageSize(PageSize.LETTER);
				doc.open();
				Paragraph paragraph = new Paragraph("The document gives average, median, first and third quartile");
				
				float[] columnWidths = {1.5f, 2f, 2f, 2f,2f};
				PdfPTable table = new PdfPTable(columnWidths);
				table.setWidthPercentage(90f);
				
				insertCell(table, "n", Element.ALIGN_LEFT, 1, bfBold12);
				insertCell(table, "Median", Element.ALIGN_LEFT, 1, bfBold12);
				insertCell(table, "Average", Element.ALIGN_LEFT, 1, bfBold12);
				insertCell(table, "First quartile", Element.ALIGN_LEFT, 1, bfBold12);
				insertCell(table, "Third quartile", Element.ALIGN_LEFT, 1, bfBold12);
				table.setHeaderRows(1);
				double orderTotal, total = 0;
				
				//just some random data to fill 
				for(int i=0;i<medians.size();i++){
					insertCell(table, limits.get(i)+"", Element.ALIGN_LEFT, 1, bf12);
					insertCell(table, medians.get(i)+"", Element.ALIGN_LEFT, 1, bf12);
					insertCell(table, averages.get(i)+"", Element.ALIGN_LEFT, 1, bf12);
					insertCell(table, q1s.get(i)+"", Element.ALIGN_LEFT, 1, bf12);
					insertCell(table, q3s.get(i)+"", Element.ALIGN_LEFT, 1, bf12);
				}
				paragraph.add(table);
				doc.add(paragraph);
				
			}
			catch (DocumentException dex){
				dex.printStackTrace();
			}
			catch (Exception ex){
				ex.printStackTrace();
			}
			finally
			{
				if (doc != null){
					doc.close();
				}
				if (docWriter != null){
					docWriter.close();
				}
			}
	}

	public void insertCell(PdfPTable table, String text, int align, int colspan, Font font){
		PdfPCell cell = new PdfPCell(new Phrase(text.trim(), font));
		cell.setHorizontalAlignment(align);
		cell.setColspan(colspan);
		if(text.trim().equalsIgnoreCase("")){
			cell.setMinimumHeight(10f);
		}
		table.addCell(cell);
	}

}
