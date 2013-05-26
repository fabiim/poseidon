package datastore.workloads;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.floodlightcontroller.core.module.FloodlightModuleException;

import org.python.google.common.collect.Lists;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import datastore.workloads.logger.RequestLogger;

class Ch{
	Session session;

	public void start(String input) {
		  Channel channel;
		try {
			channel = session.openChannel("exec");
		
	      ((ChannelExec)channel).setCommand(input);
	 
	      //channel.setInputStream(System.in);
	      channel.setInputStream(null);
	 
	      channel.setOutputStream(System.out);
	      
	      //FileOutputStream fos=new FileOutputStream("/tmp/stderr");
	      //((ChannelExec)channel).setErrStream(fos);
	      ((ChannelExec)channel).setErrStream(System.err);
	 
	      InputStream in=channel.getInputStream();
	 
	      channel.connect();
	 
	      byte[] tmp=new byte[1024];
	      while(true){
	        while(in.available()>0){
	          int i=in.read(tmp, 0, 1024);
	          if(i<0)break;
	          System.out.print(new String(tmp, 0, i));
	        }
	        if(channel.isClosed()){
	          System.out.println("exit-status: "+channel.getExitStatus());
	          break;
	        }
	        try{Thread.sleep(1000);}catch(Exception ee){}
	      }
	      channel.disconnect(); 
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			AutomaticTests.abortDueToException(e, "Problems with execution of mininet"); 
		}
	} 
	
}

class Runs implements Serializable{
	public Runs(List<RequestLogEntry> operations2,
			List<ActivityEvent> events2, String input) {
		this.operations = operations2; 
		this.events = events2; 
		this.input  = input; 
	}
	String input; 
	List<RequestLogEntry> operations;
	List<ActivityEvent> events;
}

public class AutomaticTests implements Serializable{
	private static final String LOCAL_MN_SCRIPT = "scriptXptoByssh.py";
	private static final String OUTPUT_LOG =   "log";
	String description;
	String codeName; 
	List<String> mininetScriptInput; 
	String mininetScriptPath;

	
	List<Runs>  runs= Lists.newArrayList();
	
	public static void  main(String[] args){
		AutomaticTests test = new AutomaticTests(); 
		test.description = "Running a increasingly bigger TreeTopology to check the kind of stress it sets on smart by Link updates code originated in the Topology Manager"; 
		test.mininetScriptInput = Lists.newArrayList(
				"--depth  2 --fanout 2",
				"--depth  4 --fanout 2",
				"--depth  6 --fanout 2",
				"--depth  8 --fanout 2",
				"--depth  2 --fanout 4",
				"--depth  2 --fanout 6",
				"--depth  2 --fanout 8" //64 links  
				); 
		test.mininetScriptPath = "./treeNode.py";
		test.codeName = "treeTopo.topologyManager.workload." + System.currentTimeMillis();
		
		Ch mininetChannel = setMininetChannel();
		
		for (String input : test.mininetScriptInput){
			//Thread t = startFloodlight();
			RequestLogger logger =datastore.workloads.logger.RequestLogger.getRequestLogger(); 
			wwaitForFloodlight();			
			mininetChannel.start( " sudo python "  + test.mininetScriptPath + " " +  input);
			//System.out.println("WAITING FOR USER INPUT TO COLLECT DATA (also gives ti"); 
			//System.in.read();
			test.runs.add(new Runs(logger.getOperations(), logger.getEvents(), input)); 
			
		//	stopFloodlight(t);
		}
		
		pullScriptFromMininet(test);
		doBackupInfo(test);
	}
		
	
	
	private static void wwaitForFloodlight() {
		String ip = "127.0.0.1";
		int port = 6633; 
		int maxtries = 5; 
		waitForConnection(ip, port, maxtries);
		
	}

	private static void waitForConnection(String ip, int port, int maxtries) {
		int tries = 0;
		boolean connect = false; 
		while (!connect && tries < maxtries){
			tries++; 
			try{
			Socket s = new Socket(ip, port);
			if (s.isConnected()){
				connect = true;
				s.close(); 
			}
			else{
				s.close(); 
				try{Thread.sleep(2000);} catch(Exception e){}
			}
			}catch(IOException e){
				try{Thread.sleep(2000);} catch(Exception e2){}
				continue; 
			}
		}
		if (tries == maxtries){
			abortDueToException(new Exception(), "Can not connect to server " + ip  + "@" + port); 
		}
		System.out.println("Connected to " + ip + "@" + port); 
	}

