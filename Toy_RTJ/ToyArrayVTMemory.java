import javax.realtime.VTMemory;
import javax.realtime.RealtimeThread;

class ToyArrayVTMemory extends RealtimeThread {
    public static void main(String[] arg) {
	int asize=0;
	int repeats=0;
	try {
	    asize=Integer.parseInt(arg[0]);
	    repeats=Integer.parseInt(arg[1]);
	} catch (Exception e) {
	    System.out.println("Toy Array <array size> <repeats>");
	    System.exit(-1);
	}
	ToyArrayVTMemory ta = new ToyArrayVTMemory(asize,repeats);
	ToyArrayVTMemory tb = new ToyArrayVTMemory(asize,repeats);
	long start=System.currentTimeMillis();
	ta.start();
	tb.start();
	try {
	    ta.join();
	    tb.join();
	} catch (Exception e) {System.out.println(e);}

	long end=System.currentTimeMillis();
	System.out.println("Elapsed time(mS): "+(end-start));
    }

    public ToyArrayVTMemory(int size, int repeat) {
	super(new VTMemory(1000000, 1000000));
	this.size=size;
	this.repeat=repeat;
    }

    int size;
    int repeat;

    public void run() {
	Object[] a=new Object[size];
	Object[] b=new Object[size];
	Object ao=new Object();
	Object bo=new Object();
	for (int i=0;i<size;i++) {
	    a[i]=ao;
	    b[i]=bo;
	}
	for (int j=0;j<repeat;j++)
	    for (int i=0;i<size;i++) {
		Object t=a[i];
		a[i]=b[i];
		b[i]=t;
	    }
    }

}
