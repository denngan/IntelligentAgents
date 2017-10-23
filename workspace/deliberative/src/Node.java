public class Node<T> {
	public Node(T nodeData, double heuristicValue) {
		this.nodeData = nodeData;
		this.h = heuristicValue;
		this.g = -1;
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
		return h;
	}

}