	private static void stopFloodlight(Thread t2) {
		t2.stop();
		Thread[] allOthers = getGroupThreads(t2.getThreadGroup());
		for (Thread t : allOthers){
			 if (t != Thread.currentThread() ){
				 t.stop();
				 //TODO - inherently unsafe... lol 
			 }
		}
	}
	
	private static Thread[] getGroupThreads( final ThreadGroup group ) {
	    if ( group == null )
	        throw new NullPointerException( "Null thread group" );
	    int nAlloc = group.activeCount( );
	    int n = 0;
	    Thread[] threads;
	    do {
	        nAlloc *= 2;
	        threads = new Thread[ nAlloc ];
	        n = group.enumerate( threads );
	    } while ( n == nAlloc );
	    return java.util.Arrays.copyOf( threads, n );

	}
	private static Ch setMininetChannel() {
		 JSch jsch=new JSch();  
         try {
        	String knownHostsFilename = "/Users/fabiim/.ssh/known_hosts";
     		jsch.setKnownHosts( knownHostsFilename );
     		
			Session session=jsch.getSession("mininet", "192.168.38.128", 22);
			session.setUserInfo(ui); 
			session.setPassword("mininet"); 
			session.connect(); 
			Ch c = new Ch(); 
			c.session = session ; 
			return c; 
			  
		} catch (JSchException e) {
			abortDueToException(e, "Can not connect by ssh to mininet ");
			e.printStackTrace();
		}
		return null;
	}
	
	private static void doBackupInfo(AutomaticTests test) {
		try {
			AutomaticTests.zip(Lists.newArrayList(
					//FOLDERS OF CODE TO BACKUP AS CODE
					new File("./src/main/java"), 
					new File("/Users/fabiim/dev/eclipse-workspaces/tese/bft-smart/src/mapserver"),
					new File ("./workloads/" + AutomaticTests.LOCAL_MN_SCRIPT), 
					new File ("./workloads/" + OUTPUT_LOG)
					//Ouput File : 
					), new File("./workloads/" + test.codeName + ".zip") 
					
					);
					
		} catch (IOException e) {
			abortDueToException(e, "Can not zip files"); 
		}
		
	}

	private static UserInfo ui = new UserInfo(){
		  public String getPassword(){
			  return "mininet"; 
		  }

		@Override
		public String getPassphrase() {
			return "mininet";
		}

		@Override
		public boolean promptPassphrase(String arg0) {
			return false;
		}

		@Override
		public boolean promptPassword(String arg0) {
			return false;
		}

		@Override
		public boolean promptYesNo(String arg0) {
			return false;
		}

		@Override
		public void showMessage(String arg0) {
			
		}
	  };

	private static void pullScriptFromMininet(AutomaticTests test) {
		try{
		JSch jsch = new JSch();

		String knownHostsFilename = "/Users/fabiim/.ssh/known_hosts";
		jsch.setKnownHosts( knownHostsFilename );

		Session session = jsch.getSession( "mininet", "192.168.38.128" );    
		{
		  // "interactive" version
		  // can selectively update specified known_hosts file 
		  // need to implement UserInfo interface
		  // MyUserInfo is a swing implementation provided in 
		  //  examples/Sftp.java in the JSch dist
		  session.setUserInfo(ui); 

		  // OR non-interactive version. Relies in host key being in known-hosts file
		//  session.setPassword( "mininet" );
		}

		session.connect();

		Channel channel = session.openChannel( "sftp" );
		channel.connect();
		
		ChannelSftp sftpChannel = (ChannelSftp) channel;
		try{
			sftpChannel.rm(LOCAL_MN_SCRIPT);
		}catch(Exception e){
			;
		}
		File f = new File(LOCAL_MN_SCRIPT);
		if (f.exists()){
			f.delete(); 
		}
		
		sftpChannel.get(test.mininetScriptPath, "./workloads/" + LOCAL_MN_SCRIPT);
		sftpChannel.exit();
		session.disconnect();
		}catch(Exception e){
			AutomaticTests.abortDueToException(e, "failed@pushing file to mininet"); 
		}
	}
	
	private static boolean connected2smart = false;
	
