package com.vynk.cv.services;

import com.vynk.identity.entities.Profile;
import com.vynk.profiles.PublicProfileResponse;
import com.vynk.profiles.services.ProfileService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** Builds an on-demand, public-data-only, searchable CV PDF. */
@Service
public class CvPdfService {
    private static final float MARGIN =  fifty();
    private static final float FONT_SIZE = 10;
    private static final float LEADING = 15;

    private final ProfileService profiles;

    public CvPdfService(ProfileService profiles) {
        this.profiles = profiles;
    }

    public byte[] render(String username) {
        PublicProfileResponse profile = profiles.publicProfile(username);
        try (PDDocument document = new PDDocument();
             InputStream fontStream = new ClassPathResource("fonts/AdwaitaSans-Regular.ttf").getInputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType0Font font = PDType0Font.load(document, fontStream);
            Writer writer = new Writer(document, font);
            writer.heading(profile.firstName() + " " + profile.lastName(), 20);
            writer.line(profile.headline(), 12);
            writer.line(profile.location(), FONT_SIZE);
            writer.line("Profile: " + profile.username(), FONT_SIZE);
            writer.section("Summary");
            writer.paragraph(profile.summary());
            if (!profile.experience().isEmpty()) {
                writer.section("Experience");
                for (var item : profile.experience()) {
                    writer.entry(item.title() + " — " + item.company(), item.location());
                    writer.line(date(item.startDate()) + " – " + (item.current() ? "Present" : date(item.endDate())), FONT_SIZE);
                    writer.paragraph(item.description());
                }
            }
            if (!profile.education().isEmpty()) {
                writer.section("Education");
                for (var item : profile.education()) {
                    writer.entry(item.school(), join(item.degree(), item.fieldOfStudy()));
                    writer.line(date(item.startDate()) + " – " + date(item.endDate()), FONT_SIZE);
                    writer.paragraph(item.description());
                }
            }
            if (!profile.certifications().isEmpty()) {
                writer.section("Certifications");
                for (var item : profile.certifications()) writer.line(join(item.name(), item.issuer(), date(item.issueDate())), FONT_SIZE);
            }
            if (!profile.projects().isEmpty()) {
                writer.section("Projects");
                for (var item : profile.projects()) {
                    writer.entry(item.name(), join(date(item.startDate()), date(item.endDate())));
                    writer.paragraph(item.description());
                    writer.link(item.url());
                }
            }
            if (!profile.skills().isEmpty()) {
                writer.section("Skills");
                writer.paragraph(String.join(", ", profile.skills().stream().map(PublicProfileResponse.SkillDto::name).toList()));
            }
            if (!profile.languages().isEmpty()) {
                writer.section("Languages");
                for (var item : profile.languages()) writer.line(join(item.name(), item.proficiency()), FONT_SIZE);
            }
            if (!profile.links().isEmpty()) {
                writer.section("Links");
                for (var item : profile.links()) writer.link(join(item.label(), item.url()));
            }
            writer.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render profile CV.", exception);
        }
    }

    private static String date(java.time.LocalDate date) { return date == null ? "" : date.toString(); }
    private static String join(String... values) {
        List<String> present = new ArrayList<>();
        for (String value : values) if (value != null && !value.isBlank()) present.add(value.trim());
        return String.join(" · ", present);
    }
    private static float fifty() { return 50f; }

    private static final class Writer {
        private final PDDocument document;
        private final PDType0Font font;
        private PDPageContentStream stream;
        private float y;

        private Writer(PDDocument document, PDType0Font font) throws IOException {
            this.document = document;
            this.font = font;
            page();
        }
        private void page() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }
        private void ensure(float height) throws IOException { if (y - height < MARGIN) page(); }
        private void heading(String text, float size) throws IOException { text(text, size, true); }
        private void section(String text) throws IOException { ensure(30); y -= 10; text(text, 13, true); }
        private void entry(String title, String subtitle) throws IOException {
            text(title, 11, true);
            if (subtitle != null && !subtitle.isBlank()) text(subtitle, FONT_SIZE, false);
        }
        private void paragraph(String text) throws IOException {
            if (text == null || text.isBlank()) return;
            float maxWidth = PDRectangle.LETTER.getWidth() - MARGIN * 2;
            StringBuilder line = new StringBuilder();
            for (String word : text.trim().split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (font.getStringWidth(candidate) / 1000 * FONT_SIZE > maxWidth && !line.isEmpty()) {
                    text(line.toString(), FONT_SIZE, false);
                    line.setLength(0);
                }
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
            if (!line.isEmpty()) text(line.toString(), FONT_SIZE, false);
        }
        private void link(String value) throws IOException { if (value != null && !value.isBlank()) paragraph(value); }
        private void line(String value, float size) throws IOException { if (value != null && !value.isBlank()) text(value, size, false); }
        private void text(String value, float size, boolean bold) throws IOException {
            if (value == null || value.isBlank()) return;
            ensure(size + 4);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(value);
            stream.endText();
            y -= size + (bold ? 7 : 4);
        }
        private void close() throws IOException { if (stream != null) { stream.close(); stream = null; } }
    }
}
