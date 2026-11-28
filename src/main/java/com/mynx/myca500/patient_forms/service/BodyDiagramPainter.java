package com.mynx.myca500.patient_forms.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.List;



final class BodyDiagramPainter {

    static final int MAX_SIDE = 1600;
    private static final Color MARKER = new Color(0xDC, 0x26, 0x26);

    record Marker(int number, double x, double y) {
    }

    private BodyDiagramPainter() {
    }

    static boolean onImage(Double x, Double y) {
        return x != null && y != null && x >= 0 && x <= 1 && y >= 0 && y <= 1;
    }

    static BufferedImage paint(BufferedImage source, List<Marker> markers) {
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(source.getWidth(), source.getHeight()));
        int w = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(source.getHeight() * scale));

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.drawImage(source, 0, 0, w, h, null);

            int r = Math.max(7, (int) Math.round(Math.min(w, h) * 0.025));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, (int) Math.round(r * 1.15))));
            FontMetrics fm = g.getFontMetrics();
            g.setStroke(new BasicStroke(Math.max(1.5f, r / 5f)));

            for (Marker m : markers) {
                double cx = m.x() * w;
                double cy = m.y() * h;
                Ellipse2D dot = new Ellipse2D.Double(cx - r, cy - r, 2.0 * r, 2.0 * r);
                g.setColor(MARKER);
                g.fill(dot);
                g.setColor(Color.WHITE);
                g.draw(dot);
                String label = Integer.toString(m.number());
                g.drawString(label, (float) (cx - fm.stringWidth(label) / 2.0),
                        (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
            }
        } finally {
            g.dispose();
        }
        return out;
    }
}
