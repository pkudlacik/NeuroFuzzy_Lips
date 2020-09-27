package badania.main;

import badania.neuro.NeuroTest;

public class MainNeuroTest {

	public static void main(String[] args) {
		
		NeuroTest test = new NeuroTest();

		test.setSimpleResults(false);
		test.setShowOnlyBestResults(false);
		//test.setBestLevel(80.0);
		test.setBestRange(5.0);
		test.setThreads(8);
		
//		test.doTest(10000, 10000, 1000, // iterations - start, stop, step
//					15.0, 15.0, 0.8, // error - start, stop, step
//					15, 15, // size - start, stop
//					100 ); // runs in one set
		
		//for (int i=0; i<5; i++)
			test.doTestLeaveOneOut(2000, 0.9, 12);
		
	}

}
