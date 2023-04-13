package examples.hello;

import com.ibm.cics.server.CommAreaHolder;
import com.ibm.cics.server.Task;

public class HelloCICSWorld {
    public static void main(CommAreaHolder CAH) {
        Task t = Task.getTask();
        if (t == null) {
            System.err.println("examples.hello.HelloCICSWorld example: Can't get Task");
        }
        else {
            t.out.println("Hello from a Java CICS application");
        }
    }
}