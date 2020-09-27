package badania.neuro;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import badania.TestResult;
import neuralnetwork.actfun.AFSigm;
import neuralnetwork.data.DataPackage;
import neuralnetwork.data.DataVector;
import neuralnetwork.exceptions.NeuroException;
import neuralnetwork.learningalg.BackPAlgM;
import neuralnetwork.network.NNetFF;

public class NeuroTestWorker extends Thread {

	private int max_iter = 1000;
	private double max_error = 0.2;

	private long iter = 0; // performed learning iterations
	private double error = 1.0; // obtained error

	private int sizePackage = 5;
	private boolean verbose = false;

	private int success = 0;
	private int failed = 0;

	private long start;
	private long stop;

	private NeuroTest test;

	DataPackage learnInput;
	DataPackage learnOutput;
	DataPackage testInput;
	DataPackage testOutput;

	private NNetFF net;
	private AFSigm act_fun;
	BackPAlgM alg_m;

	/**
	 * Configure test
	 * 
	 * @param a           range divider (the higher a - the narrower the set
	 *                    description.)
	 * @param sizePackage number of learning samples taken from each class
	 * @param verbose     if true - print results on console
	 */
	public NeuroTestWorker(int max_iterations, double max_error, int sizePackage, boolean verbose, NeuroTest test) {
		super();
		this.max_iter = max_iterations;
		this.max_error = max_error;

		this.sizePackage = sizePackage;
		this.verbose = verbose;
		this.test = test;
	}

	public int getSuccess() {
		return success;
	}

	public int getFailed() {
		return failed;
	}

	public long getStart() {
		return start;
	}

	public long getStop() {
		return stop;
	}

	/**
	 * Run test
	 */
	public void run() {
		start = System.currentTimeMillis();

		//loadData();
		loadData_leaveOneOut();
		teachNeuralNetwork();
		testNeuralNetwork();

		stop = System.currentTimeMillis();

		test.addResult(new TestResult(success, failed, iter, error, stop - start));
	}

	private void loadData_leaveOneOut() {

		DataPackage learn = new DataPackage();
		DataPackage bottom = new DataPackage();
		
		bottom.loadTextFile("data/SEG10_BOTTOM.txt");
		learn.loadTextFile("data/SEG10_UP.txt");
		DataPackage rest = learn.splitByColumn(learn.getMaxRowSize()-2);
		learn.merge(bottom);

		DataPackage test = learn.removeVector(sizePackage);
		
		DataPackage learnOut = learn.splitByColumn(learn.getMaxRowSize()-2);
		DataPackage testOut = test.splitByColumn(test.getMaxRowSize()-2);

		// make packages for NN
		learnInput = learn;
		testInput = test;

		// normalize
		int learn_size = learnInput.size();
		learnInput.add(testInput);

		// normalize all sets together
		learnInput.normalize();
		testInput = learnInput.splitByRow(learn_size - 1); // split sets again

		testOutput = new DataPackage();
		// replace class number with a vector
		for (DataVector v : testOut.getList()) {
			double value = v.get(0)-1; //make values from 0-99
			double [] tab = new double[100];
			tab[(int)value] = 1.0;
			testOutput.add(new DataVector(tab));
		}
		learnOutput = new DataPackage();
		for (DataVector v : learnOut.getList()) {
			double value = v.get(0)-1; //make values from 0-99
			double [] tab = new double[100];
			tab[(int)value] = 1.0;
			learnOutput.add(new DataVector(tab));
		}

		if (verbose) {
			System.out.println(learnInput);
			System.out.println(learnOutput);
			System.out.println(testInput);
			System.out.println(testOutput);
		}
	}

	private void teachNeuralNetwork() {

		// 0. create neural network
		net = new NNetFF();
		act_fun = new AFSigm(1.0);

		alg_m = new BackPAlgM();

		net.setInputSize(140);
		net.setNetworkSize(3);

		net.setLayerSize(1, 140);
		net.setLayerSize(2, 160);
		net.setLayerSize(3, 100);
		net.setOutActFun(act_fun);

		net.initializeWeights();

		alg_m.setNeuralNetwork(net);
		alg_m.setMaxIter(max_iter);
		alg_m.setMaxError(max_error);
		alg_m.setMomentum(0.0);

		try {
			alg_m.learnEx(learnInput, learnOutput);
		} catch (NeuroException e) {
			e.printStackTrace();
		}
		
		error = alg_m.getLastError();
		iter = alg_m.getLastIter();

		// System.out.println("Lastiter :" + iter);
		// System.out.println("Lasterror: " + error);

		// testowanie nauczonej sieci neuronowej
		// alg_m.testNetwork(learnInput, learnOutput);

		/*
		 * alg_m.getListVector().forEach(item -> {
		 * System.out.println("testowy outNetwork: " + item); });
		 */
	}

	private void testNeuralNetwork() {

		success = 0;
		failed = 0;

		DataVector output = new DataVector();

		int outIdx = 0;

		try {
			for (DataVector input : testInput.getList()) {

				int class_real = 0;
				int class_obtained = 0;

				// get output row
				DataVector res = testOutput.get(outIdx);
				outIdx++;

				// process data by network
				net.process(input, output);

				// analyze results
				double max_result = 0.0;
				for (int i = 0; i < 100; i++) {

					// get real class
					if (res.get(i) > 0.5) {
						class_real = i + 1;
					}

					// get result :
					double temp_result = output.get(i);

					if (temp_result > max_result) {
						max_result = temp_result;
						class_obtained = i + 1;
					}
				}

				// sprawdzanie:

				if (class_real == class_obtained) {
					success++;
				} else {
					failed++;
				}

//				DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
//				otherSymbols.setDecimalSeparator('.');
//				DecimalFormat df = new DecimalFormat("0.000", otherSymbols);
//
//				System.out.println("object [c" + class_real + "]: " + input);
//				System.out.print("[c1]: " + df.format(output.get(0)));
//				System.out.print("\t[c2]: " + df.format(output.get(1)));
//				System.out.println("\t[c3]: " + df.format(output.get(2)));
//				System.out.println("----------------------------------------------------");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (verbose) {
			DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
			otherSymbols.setDecimalSeparator('.');
			DecimalFormat df = new DecimalFormat("#.#", otherSymbols);

			double procSucc = (success * 1.0 / (success + failed)) * 100;
			double procFail = (failed * 1.0 / (success + failed)) * 100;
			System.out.println("***WORKER**********************************************");
			System.out.println("Iter: " + iter + ", Error: " + error + ", Package: " + sizePackage);
			System.out.println("Success: " + df.format(procSucc) + "%, (" + success + ")");
			System.out.println("Failed: " + df.format(procFail) + "%, (" + failed + ")");
			long stop = System.currentTimeMillis();
			System.out.println("Czas wykonania (sek.): " + (stop - start) / 1000);
			System.out.println("*******************************************************");
		}

	}

}
