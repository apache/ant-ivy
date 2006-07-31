package fr.jayasoft.ivy.filter;

public class OrFilter implements Filter {
	private Filter _op1;
	private Filter _op2;
	
	public OrFilter(Filter op1, Filter op2) {
		_op1 = op1;
		_op2 = op2;
	}
	public Filter getOp1() {
		return _op1;
	}
	public Filter getOp2() {
		return _op2;
	}
	public boolean accept(Object o) {
		return _op1.accept(o) || _op2.accept(o);
	}
}
