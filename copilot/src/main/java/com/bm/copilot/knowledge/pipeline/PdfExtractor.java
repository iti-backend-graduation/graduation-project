package com.bm.copilot.knowledge.pipeline;

import com.bm.copilot.knowledge.model.ExtractedSection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfExtractor implements DocumentExtractor {
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    @Override
    public List<ExtractedSection> extract(byte[] content) throws IOException {
        List<ExtractedSection> sections = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                if (!text.isBlank()) {
                    sections.add(new ExtractedSection(text, page, null));
                }
            }
        }
        return sections;
    }
}