	/*private static  Thread startFloodlight(){
			String [] emptyArrayCauseIAmNoob = new String[0]; 

			if (!connected2smart){
				connect2Smart(emptyArrayCauseIAmNoob);
			}
			
			//Safely start floodlight
			try {
				Process p = Runtime.getRuntime().exec("/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/bin/java" + " -Dfile.encoding=MacRoman " + 
						" -classpath /Users/fabiim/dev/open/floodlight-v0.9/target/bin:/Users/fabiim/dev/open/floodlight-v0.9/target/bin-test:/Users/fabiim/dev/open/floodlight-v0.9/lib/args4j-2.0.16.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/cglib-nodep-2.2.2.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/concurrentlinkedhashmap-lru-1.2.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/jackson-core-asl-1.8.6.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/jackson-mapper-asl-1.8.6.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/junit-4.8.2.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/jython-2.5.2.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/libthrift-0.7.0.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/logback-classic-1.0.0.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/logback-core-1.0.0.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/objenesis-1.2.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/org.easymock-3.1.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/org.restlet-2.1-RC1.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/org.restlet.ext.jackson-2.1-RC1.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/org.restlet.ext.simple-2.1-RC1.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/org.restlet.ext.slf4j-2.1-RC1.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/simple-4.1.21.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/slf4j-api-1.6.4.jar:/Users/fabiim/dev/eclipse-workspaces/tese/bft-smart/bin:/Users/fabiim/dev/eclipse-workspaces/tese/bft-smart/lib/commons-codec-1.5.jar:/Users/fabiim/dev/eclipse-workspaces/tese/bft-smart/lib/slf4j-api-1.5.8.jar:/Users/fabiim/dev/eclipse-workspaces/tese/bft-smart/lib/slf4j-jdk14-1.5.8.jar:/Users/fabiim/dev/eclipse-workspaces/tese/bft-smart/lib/guava-14.0-rc1.jar:/Users/fabiim/dev/eclipse-workspaces/tese/bft-smart/lib/netty-3.2.6.Final.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/netty-3.2.6.Final.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/commons-math3-3.1.1.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/commons-configuration-1.9.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/commons-lang-2.6.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/commons-logging-1.1.1.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/commons-logging-adapters-1.1.1.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/commons-logging-api-1.1.1.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/commons-net-3.2.jar:/Users/fabiim/dev/open/floodlight-v0.9/lib/jsch-0.1.49.jar" 
						+ " net.floodlightcontroller.core.Main");
				System.out.println("to string: " + p.toString()); 
				System.out.println("class: " + p.getClass().getCanonicalName());
			} catch (IOException e1) {
				abortDueToException(e1, "Failed to start floodlight"); 
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
	}
*/
	private static void connect2Smart(String[] emptyArrayCauseIAmNoob) {
		mapserver.MapSmart.main(emptyArrayCauseIAmNoob);
		waitForConnection("127.0.0.1", 11000, 5); 
		
	}

	static void abortDueToException(Exception e, String cause) {
		System.err.println("Test aborted due"  + cause ) ; 
		e.printStackTrace();
		
		//Clean up 
		
		System.exit(-1); 
	}

	
	public static void zip(List<File> directories, File zipfile) throws IOException {
		Closeable res = null; 
		try {
			OutputStream out = new FileOutputStream(zipfile);
			ZipOutputStream zout = new ZipOutputStream(out);
			res = out;
			for (File directory : directories){
				URI base = directory.toURI();
				Deque<File> queue = new LinkedList<File>();
				queue.push(directory);
				

				res = zout;
				while (!queue.isEmpty()) {
					directory = queue.pop();
					for (File kid : directory.listFiles()) {
						String name = base.relativize(kid.toURI()).getPath();
						if (kid.isDirectory()) {
							queue.push(kid);
							name = name.endsWith("/") ? name : name + "/";
							zout.putNextEntry(new ZipEntry(name));
						} else {
							zout.putNextEntry(new ZipEntry(name));
							copy(kid, zout);
							zout.closeEntry();
						}
					}
				}
			}
		}catch(Exception e){
			System.err.println("Could not perform backup of code");
		}finally{
			res.close(); 
		}
	}

	 private static void copy(InputStream in, OutputStream out) throws IOException {
		    byte[] buffer = new byte[1024];
		    while (true) {
		      int readCount = in.read(buffer);
		      if (readCount < 0) {
		        break;
		      }
		      out.write(buffer, 0, readCount);
		    }
		  }

		  private static void copy(File file, OutputStream out) throws IOException {
		    InputStream in = new FileInputStream(file);
		    try {
		      copy(in, out);
		    } finally {
		      in.close();
		    }
		  }

		  private static void copy(InputStream in, File file) throws IOException {
		    OutputStream out = new FileOutputStream(file);
		    try {
		      copy(in, out);
		    } finally {
		      out.close();
		    }
		  }
}