public class Node<T> {
	public Node(T nodeData) {
		this.nodeData = nodeData;
	}

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
		if (h == 0) {
			// compute heuristic
			h = 1;
		}
		return h;
	}

}
