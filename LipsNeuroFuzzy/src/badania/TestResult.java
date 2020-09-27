package badania;

public class TestResult implements Comparable {
	private int success;
	private int fail;
	private long time;
	
	private long iter;
	private double error;
	
	public TestResult(int success, int fail, long time) {
		super();
		this.success = success;
		this.fail = fail;
		this.time = time;
	}

	public TestResult(int success, int fail, long iter, double error, long time) {
		super();
		this.success = success;
		this.fail = fail;
		this.time = time;
		this.iter = iter;
		this.error = error;
	}

	public int getSuccess() {
		return success;
	}

	public int getFail() {
		return fail;
	}

	public long getTime() {
		return time;
	}

	public long getIter() {
		return iter;
	}

	public double getError() {
		return error;
	}

	@Override
	public int compareTo(Object o) {
		
		TestResult second = (TestResult) o;
		
		if (this.success > second.success) {
			return 1;
		} else if (this.success < second.success) {
			return -1;
		} else return 0;
		
	}

}
