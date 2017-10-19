public class Node<T> {
	private final T nodeData;

	private double g;
	private double h;

	public T getNodeData() {
		return nodeData;
	}

	public double getF() {
		return g+h;
	}

	public double getG() {
		return g;
	}

	public void setG(double g) {
		this.g = g;
	}

	public double getH() {
		return h;
	}

	public void setH(double h) {
		this.h = h;
	}
}
