package net.minervamc.minerva.utils;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class ParticleUtils {
    public static List<Vector> getStarPoints(int vertices, double starSize, double starHeaviness, int interpolationCount) {
        Vector[] polygonVertices = new Vector[2 * vertices];

        for (int i = 0; i < vertices; i++) {
            double angle = 2 * i * Math.PI / vertices;
            polygonVertices[2 * i] = new Vector(FastUtils.sin(angle), 0, FastUtils.cos(angle)).multiply(starSize);
            polygonVertices[2 * i + 1] = new Vector(FastUtils.sin(angle + Math.PI / vertices), 0, FastUtils.cos(angle + Math.PI / vertices)).multiply(starHeaviness * starSize);
        }

        List<Vector> starFull = new ArrayList<>();
        float step = 1.0f / (float) (interpolationCount + 1);
        for (int starPointIterator = 0; starPointIterator < polygonVertices.length; starPointIterator++) {
            starFull.add(polygonVertices[starPointIterator]);
            for (int i = 1; i <= interpolationCount; i++) {
                float t = step * i;
                starFull.add(
                        polygonVertices[starPointIterator == polygonVertices.length - 1 ? 0 : starPointIterator + 1].clone()
                                .subtract(polygonVertices[starPointIterator].clone())
                                .multiply(t)
                                .add(polygonVertices[starPointIterator].clone())
                );
            }
        }
        return starFull;
    }

    public static List<Vector> getLinePoints(Vector direction, double distance, double step) {
        List<Vector> points = new ArrayList<>();

        for (double i = 0; i < distance; i += step) {
            Vector point = direction.clone().multiply(i);
            points.add(point);
        }

        return points;
    }

    public static List<Vector> getCirclePoints(double radius) {
        List<Vector> points = new ArrayList<>();
        for (int d = 0; d < 20; d += 1) {
            double angle = (2*Math.PI*d)/20;
            // Cosine for X
            Vector vector = new Vector(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            points.add(vector);
        }

        return points;
    }
    public static List<Vector> getVerticalCirclePoints(double radius, float pitch, float yaw, int particles) {
        List<Vector> points = new ArrayList<>();
        for (int i = 0; i < particles; i++) {
            double theta = i * 2 * Math.PI / particles;
            double x = radius * Math.sin(theta);
            double y = radius * Math.cos(theta);

            Vector point = new Vector(x, y, 0);
            point = rotateXAxis(point, pitch);
            point = rotateYAxis(point, yaw);

            points.add(point);
        }
        return points;
    }

        public static List<Vector> getSpiralPoints(double radius, double radiusIncrease, double maxY) {
            List<Vector> points = new ArrayList<>();
            for (int d = 0; d <= 60; d += 1) {
                radius += radiusIncrease;
                double y = 0;
                if (maxY != 0) y = d/(60/maxY);
                Vector vector = new Vector(Math.cos(d) * radius, d/(60/maxY), Math.sin(d) * radius);

                points.add(vector);
            }

            return points;
        }

    public static List<Vector> getFilledCirclePoints(double radius, double pointCount) {
        List<Vector> points = new ArrayList<>();
        for (int i = 0; i < pointCount; i++) {
            double a = Math.random();
            double b = Math.random();
            double x;
            double z;
            if (b != 0) {
                x = b * radius * FastUtils.cos(2 * Math.PI * a / b);
                z = b * radius * FastUtils.sin(2 * Math.PI * a / b);
            } else {
                x = 0;
                z = 0;
            }
            points.add(new Vector(x, 0, z));
        }

        return points;
    }

    public static List<Vector> getFilledRectanglePoints(double width, double height, Vector direction) {
        List<Vector> points = new ArrayList<>();

        for (double i = 0; i < width/2; i += 0.2) {
            for (double j = 0; j < height/2; j += 0.2) {
                points.add(direction.clone().add(new Vector(i, 0, j)));
                points.add(direction.clone().add(new Vector(i, 0, -j)));
                points.add(direction.clone().add(new Vector(-i, 0, j)));
                points.add(direction.clone().add(new Vector(i, 0, -j)));
            }
        }

        return points;
    }

    public static List<Vector> getCylinderPoints(double radius, double height) {
        List<Vector> originalCirclePoints = getCirclePoints(radius);
        List<Vector> points = new ArrayList<>(originalCirclePoints);
        for (int i = 0; i <= height/2; i++) {
            for (Vector point : originalCirclePoints) {
                points.add(point.clone().add(new Vector(0, i, 0)));
                points.add(point.clone().add(new Vector(0, -i, 0)));
            }
        }
        return points;
    }

    public static List<Vector> getSpherePoints(double radius, int particles) {
        List<Vector> points = new ArrayList<>();

        double increment = Math.PI / particles;
        for (double theta = 0; theta <= Math.PI; theta += increment) {
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);
            for (double phi = 0; phi <= 2 * Math.PI; phi += increment) {
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);
                Vector vector = new Vector(radius * sinTheta * cosPhi, radius * sinTheta * sinPhi, radius * cosTheta);

                points.add(vector);
            }
        }

        return points;
    }

    public static List<Vector> getQuadraticBezierPoints(Vector A, Vector B, Vector C, double particles) {
        List<Vector> points = new ArrayList<>();
        for (double i = 0; i <= particles; i += 1) {
            double t = i/particles;
            double u = 1 - t;
            Vector bezierPoint = A.multiply(u * u).add(B.clone().multiply(2 * u * t)).add(C.clone().multiply(t * t));
            points.add(bezierPoint.clone());
        }
        return points;
    }

    public static Vector rotateXAxis(Vector position, double degrees) {
        // Angle is negative, since:
        // This is 1:1 with Minecraft; -90 is up, 90 is down, 0 is straight ahead.
        double x = position.getX();
        double y = position.getY();
        double z = position.getZ();
        double cos = FastUtils.cosDeg(degrees);
        double sin = FastUtils.sinDeg(degrees);
        return new Vector(x, y * cos - z * sin, z * cos + y * sin);
    }

    public static Vector rotateYAxis(Vector position, double degrees) {
        double x = position.getX();
        double y = position.getY();
        double z = position.getZ();
        double cos = FastUtils.cosDeg(degrees);
        double sin = FastUtils.sinDeg(degrees);
        return new Vector(x * cos - z * sin, y, z * cos + x * sin);
    }

    public static Vector rotateZAxis(Vector position, double degrees) {
        double x = position.getX();
        double y = position.getY();
        double z = position.getZ();
        double cos = FastUtils.cosDeg(degrees);
        double sin = FastUtils.sinDeg(degrees);
        return new Vector(x * cos + y * sin, y * cos - x * sin, z);
    }

    public static Vector getDirection(Location from, Location to) {
        return to.toVector().subtract(from.toVector()).normalize();
    }
}
