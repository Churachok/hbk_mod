package dev.kirill.hbk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** CPU-only validation of the baked model: no Minecraft world or graphics window required. */
public final class SovietBusMeshChecks {
	private record Face(String part, int axis, int sign, double plane, double[] min, double[] max) {}

	private SovietBusMeshChecks() {
	}

	public static void main(String[] args) {
		verify();
	}

	public static void verify() {
		List<Face> faces = new ArrayList<>();
		for (var material : SovietBusModel.Material.values()) {
			var root = SovietBusModel.createLayer(material).bakeRoot();
			root.visit(new PoseStack(), (pose, path, index, cube) -> {
				for (var polygon : cube.polygons) {
					var normal = polygon.normal();
					int axis = Math.abs(normal.x()) > 0.5 ? 0 : Math.abs(normal.y()) > 0.5 ? 1 : 2;
					int sign = (axis == 0 ? normal.x() : axis == 1 ? normal.y() : normal.z()) > 0 ? 1 : -1;
					double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
					double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
					for (var vertex : polygon.vertices()) {
						var position = pose.pose().transformPosition(new Vector3f(vertex.worldX(), vertex.worldY(), vertex.worldZ()));
						double[] coordinates = {position.x, position.y, position.z};
						for (int dimension = 0; dimension < 3; dimension++) {
							min[dimension] = Math.min(min[dimension], coordinates[dimension]);
							max[dimension] = Math.max(max[dimension], coordinates[dimension]);
						}
						if (vertex.u() < 0 || vertex.u() > 1 || vertex.v() < 0 || vertex.v() > 1) {
							throw new AssertionError("Bus UV outside texture atlas: " + path);
						}
					}
					faces.add(new Face(material + path, axis, sign, min[axis], min, max));
				}
			});
		}
		for (int index = 0; index < faces.size(); index++) {
			Face first = faces.get(index);
			for (int otherIndex = index + 1; otherIndex < faces.size(); otherIndex++) {
				Face other = faces.get(otherIndex);
				if (first.axis != other.axis || first.sign != other.sign
						|| Math.abs(first.plane - other.plane) > 0.000001) continue;
				boolean overlap = true;
				for (int dimension = 0; dimension < 3; dimension++) {
					if (dimension != first.axis && Math.min(first.max[dimension], other.max[dimension])
							- Math.max(first.min[dimension], other.min[dimension]) < 0.000001) {
						overlap = false;
					}
				}
				if (overlap) {
					throw new AssertionError("Coplanar overlapping bus faces: " + first.part + " / " + other.part);
				}
			}
		}
		System.out.println("Bus mesh: " + faces.size() + " faces; no overlapping coplanar faces; all UVs inside atlas");
	}
}
