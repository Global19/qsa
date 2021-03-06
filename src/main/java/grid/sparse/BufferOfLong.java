package grid.sparse;

import java.util.ArrayList;
import java.util.List;

public class BufferOfLong {

	long[] values;
	int size;

	public BufferOfLong(int n) {
		values = new long[n];
	}

	public BufferOfLong() {
		this(1000000);
	}

	public void clear() {
		size = 0;
	}

	public void add(long t) {
		if (values.length <= size) {
			extend();
		}
		values[size++] = t;
	}

	private void extend() {
		long[] newValues = new long[size * 2];
		System.arraycopy(values, 0, newValues, 0, size);
		values = newValues;
	}

	public int size() {
		return size;
	}

	public long get(int i) {
		return values[i];
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public void addAll(Iterable<Long> it) {
		for (long t : it) {
			add(t);
		}
	}

	public void addAll(long[] ts) {
		for (long t : ts) {
			add(t);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++) {
			sb.append(values[i]);
			if (i != size - 1) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	public List<Long> toList() {
		List<Long> list = new ArrayList<>();
		for (int i = 0; i < size(); i++) {
			list.add(get(i));
		}
		return list;
	}

}
