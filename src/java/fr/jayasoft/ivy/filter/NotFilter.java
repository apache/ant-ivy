package fr.jayasoft.ivy.filter;

public class NotFilter implements Filter {
	private Filter _op;
	
	public NotFilter(Filter op) {
		_op = op;
	}
	public Filter getOp() {
		return _op;
	}
	public boolean accept(Object o) {
		return !_op.accept(o);
	}
}
