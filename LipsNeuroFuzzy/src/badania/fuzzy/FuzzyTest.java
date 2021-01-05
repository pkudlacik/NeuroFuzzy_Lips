package badania.fuzzy;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import badania.TestResult;

public class FuzzyTest {

	private LinkedList<TestResult> results;

	private int success = 0;
	private int fail = 0;
	private double avgSuccess = 0.0;
	private double avgFail = 0.0;

	private int reported = 0;
	private long time = 0;
	private double avgTime = 0.0;

	private double procSucc = 0.0;
	private double procFail = 0.0;

	private boolean simpleResults = false;
	private boolean allResults = true;

	private double bestSuccess = 0.0;
	private double bestRange = 2.0;
	private String bestParams;

	private int threads = 8;
	
	public FuzzyTest() {
		results = new LinkedList<TestResult>();
	}

	public void setSimpleResults(boolean simpleResults) {
		this.simpleResults = simpleResults;
	}

	public void setShowOnlyBestResults(boolean showBestResults) {
		this.allResults = !showBestResults;
	}

	public void setBestLevel(double bestPercentage) {
		this.bestSuccess = bestPercentage;
	}

	public void setBestRange(double bestRange) {
		this.bestRange = bestRange;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	private void clearStats() {
		results.clear();
		success = 0;
		fail = 0;
		time = 0;
		reported = 0;

		avgTime = 0;
		avgFail = 0;
		avgSuccess = 0;

		procSucc = 0.0;
		procFail = 0.0;
	}

	synchronized public void addResult(TestResult result) {
		results.add(result);
		reported++;
	}

	private void summarizeTest() {

		if (reported > 500000) { // delete outliers if there is enough runs
			// delete 5% lowest and highest
			int to_delete = results.size();
			to_delete /= 20;
			if (to_delete == 0) {
				to_delete = 1;
			}

			// Sort results
			results.sort(null);

			// delete top 5% and bottom 5%
			for (int i = 0; i < to_delete; i++) {
				results.removeFirst();
				results.removeLast();
				reported -= 2;
			}
		}

		for (TestResult result : results) {
			this.success += result.getSuccess();
			this.fail += result.getFail();
			this.time += result.getTime();
		}

		avgTime = (time * 1.0) / reported;
		avgFail = fail / reported;
		avgSuccess = success / reported;

		procFail = fail * 100.0 / (fail + success);
		procSucc = success * 100.0 / (fail + success);
	}

	public void doTest(double a_start, double a_stop, double a_step, double b_start, double b_stop, double b_step,
			int size_start, int size_stop, int segments, int tests) {

		double a = 5;
		double b = 2;
		int size = 5;

		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("#.###", otherSymbols);

		System.out.println("Starting ...");

		int counter = 0;

		// for all specified sizes
		for (size = size_start; size <= size_stop; size++) {
			// for all specified As
			for (a = a_start; a <= a_stop; a += a_step) {
				// for all specified Bs
				for (b = b_start; b <= b_stop; b += b_step) {

					clearStats();

					int runs = tests;

					if (!simpleResults) {
						System.out.println("*** Processing ");
					}
					// run defined threads (tests)
					for (int i = 0; i < runs; i++) {
						FuzzyTestWorker test = new FuzzyTestWorker(a, b, size, segments, false, this);
						// FuzzyTestGrouppedWorker test = new FuzzyTestGrouppedWorker(a, b, size, false,
						// this);
						// FuzzyTestClusteredWorker test = new FuzzyTestClusteredWorker(a, b, size,
						// false, this);
						test.start();
						if ((i+1) % threads == 0) { //wait every defined number of threads until they are done to continue creating new ones
							while (reported < i + 1) {
								try {
									if (!simpleResults) {
										System.out.print(".");
										counter++;
										if (counter > 80) {
											System.out.println();
											counter = 0;
										}
									}
									TimeUnit.MILLISECONDS.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
					}

					// wait for the rest to finish
					while (reported < runs) {
						try {
							if (!simpleResults) {
								System.out.print(".");
								counter++;
								if (counter > 80) {
									counter = 0;
								}
							}
							TimeUnit.MILLISECONDS.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (!simpleResults)
						System.out.println(" DONE ***");

					summarizeTest();

					if (allResults || (procSucc > (bestSuccess - bestRange))) {

						if (procSucc > bestSuccess) {
							bestSuccess = procSucc;
							bestParams = "A: " + df.format(a) + ", B: " + df.format(b) + ", Size: " + size;
						}

						if (!simpleResults) {
							System.out.println("A: " + df.format(a) + ", B: " + df.format(b) + ", Size: " + size
									+ ", Reported threads: " + reported);
							System.out.print("Success: " + df.format(procSucc) + "%, (" + success + "), ");
							System.out.println("Failed: " + df.format(procFail) + "%, (" + fail + ")");
							System.out.print("Average Success: " + df.format(avgSuccess) + ", ");
							System.out.println("Average Failed: " + df.format(avgFail));
							System.out.println("Average time (sec.): " + avgTime / 1000);
							System.out.println("*******************************************************");
						} else {
							System.out.println(df.format(a) + "\t" + df.format(b) + "\t" + size + "\t" + success + "\t"
									+ fail + "\t" + df.format(procSucc) + "\t" + df.format(procFail) + "\t"
									+ df.format(avgSuccess) + "\t" + df.format(avgFail) + "\t" + df.format(avgTime));
						}
					}
				}
			}
		}

		System.out.println("The best: " + df.format(bestSuccess) + "%, " + bestParams);

	}

	public void doTestLeaveOneOut(double a, double b, int set_size, int segments) {

		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("#.###", otherSymbols);

		//System.out.println("Starting ...");

		clearStats();

		if (!simpleResults) {
			System.out.println("*** Processing ");
		}
		int counter = 0;

		// for all positions
		// run defined threads (tests)
		for (int i = 0; i < set_size; i++) {
			FuzzyTestWorker test = new FuzzyTestWorker(a, b, i, segments, false, this);
//			FuzzyTestWorker2 test = new FuzzyTestWorker2(a, b, i, false, this);
//			FuzzyTestClusteredWorker test = new FuzzyTestClusteredWorker(a, b, i, false, this);
			test.start();
			
			if ((i+1) % threads == 0) { //wait every defined number of threads until they are done to continue creating new ones
				while (reported < i + 1) {
					try {
						if (!simpleResults) {
							System.out.print(".");
							counter++;
							if (counter > 80) {
								System.out.println();
								counter = 0;
							}
						}
						TimeUnit.MILLISECONDS.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		}

		// wait for the rest to finish
		while (reported < set_size) {
			try {
				if (!simpleResults) {
					System.out.print(".");
					counter++;
					if (counter > 80) {
						System.out.println();
						counter = 0;
					}
				}

				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (!simpleResults)
			System.out.println(" DONE ***");

		summarizeTest();

		if (allResults || (procSucc > (bestSuccess - bestRange))) {

			if (procSucc > bestSuccess) {
				bestSuccess = procSucc;
				bestParams = "A: " + df.format(a) + ", B: " + df.format(b);
			}

			if (!simpleResults) {
				System.out.println("A: " + df.format(a) + ", B: " + df.format(b) + ", segments: " + df.format(segments) + ", Reported threads: " + reported);
				System.out.print("Success: " + df.format(procSucc) + "%, (" + success + "), ");
				System.out.println("Failed: " + df.format(procFail) + "%, (" + fail + ")");
				System.out.print("Average Success: " + df.format(avgSuccess) + ", ");
				System.out.println("Average Failed: " + df.format(avgFail));
				System.out.println("Average time (sec.): " + avgTime / 1000);
				System.out.println("*******************************************************");
			} else {
				System.out.println(df.format(segments) + "\t" + df.format(a) + "\t" + df.format(b) + "\t" + success + "\t" + fail + "\t"
						+ df.format(procSucc) + "\t" + df.format(procFail) + "\t" + df.format(avgSuccess) + "\t"
						+ df.format(avgFail) + "\t" + df.format(avgTime));
			}
		}

		//System.out.println("The best: " + df.format(bestSuccess) + "%, " + bestParams);

	}

	public void doTestLeaveOneOutDegraded(double a, double b, int set_size, int degradation_level) {
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("#.###", otherSymbols);

		//System.out.println("Starting ...");

		clearStats();

		if (!simpleResults) {
			System.out.println("*** Processing ");
		}
		int counter = 0;

		// for all positions
		// run defined threads (tests)
		for (int i = 0; i < set_size; i++) {
			FuzzyTestWorkerDegraded test = new FuzzyTestWorkerDegraded(a, b, i, 18, degradation_level, false, this);
			test.start();
			
			if ((i+1) % threads == 0) { //wait every defined number of threads until they are done to continue creating new ones
				while (reported < i + 1) {
					try {
						if (!simpleResults) {
							System.out.print(".");
							counter++;
							if (counter > 80) {
								System.out.println();
								counter = 0;
							}
						}
						TimeUnit.MILLISECONDS.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		}

		// wait for the rest to finish
		while (reported < set_size) {
			try {
				if (!simpleResults) {
					System.out.print(".");
					counter++;
					if (counter > 80) {
						System.out.println();
						counter = 0;
					}
				}

				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (!simpleResults)
			System.out.println(" DONE ***");

		summarizeTest();

		if (allResults || (procSucc > (bestSuccess - bestRange))) {

			if (procSucc > bestSuccess) {
				bestSuccess = procSucc;
				bestParams = "A: " + df.format(a) + ", B: " + df.format(b);
			}

			if (!simpleResults) {
				System.out.println("A: " + df.format(a) + ", B: " + df.format(b) + ", segments: " + df.format(degradation_level) + ", Reported threads: " + reported);
				System.out.print("Success: " + df.format(procSucc) + "%, (" + success + "), ");
				System.out.println("Failed: " + df.format(procFail) + "%, (" + fail + ")");
				System.out.print("Average Success: " + df.format(avgSuccess) + ", ");
				System.out.println("Average Failed: " + df.format(avgFail));
				System.out.println("Average time (sec.): " + avgTime / 1000);
				System.out.println("*******************************************************");
			} else {
				System.out.println(df.format(degradation_level) + "\t" + df.format(a) + "\t" + df.format(b) + "\t" + success + "\t" + fail + "\t"
						+ df.format(procSucc) + "\t" + df.format(procFail) + "\t" + df.format(avgSuccess) + "\t"
						+ df.format(avgFail) + "\t" + df.format(avgTime));
			}
		}

		//System.out.println("The best: " + df.format(bestSuccess) + "%, " + bestParams);
	}
}
