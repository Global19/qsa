package fragments;

/**
 * Aligned word pair.
 */
public class AwpNode {

	private Word x, y; // from first and second protein
	private int clusterId;
	public int id;
	private double rmsd = Double.MAX_VALUE;
	private int connectivity;
	private Component component;

	
	public AwpNode(Word x, Word y) {
		this.x = x;
		this.y = y;
	}

	public boolean before(AwpNode other) {
		if (x.getId() < other.x.getId()) {
			return true;
		} else if (x.getId() > other.x.getId()) {
			return false;
		} else { // x ~ other.x
			if (y.getId() < other.y.getId()) {
				return true;
			} else if (y.getId() > other.y.getId()) {
				return false;
			} else {
				throw new RuntimeException();
			}
		}
	}

	public void setComponent(Component c) {
		this.component = c;
	}

	public Component getComponent() {
		return component;
	}

	public void connect() {
		connectivity++;
	}

	public int getConnectivity() {
		return connectivity;
	}

	public void updateRmsd(double r) {
		if (r < rmsd) {
			rmsd = r;
		}
	}

	public int getClusterId() {
		return clusterId;
	}

	public Word[] getWords() {
		Word[] words = {x, y};
		return words;
	}

	public void setClusterId(int id) {
		this.clusterId = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((x == null) ? 0 : x.hashCode());
		result = prime * result + ((y == null) ? 0 : y.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return x.getId() + "_" + y.getId();
	}

	@Override
	public boolean equals(Object o) {
		AwpNode other = (AwpNode) o;
		return x.equals(other.x) && y.equals(other.y);
	}

}
