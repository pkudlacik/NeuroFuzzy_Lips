package badania.neuro;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import badania.TestResult;

public class NeuroTest {

	private LinkedList<TestResult> results;

	private long iter;
	private double error;

	private int success = 0;
	private int fail = 0;
	private double avgSuccess = 0.0;
	private double stdDev = 0.0;
	private double procStdDev = 0.0;
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

	public NeuroTest() {
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
		procSucc = 0.0;
		procFail = 0.0;
	}

	synchronized public void addResult(TestResult result) {
		results.add(result);
		reported++;
	}

	private void summarizeTest() {
		if (reported > 5000000) { // delete outliers if there is enough runs
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
			this.iter += result.getIter();
			this.error += result.getError();
		}

		iter /= reported;
		error /= reported;

		avgTime = time * 1.0 / reported;
		avgFail = fail / reported;
		avgSuccess = success / reported;

		procFail = fail * 100.0 / (fail + success);
		procSucc = success * 100.0 / (fail + success);
	}

	public void doTest(int iter_start, int iter_stop, int iter_step, double error_start, double error_stop,
			double error_step, int size_start, int size_stop, int tests) {

		int i = 1000;
		double e = 0.2;
		int size = 5;

		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("#.##", otherSymbols);

		System.out.println("Starting ...");

		int counter = 0;

		// for all specified sizes
		for (size = size_start; size <= size_stop; size++) {
			// for all specified iterations
			for (i = iter_start; i <= iter_stop; i += iter_step) {
				// for all specified iterations
				for (e = error_start; e <= error_stop; e += error_step) {

					clearStats();

					int runs = tests;

					if (!simpleResults) {
						System.out.println("*** Processing ");
					}
					// run defined threads (tests)
					for (int k = 0; k < runs; k++) {
						NeuroTestWorker test = new NeuroTestWorker(i, e, size, false, this);
						test.start();
						if ((k+1) % threads == 0) { // wait every defined number of threads until they are done to continue
												// creating new ones
							while (reported < k + 1) {
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
								} catch (InterruptedException ex) {
									ex.printStackTrace();
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
									System.out.println();
									counter = 0;
								}
							}
							TimeUnit.MILLISECONDS.sleep(1000);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
					}
					if (!simpleResults)
						System.out.println(" DONE ***");

					summarizeTest();

					if (allResults || (procSucc > (bestSuccess - bestRange))) {

						if (procSucc > bestSuccess) {
							bestSuccess = procSucc;
							bestParams = "Iter: " + i + ", Error: " + df.format(e) + ", Size: " + size;
						}

						if (!simpleResults) {
							System.out.println("Iter: " + i + ", Error: " + df.format(e) + ", Size: " + size
									+ ", Reported threads: " + reported);
							System.out.print("Success: " + df.format(procSucc) + "%, (" + success + "), ");
							System.out.println("Failed: " + df.format(procFail) + "%, (" + fail + ")");
							System.out.print("Average Success: " + df.format(avgSuccess) + ", ");
							System.out.println("Average Failed: " + df.format(avgFail));
							System.out.print("Average iter: " + df.format(iter) + ", ");
							System.out.println("Average error: " + df.format(error));
							System.out.println("Average time (sec.): " + avgTime / 1000);
							System.out.println("*******************************************************");
						} else {
							System.out.println(i + "\t" + df.format(e) + "\t" + size + "\t" + df.format(iter) + "\t"
									+ df.format(error) + "\t" + success + "\t" + fail + "\t" + df.format(procSucc)
									+ "\t" + df.format(procFail) + "\t" + df.format(avgSuccess) + "\t"
									+ df.format(avgFail) + "\t" + df.format(avgTime));
						}
					}
				}
			}
		}

		System.out.println("The best: " + df.format(bestSuccess) + "%, " + bestParams);

	}

	public void doTestLeaveOneOut(int max_iter, double max_error, int set_size) {

		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("#.###", otherSymbols);

		System.out.println("Starting ...");

		clearStats();

		int counter = 0;
		if (!simpleResults) {
			System.out.println("*** Processing ");
		}

		// for all positions
		// run defined threads (tests)
		for (int i = 0; i < set_size; i++) {
			NeuroTestWorker test = new NeuroTestWorker(max_iter, max_error, i, false, this);
			test.start();
			if ((i+1) % threads == 0) { // wait every defined number of threads until they are done to continue
								// creating new ones
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
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		if (!simpleResults)
			System.out.println(" DONE ***");

		summarizeTest();

		if (allResults || (procSucc > (bestSuccess - bestRange))) {

			if (procSucc > bestSuccess) {
				bestSuccess = procSucc;
				bestParams = "Iter: " + max_iter + ", Error: " + df.format(max_error) + ", Size: " + set_size;
			}

			if (!simpleResults) {
				System.out.println("Iter: " + max_iter + ", Error: " + df.format(max_error) + ", Size: " + set_size
						+ ", Reported threads: " + reported);
				System.out.print("Success: " + df.format(procSucc) + "%, (" + success + "), ");
				System.out.println("Failed: " + df.format(procFail) + "%, (" + fail + ")");
				System.out.print("Average Success: " + df.format(avgSuccess) + ", ");
				System.out.println("Average Failed: " + df.format(avgFail));
				System.out.print("Average iter: " + df.format(iter) + ", ");
				System.out.println("Average error: " + df.format(error));
				System.out.println("Average time (sec.): " + avgTime / 1000);
				System.out.println("*******************************************************");
			} else {
				System.out.println(max_iter + "\t" + df.format(max_error) + "\t" + set_size + "\t" + df.format(iter)
						+ "\t" + df.format(error) + "\t" + success + "\t" + fail + "\t" + df.format(procSucc) + "\t"
						+ df.format(procFail) + "\t" + df.format(avgSuccess) + "\t" + df.format(avgFail) + "\t"
						+ df.format(avgTime));
			}
		}

		System.out.println("The best: " + df.format(bestSuccess) + "%, " + bestParams);

	}

}
