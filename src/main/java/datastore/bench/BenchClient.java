package datastore.bench;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import bftsmart.tom.ServiceProxy;
import datastore.bench.flowsimulations.FlowSimulation;
import datastore.bench.flowsimulations.deviceManager.DeviceManager;

public abstract class BenchClient implements Runnable{
	
	public static void main(String[] args) throws InterruptedException{
	    if (args.length < 3) {
            System.out.println("Usage: ... ThroughputLatencyClient <num. threads> <number of operations per thread> <verbose>"); 
            System.exit(-1);
        }

	    int numThreads = Integer.parseInt(args[0]);
        int numberOfFlows = Integer.parseInt(args[1]); 
        boolean verbose =Boolean.parseBoolean(args[3]);
        long interval = Integer.parseInt(args[2]); 
        Thread[] c = new Thread[numThreads];
        
        if (verbose){
        	System.out.println("Creating threads"); 
        }
        
        for(int i=0; i<numThreads; i++) {
            c[i] = new Thread(new MultiFlowTypes(i, numberOfFlows, verbose, interval)); 
            //c[i].start();
        }

        
        for(int i=0; i<numThreads; i++) {
            c[i].start();
        }
        if (verbose){
        	System.out.println("Threads created... "); 
        }
        
        for(int i=0; i<numThreads; i++) {

            try {
                c[i].join();
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
        }
        System.exit(0);
	}

	protected ServiceProxy proxy ; 
	protected  final int id;
	DescriptiveStatistics latency = new DescriptiveStatistics();
	private final int numFlows;
	private final boolean verbose;  
	private final long interval; 
	
	public BenchClient(int id, int numFlows, boolean verbose, long interval){
		this.id = id; 
		proxy = new ServiceProxy(id); 
		this.numFlows = numFlows; 
		this.verbose = verbose; 
		this.interval = interval; 
	}
	
	@Override
	public  void run(){
		byte[] start= null; 
		byte[] end = null; 
		
		try{
			ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			DataOutputStream dos = new DataOutputStream(out);
			dos.writeInt(-1); 
			dos.flush(); dos.close();
			start = out.toByteArray(); 
			 out = new ByteArrayOutputStream(); 
			dos = new DataOutputStream(out);
			dos.writeInt(-2); 
			dos.flush(); dos.close();
			end = out.toByteArray(); 
			
		}catch(Exception e){
			e.printStackTrace(); 
		}
		
		if (verbose ){
			System.out.println("Starting thread " + this.id); 
		}
		for (long i = 0; i < numFlows ; i++){
			if (verbose && (i % interval) == 0 ){
				System.out.println("Thread " + id + " on request: " + i); 
			}
			final long tflow_started = System.nanoTime();
			FlowSimulation chooseFlow = chooseNextFlow();
			chooseFlow.run(proxy);	 
			proxy.invokeUnordered(end);
			latency.addValue(System.nanoTime()- tflow_started);
		}
		
		System.out.println("Thread " + id + ":\n"  + latency.toString()); 
	}
	
	protected  abstract FlowSimulation chooseNextFlow();
}
