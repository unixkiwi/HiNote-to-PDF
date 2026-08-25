package de.unixkiwi.hinoteconverter;

import de.unixkiwi.hinoteconverter.models.Page;
import de.unixkiwi.hinoteconverter.models.Point;
import de.unixkiwi.hinoteconverter.models.Stroke;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Renderer {
    private Page page;
    private float width;
    private float height;


    public Renderer(Page page) {
        this.page = page;
        this.width = page.getWidth();
        this.height = page.getHeight();
    }

    private Point getPointOnLine(Point p0, Point p1, Float t) {
        Float dx = p1.x() - p0.x();
        Float dy = p1.y() - p0.y();
        return new Point(p0.x() + dx * t, p0.y() + dy * t);
    }

    private List<Point> chaikinSmooth(List<Point> points, int iterations) {
        if (points.size() < 6) {
            return points;
        }

        List<Point> smoothedPoints = new ArrayList<>();
        smoothedPoints.add(points.getFirst());

        for (int i = 0; i < points.size() - 1; i++) {
            Point p0 = points.get(i);
            Point p1 = points.get(i + 1);

            Point sp0 = getPointOnLine(p0, p1, 0.25F);
            Point sp1 = getPointOnLine(p0, p1, 0.75F);

            smoothedPoints.add(sp0);
            smoothedPoints.add(sp1);
        }

        smoothedPoints.add(points.getLast());

        if (iterations > 1) {
            return chaikinSmooth(smoothedPoints, iterations - 1);
        } else {
            return smoothedPoints;
        }
    }

    public GraphicAsset buildGraphic() {
        return (g2d) -> {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, (int) width, (int) height);

            // Turn on beautiful rendering hints
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            for (Stroke stroke : page.getStrokes()) {
                List<Point> smoothedPoints = chaikinSmooth(stroke.points(), 2);
                if (smoothedPoints.isEmpty()) continue;

                g2d.setStroke(new BasicStroke(stroke.baseWidth() * 0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.setColor(stroke.color());

                java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();

                // Move to the exact floating-point starting location
                Point first = smoothedPoints.get(0);
                path.moveTo(first.x(), first.y());

                // Connect lines using true sub-pixel float positions
                for (int i = 1; i < smoothedPoints.size(); i++) {
                    Point p = smoothedPoints.get(i);
                    path.lineTo(p.x(), p.y());
                }

                // Draw the complete smooth stroke shape
                g2d.draw(path);
            }
        };
    }
}
